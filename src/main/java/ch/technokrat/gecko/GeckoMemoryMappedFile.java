/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/** 
 * This class encapsulates the memory-mapped file (MMF) used for the alternative GeckoRemote interface.
 * It contains the pointer to the file and the objects used to write and read to it. It is the sole interface between the memory-mapping "plumbing" and the rest 
 * of GeckoCIRCUITS / GeckoRemote.
 * @author andrija s.
 */
public class GeckoMemoryMappedFile {
     
    private final File _file; //the file being used for communication
    private final MappedByteBuffer _mmb; //the memory-mapped byte buffer the file is mapped to
    
    private static final int CONNECTION_ID_POS = 0; //the position of the connection ID in the buffer
    private static final int STATUS_ID_POS = 8; //the position of the file status ID in the buffer
    private static final int BUFFER_SIZE_POS = 16; //the position of the buffer size in the buffer
    private static final int PIPE_OBJECT_SIZE_POS = 24;
    private static final int PIPE_OBJECT_POS = 32; //the position of the serialized pipe object in the buffer
    
    public static final long _defaultBufferSize = 10000000; //the initial size of the buffer
    
    //the status ID values
    private static final long DISCONNECTED = -1;
    private static final long IDLE = 0;
    private static final long CONNECTION_ATTEMPT = 2;
    private static final long CONNECTION_REJECTED = -2;
    private static final long CONNECTION_ACCEPTED = 3;
    private static final long METHOD_CALL = 4;
    private static final long METHOD_RETURN = 5;
    private static final long DISCONNECT_REQUEST = 6;
    private static final long SHUTDOWN_REQUEST = 7;
    
    /**
     * Create a new memory mapped file of a given size.
     * This constructor is used on the GeckoCIRCUITS side, to open up GeckoCIRCUITS to remote access.
     * @param fileName the name of the file to be mapped to memory
     * @param bufferSize the size of the buffer, in bytes
     * @throws FileNotFoundException if the file of the given name cannot be found / created
     * @throws IOException if something goes wrong in the process of memory-mapping
     */
    public GeckoMemoryMappedFile(final String fileName, final long bufferSize) throws FileNotFoundException, IOException {
        _file = new File(fileName);
        final FileChannel fileChannel = new RandomAccessFile(_file,"rw").getChannel();
        _mmb = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
        
        //default connection ID is -1 - disconnected
        //default state is disconnected
        _mmb.putLong(CONNECTION_ID_POS,-1);
        _mmb.putLong(STATUS_ID_POS,DISCONNECTED);
        _mmb.putLong(BUFFER_SIZE_POS,bufferSize);
    }
    
    /**
     * Create a new memory mapped file of the default size.
     * This constructor is used on the GeckoCIRCUITS side, to open up GeckoCIRCUITS to remote access.
     * @param fileName the name of the file to be mapped to memory
     * @throws FileNotFoundException if the file of the given name cannot be found / created
     * @throws IOException if something goes wrong in the process of memory-mapping
     */
    public GeckoMemoryMappedFile(final String fileName) throws FileNotFoundException, IOException {
        this(fileName,_defaultBufferSize);
    }
    
    /**
     * Private constructor used by the factory method to return a GeckoMemoryMappedFile object pointing to an already existing
     * memory mapped-file created by GeckoCIRCUITS to be used for remote access.
     * @param file the file object for this file
     * @param mmb the memory mapped byte buffer corresponding to this file
     */
    private GeckoMemoryMappedFile(final File file, final MappedByteBuffer mmb) {
        _file = file;
        _mmb = mmb;
    }
    
