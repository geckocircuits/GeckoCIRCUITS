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
package ch.technokrat.gecko.geckoscript;

import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JTextArea;

/**
 * Simple way to "print" to a JTextArea; just say
 * PrintStream out = new PrintStream(new TextAreaOutputStream(myTextArea));
 * Then out.println() et all will all appear in the TextArea.
 */
public final class TextAreaOutputStream extends OutputStream {

	private final JTextArea textArea;
	private final StringBuilder sb = new StringBuilder(4048);

	public TextAreaOutputStream(final JTextArea textArea) {
		this.textArea = textArea;
	}

    @Override
    public void flush(){ }
    
    @Override
    public void close(){ }

	@Override
	public void write(int b) throws IOException {                
		if (b == '\r')
			return;
		
		if (b == '\n') {                    
			textArea.append(sb.toString());                        
			sb.setLength(0);
		}
		
		sb.append((char)b);                
	}
}