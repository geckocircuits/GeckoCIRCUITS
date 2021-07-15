/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.datacontainer;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 *
 * @author andreas
 */
public class CompressorIntMatrix {    

    private List<byte[]> byteContainer = new ArrayList<byte[]>();
    private static final int BYTE_BLOCK_SIZE = 1024;
    public final float compressionRatio;
    public final float compressionTime;
    private int bytesCompressed = 0;
    private final int _m;
    private final int _n;

    public CompressorIntMatrix(int[][] dataToCompress) {
        _m = dataToCompress.length;
        if(_m > 0) {
            _n = dataToCompress[0].length;
        } else { // this could happen if we don't have a scope in the model!
            _n = 0;
        }
        
        long tick = System.currentTimeMillis();
        doCompression(dataToCompress);
        long tock = System.currentTimeMillis();
        compressionRatio = (float) 4.0 * _m * _n / (byteContainer.size() * BYTE_BLOCK_SIZE);
        compressionTime = (tock - tick) / 1000.0f;
    }
    static long compressedByteSize = 0;

    public void doCompression(int[][] origData) {
        byte[] input = convertIntToByteArray(origData);
        // Compress the bytes            
        Deflater compressor = new Deflater(5);
        compressor.setInput(input);
        compressor.finish();

        compress(compressor);
        //System.out.println("data bytes size: " + compressedByteSize / 1024 / 1024 + " MB");

        recycleByteArray(input);
    }
    
    private void compress(Deflater compressor) {
        while (!compressor.finished()) {
            byte[] compressed = new byte[BYTE_BLOCK_SIZE];
            compressedByteSize += BYTE_BLOCK_SIZE;
            bytesCompressed += compressor.deflate(compressed);
            byteContainer.add(compressed);
        }
    }

    public int[][] deCompress() {
        Inflater decompresser = new Inflater();

        int bytesToDecompress = 4 * _m * _n;
        int bytesDecompressed = 0;

        byte[] result = new byte[bytesToDecompress];
        for (int i = 0; i < byteContainer.size(); i++) {
            try {
                int length = Math.min(BYTE_BLOCK_SIZE, bytesToDecompress);
                decompresser.setInput(byteContainer.get(i), 0, length);
                int localDecompressed = decompresser.inflate(result, bytesDecompressed, bytesToDecompress);
                bytesToDecompress -= localDecompressed;
                bytesDecompressed += localDecompressed;
            } catch (DataFormatException ex) {
                Logger.getLogger(CompressorIntMatrix.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        decompresser.end();

        // Decode the bytes into a String
        return convertByteArrayToInt(result, _m, _n);
    }

    public static void main(String[] args) {
        final int m = 400;
        final int n = 120;
        int[][] origData = new int[m][n];


        Random random = new Random();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                origData[i][j] = random.nextInt(2) + i + 7 * j - 4;
            }
        }

        CompressorIntMatrix compObj = new CompressorIntMatrix(origData);

        int[][] decompData = compObj.deCompress();
        System.out.println("decompressed:");
        for (int i = 0; i < decompData.length; i++) {
            for (int j = 0; j < decompData[0].length; j++) {
                assert decompData[i][j] == origData[i][j];
            }
        }

        System.out.println("compression ratio: " + compObj.compressionRatio);
        System.out.println("compression time:  " + compObj.compressionTime);
    }

    private static int[][] convertByteArrayToInt(byte[] buffer, int m, int n) {
        int[][] returnValue = new int[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int value = (0xFF & buffer[0 + 4 * i * n + 4 * j]) << 24;
                value |= (0xFF & buffer[1 + 4 * i * n + 4 * j]) << 16;
                value |= (0xFF & buffer[2 + 4 * i * n + 4 * j]) << 8;
                value |= (0xFF & buffer[3 + 4 * i * n + 4 * j]);
                returnValue[i][j] = value;
            }
        }

        return returnValue;
    }
    private static ConcurrentNavigableMap<Integer, byte[]> _byteArrayCache = new ConcurrentSkipListMap<Integer, byte[]>();
    
    private static byte[] getCachedByteArray(final int size) {
        byte[] isPresent = _byteArrayCache.remove(size);
        if (isPresent != null) {
            return isPresent;
        } else {
            return new byte[size];
        }
    }

    private static void recycleByteArray(final byte[] toRecycle) {
        if(_byteArrayCache.size() > 50) {
            _byteArrayCache.clear();
        }
        _byteArrayCache.put(toRecycle.length, toRecycle);
    }

    private static byte[] convertIntToByteArray(int[][] val) {

        
        int m = 0;
        if(val.length > 0) { // == 0 can happen when no scope is available!
            m = val[0].length;
        }

        byte[] buffer = getCachedByteArray(val.length * 4 * m);
        for (int i = 0; i < val.length; i++) {
            for (int j = 0; j < m; j++) {
                buffer[0 + 4 * j + m * 4 * i] = (byte) (val[i][j] >>> 24);
                buffer[1 + 4 * j + m * 4 * i] = (byte) (val[i][j] >>> 16);
                buffer[2 + 4 * j + m * 4 * i] = (byte) (val[i][j] >>> 8);
                buffer[3 + 4 * j + m * 4 * i] = (byte) val[i][j];
            }
        }

        return buffer;
    }

    /**
     *
     * @return the amount of compressed storage in Bytes
     */
    int getCompressedMemory() {
        return byteContainer.size() * BYTE_BLOCK_SIZE;
    }
    
    public static void clearCache() {
        _byteArrayCache.clear();
    }
    
}