    /**
     * Factory method used by the CLIENT connecting to GeckoCIRCUITS to receive a mapped file object for use for remote access to GeckoCIRCUITS.
     * @param fileName the name of the file
     * @return the memory mapped object containing this file
     * @throws FileNotFoundException if the file of the given name cannot be found / created
     * @throws IOException if something goes wrong in the process of memory-mapping
     */
    public static GeckoMemoryMappedFile getGeckoMemoryMappedFile(final String fileName) throws FileNotFoundException, IOException {
        final File file = new File(fileName);
        //given file must exist already!
        if (!file.exists()) {
            throw new FileNotFoundException("The file for remote access: " + fileName + " does not exist. Please check if GeckoCIRCUITS is enabled for remote access.");
        }
        //create initial memory mapped byte buffer to read buffer size
         final FileChannel fileChannel = new RandomAccessFile(file,"rw").getChannel();
         final MappedByteBuffer init_mmb = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 24);
         final long bufferSize = init_mmb.getLong(BUFFER_SIZE_POS);
         final MappedByteBuffer mmb = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
         return new GeckoMemoryMappedFile(file,mmb);        
    }
    
    /**
    *  Get the connection ID of this file.
    * @return the connection ID:
    *          -1 - no connection
    *           0 - connection attempt in progress
    *         > 0 - connection established
    */
    public long getConnectionID() {
        return _mmb.getLong(CONNECTION_ID_POS);
    }
    
    /**
     * Get the memory-mapped buffer size of this file.
     * @return the buffer size in bytes
     */
    public long getBufferSize() {
        return _mmb.getLong(BUFFER_SIZE_POS);
    }
    
    /**
     * Check if the file is free to accept a connection.
     * Connection ID must be -1 and status must be 'disconnected'.
     * @return true if it is free, false otherwise
     */
    public boolean isFree() {
        return ((-1 == _mmb.getLong(CONNECTION_ID_POS)) && (DISCONNECTED == _mmb.getLong(STATUS_ID_POS)));
    }
    
    /**
     * Used by CLIENT ONLY to connect to GeckoCIRCUITS via this file.
     * Will block until a connection request is answered or the timeout expires.
     * @param timeout the timeout time for the connection in milliseconds.
     * @return -2 if timeout, no connection
     *         -1 if file is occupied by another connection
     *          0 if connection is rejected
     *          an connection ID > 0 if connection is accepted
     */
    public long connect(final long timeout) {
        if (isFree()) {
            long status = CONNECTION_ATTEMPT;
            _mmb.putLong(CONNECTION_ID_POS,0);
            _mmb.putLong(STATUS_ID_POS,status);
            final long startTime = System.currentTimeMillis();
            while (true) {
                //check if status has changed
                status = _mmb.getLong(STATUS_ID_POS);
                if (status != CONNECTION_ATTEMPT) {
                    if (status == CONNECTION_REJECTED) {
                        _mmb.putLong(STATUS_ID_POS,DISCONNECTED);
                        return 0;
                    } else if (status == CONNECTION_ACCEPTED) {
                        final long connectionID = getConnectionID();
                        if (connectionID > 0) {
                            _mmb.putLong(STATUS_ID_POS,IDLE);
                            return connectionID;
                        } else {
                            return 0;
                        }
                    }
                } else if ((System.currentTimeMillis() - startTime) > timeout) {
                    return -2;
                }
            }
        } else {
            return -1;
        }
    }
    
    /**
     * Used by the SERVER (GeckoCIRCUITS) to reject a connection request.
     */
    public void rejectConnection() {
        _mmb.putLong(CONNECTION_ID_POS,-1);
        _mmb.putLong(STATUS_ID_POS,CONNECTION_REJECTED);
        //we wait a certain time to see if client has seen that he has been rejected;
        //if not, we do a force disconnect
        final int wait = 1000; //wait time in ms
        try {
            Thread.sleep(wait);
        } catch (InterruptedException ex) {
            forceDisconnect();
        } 
        if (_mmb.getLong(STATUS_ID_POS) == CONNECTION_REJECTED) {
            _mmb.putLong(STATUS_ID_POS,DISCONNECTED);
        }
    }
    
    
    /**
     * Used by the SERVER (GeckoCIRCUITS) to accept a connection request.
     * @param connectionID the unique random-generated connection ID for this connection (must > 0)
     */
    public void acceptConnection(final long connectionID) {
        _mmb.putLong(CONNECTION_ID_POS,connectionID);
        _mmb.putLong(STATUS_ID_POS,CONNECTION_ACCEPTED);
    }
    
    /**
     * Check if the incoming connection ID of a request transaction matches the active connection ID.
     * @param connectionID the connection ID to check
     * @throws RuntimeException if IDs don't match
     */
    private void checkConnectionID(final long connectionID) {
        if (connectionID != getConnectionID()) {
            throw new RuntimeException("Given connection ID " + connectionID + " does not match active connection ID " + getConnectionID());
        }
    }    
    
    /**
     * Used by either side to send a disconnection request.
     * @param connectionID the ID of the connection to disconnect. Must match the active connection.
     */
    public void disconnect(final long connectionID) {
        checkConnectionID(connectionID);       
        _mmb.putLong(STATUS_ID_POS,DISCONNECT_REQUEST);
    }
    
    /**
     * Used by either side to confirm a disconnection request, or by GeckoCIRCUITS to forcibly disconnect a client.
     */
    public void forceDisconnect() {
        _mmb.putLong(CONNECTION_ID_POS,-1);
        _mmb.putLong(STATUS_ID_POS,DISCONNECTED);
    }
    
    /**
     * This method serializes a GeckoRemotePipeObject into a byte array, which can then be written into the file.
     * @param pipeObject the object to serialize
     * @return the byte array containing the serialized pipe object
     * @throws IOException if something goes wrong with the serialization
     */
    private byte[] serializePipeObject(final GeckoRemotePipeObject pipeObject) throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final ObjectOutputStream pipeStream = new ObjectOutputStream(byteStream);
        pipeStream.writeObject(pipeObject);
        return byteStream.toByteArray();
    }
    
    /**
     * This method de-serializes a GeckoRemotePipeObject from a byte array in the file.
     * @param serializedPipe
     * @return the GeckoRemotePipeObject contained in the byte array read from the file
     * @throws IOException if something goes wrong reading the byte array
     * @throws ClassNotFoundException if the object cannot be reconstructed properly
     */
    private GeckoRemotePipeObject deserializePipeObject(final byte[] serializedPipe) throws IOException, ClassNotFoundException {
        final ObjectInputStream pipeStream = new ObjectInputStream(new ByteArrayInputStream(serializedPipe));
        return ((GeckoRemotePipeObject) pipeStream.readObject());
    }
    
    /**
     * This method calls a GeckoRemote method via the memory-mapped file.
     * The description of the method, along with the arguments, must be encapsulated in a GeckoRemotePipeObject. This object is serialized
     * and written into the memory-mapped file. This method then blocks until the memory-mapped file contains a response to the GeckoRemote method call.
     * @param connectionID the ID of the connection. Must match the active connection ID
     * @param methodObject the object describing the method call
     * @return a new GeckoRemotePipeObject which contains the response to the GeckoRemote function call
     * @throws IOException if something goes wrong reading or writing the memory-mapped file or (de)serializing the pipe object
     * @throws ClassNotFoundException if the pipe object cannot be reconstructed properly
     */
    public GeckoRemotePipeObject callMethod(final long connectionID, final GeckoRemotePipeObject methodObject) throws IOException, ClassNotFoundException {
        checkConnectionID(connectionID);
        if (methodObject.isMethodCall()) {
            final byte[] serializedPipe = serializePipeObject(methodObject);
            final long serializedPipeSize = serializedPipe.length;
            _mmb.putLong(PIPE_OBJECT_SIZE_POS,serializedPipeSize);
            //must load byte by byte into buffer
            final int startIndex = PIPE_OBJECT_POS;
            for (int i = startIndex; i < (startIndex + serializedPipeSize); i++) {
                _mmb.put(i, serializedPipe[i - startIndex]);
            }
            _mmb.putLong(STATUS_ID_POS,METHOD_CALL);
            //now we wait for a response
            long status;
            long returnedPipeSize;
            byte[] returnedSerializedPipe;
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                //do nothing
            }
            while (true) {
                status = _mmb.getLong(STATUS_ID_POS);
                if (status == DISCONNECT_REQUEST) {
                    forceDisconnect();
                    throw new RuntimeException("GeckoCIRCUITS has unexpectedly terminated the connection!");
                } else if (status == DISCONNECTED) {
                    throw new RuntimeException("GeckoCIRCUITS has unexpectedly terminated the connection!");
                } else if (status == IDLE) {
                    throw new RuntimeException("GeckoCIRCUITS did not respond to method call for unknown reason.");
                } else if (status != METHOD_CALL) {
                    if (status == METHOD_RETURN) {
                        returnedPipeSize = _mmb.getLong(PIPE_OBJECT_SIZE_POS);
                        returnedSerializedPipe = new byte[(int)returnedPipeSize];
                        //read byte by byte
                        for (int i = startIndex; i < (startIndex + returnedPipeSize); i++) {
                            returnedSerializedPipe[i-startIndex] = _mmb.get(i);
                        }
                        final GeckoRemotePipeObject response = deserializePipeObject(returnedSerializedPipe);
                        setIdle();
                        return response;
                    } else {
                        throw new RuntimeException("Unknown error!");
                    }
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
                    //do nothing
                }
            }
        } else {
            throw new RuntimeException("Given GeckoRemotePipeObject does not represent a method call!");
        }
    }
    
    /**
     * Check if the active connection is idle.
     * @return true if the status of the connection is idle
     */
    public boolean isIdle() {
        return (_mmb.getLong(STATUS_ID_POS) == IDLE);
    }
    
    /**
     * Check if a method call has been written into the file.
     * @return true if a method call is pending
     */
    public boolean isMethodCallPresent() {
        return (_mmb.getLong(STATUS_ID_POS) == METHOD_CALL);
    }
    
    /**
     * Retrieves a pipe object if present in the file. Used by SERVER side only.
     * @param connectionID the ID of the connection. Must match the active connection ID
     * @return the GeckoRemotePipeObject in the memory-mapped file
     * @throws IOException if something goes wrong reading the memory-mapped file or de-serializing the pipe object
     * @throws ClassNotFoundException if the pipe object cannot be reconstructed properly
     */
    public GeckoRemotePipeObject getPipeObject(final long connectionID) throws IOException, ClassNotFoundException {
        checkConnectionID(connectionID);
        final long pipeSize = _mmb.getLong(PIPE_OBJECT_SIZE_POS);
        final byte[] serializedPipe = new byte[(int) pipeSize];
        final int startIndex = PIPE_OBJECT_POS;
        //read byte by byte
        for (int i = startIndex; i < (startIndex + pipeSize); i++) {
            serializedPipe[i - startIndex] = _mmb.get(i);
        }
        return deserializePipeObject(serializedPipe);
    }
    
    /**
     * Set the status of the connection to idle.
     */
    public void setIdle() {
        _mmb.putLong(STATUS_ID_POS,IDLE);
    }
    
    /**
     * Check if a client is trying to make a connection (used by SERVER only).
     * @return true if there is a connection attempt
     */
    public boolean isConnectionAttempt() {
        return (_mmb.getLong(STATUS_ID_POS) == CONNECTION_ATTEMPT && _mmb.getLong(CONNECTION_ID_POS) == 0);
    }
    
    /**
     * Check if either side has requested a disconnection.
     * @return true if a disconnect request is present
     */
    public boolean isDisconnectRequest() {
        return (_mmb.getLong(STATUS_ID_POS) == DISCONNECT_REQUEST);
    }
    
    /**
     * Respond to a method call from the client via the memory-mapped file.
     * The response (whatever it might be) is encapsulated in a GeckoRemotePipeObject. This object is serialized, and written
     * to the file. The method returns immediately after this is done.
     * @param connectionID the ID of the connection. Must match the active connection ID
     * @param methodReturn the pipe object to be written to the file
     * @throws IOException if something goes wrong writing the object to the file
     */
    public void respondToMethodCall(final long connectionID, final GeckoRemotePipeObject methodReturn) throws IOException {
        checkConnectionID(connectionID);
        if (methodReturn.isMethodCall()) {
            throw new RuntimeException("Given GeckoRemotePipeObject does not represent a return from a method!");
        } else {
            final byte[] serializedPipe = serializePipeObject(methodReturn);
            final long serializedPipeSize = serializedPipe.length;
            _mmb.putLong(PIPE_OBJECT_SIZE_POS,serializedPipeSize);
            //must load byte by byte into buffer
            final int startIndex = PIPE_OBJECT_POS;
            for (int i = startIndex; i < (startIndex + serializedPipeSize); i++) {
                _mmb.put(i, serializedPipe[i - startIndex]);
            }
            _mmb.putLong(STATUS_ID_POS,METHOD_RETURN);
        }
    }
    
    /**
     * Delete the memory-mapped file. Used ONLY BY GeckoCIRCUITS (SERVER)!
     * This object becomes useless after this method is called. Used only for disabling remote access completely via this file.
     */
    public void deleteFile() {
        forceDisconnect();
        _file.delete();
    }
    
    /**
     * Get the name of the file used for memory-mapped access.
     * @return the absolute path to the file
     */
    public String getFileName() {
        return _file.getAbsolutePath();
    }
    
    /**
     * Used by the client to tell GeckoCIRCUITS to shutdown.
     * @param connectionID the session ID, must match active session
     */
    public void shutdown(final long connectionID) {
        checkConnectionID(connectionID);
         _mmb.putLong(STATUS_ID_POS,SHUTDOWN_REQUEST);
    }
    
    /**
     * Check if the client has requested a shutdown.
     * @return true if a shutdown request is present
     */
    public boolean isShutdownRequest() {
        return (_mmb.getLong(STATUS_ID_POS) == SHUTDOWN_REQUEST);
    }
}
