/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : OutputStream.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : OutputStream is used to convert an outgoing stream of
 *             binary data to be transmitted to the Plc controller from java native
 *             datatypes to Plc data types:
 *
 *              Plc data type  Size (bytes)            java data type
 *              ________________________________________________________
 *              BYTE                1                       byte
 *              WORD                2                       int
 *              DWORD               4                       long
 *              INT                 2                       int
 *              DINT                4                       int
 *              STRING              (n)+ 2                  PlcString *1)
 *
 *              *1) the string representation of a S7 Plc looks like follows:
 *                  1. Byte:    maximum number of char's
 *                  2. Byte:    actual number of char's stored inside the string
 *
 *
 *             In addition swapping of the byte order is done to convert from
 *             big endian to little endian<br>
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: OutputStream.java,v $
 *
 * This file is part of the jPac PLC communication library.
 * The jPac PLC communication library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The jPac PLC communication library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the jPac PLC communication library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */



/**
 * PROJECT   : jPac S7 communication library
 * MODULE    : OutputStream.java
 * VERSION   : $Revision: 1.4 $
 * DATE      : $Date: 2011/02/21 14:09:41 $
 * RELEASE   : $Name: HEAD $
 * PURPOSE   : OutputStream is used to convert an outgoing stream of
 *             binary data to be transmitted to the Plc controller from java native
 *             datatypes to Plc data types:
 *
 *              Plc data type  Size (bytes)            java data type
 *              ________________________________________________________
 *              BYTE                1                       byte
 *              WORD                2                       int
 *              DWORD               4                       long
 *              INT                 2                       int
 *              DINT                4                       int
 *              STRING              (n)+ 2                  PlcString *1)
 *
 *              *1) the string representation of a S7 Plc looks like follows:
 *                  1. Byte:    maximum number of char's
 *                  2. Byte:    actual number of char's stored inside the string
 *
 *
 *             In addition swapping of the byte order is done to convert from
 *             big endian to little endian<br>
 *
 * AUTHOR    : @author B. Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld <br>
 * SUBSTITUTE: Andreas Ulbrich<br>
 * REMARKS   : - <br>
 * CHANGES   : CH#(n) (short name) (date) (discription) <br>
 * CHECKED   : <br>
 * LOG       : $Log: OutputStream.java,v $
 * LOG       : Revision 1.4  2011/02/21 14:09:41  schuster
 * LOG       : comments added, and some minor changes
 * LOG       :
 * LOG       : Revision 1.3  2010/07/28 19:56:39  schuster
 * LOG       : logging over log4j implemented
 * LOG       :
 * LOG       : Revision 1.2  2010/06/23 12:05:00  schuster
 * LOG       : after implementing Data class
 * LOG       :
 * LOG       : Revision 1.1  2010/05/20 08:14:36  schuster
 * LOG       : first commit
 * LOG       :
 *
 *  This file is part of the jPac S7 communication library.
 *  It is based on the valuable work of Thomas Hergenhahn in his libnodave project.
 *  We made some effort to reimplement a subset of the library to get a robust Java implementation,
 *  which is more object oriented and more stable than the original java port of libnodave.

    The jPac S7 communication library is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The jPac S7 communication library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the jPac S7 communication library.  If not, see <http://www.gnu.org/licenses/>.

 */

package org.jpac.plc.s7;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * OutputStream is used to send a sequence of<br>
 * java native type data items to the plc<br>
 * During transmission the data items are converted to plc data types.<br>
 * <br>
 * Suppported plc datatypes: BYTE,WORD,DWORD,INT,DINT,STRING<br>
 * <br>
 * In addition swapping of the byte order is done to convert from<br>
 * little endian to big endian<br>
 * <br>
 * @see PlcInputStream
 */
public class OutputStream extends java.io.FilterOutputStream{
    static Logger Log = Logger.getLogger("jpac.plc.s7");

    /**
     * Indicates that the stream is meant to be operational.
     * Is set to false, if an IOException occurs while
     * accessing the stream.
     */
    private boolean               operational;
    private boolean               debug;      //indicates debug mode

    public OutputStream(java.io.OutputStream out){
        super(out);
        operational = true;
        debug       = false;
    }
                    
    /**
     * used to send a string over the stream. The string is converted to Plc format and appended to the data already written to
     * the buffer since the last flush() operation.
     * @param str string to write
     * @param maxlen maximum number length of the string
     * @exception IOException 
     */
    public synchronized void writeSTRING(String str, int maxlen) throws IOException {
        int strlen = 0;
        if (!isOperational()){
           throw new IOException("stream not operational");
        }
        strlen = str.length();
        if (strlen > maxlen)
           throw new IOException("string too long");
        write((byte) (0xff & maxlen));//send maximum length of the string
        write((byte) (0xff & strlen));//send actual length of the string
        //send string characters
        for (int i = 0 ;i < strlen; i++){
            write((byte) (0xff & str.charAt(i)));
        }
        //send padding chars
        for (int i = strlen ;i < maxlen; i++){
            write((byte)0x00);
        }
    }

    /**
     * used to write a BYTE value to the buffer. The value is converted to Plc format and appended to the data already written to
     * the buffer since the last flush() operation.
     * @param value
     * @exception IOException
     */
    public synchronized void writeByte(short value) throws IOException { //write bytes in little endian order
        if (!isOperational()){
           throw new IOException("stream not operational");
        }
        write((byte) (0xff & value));
    }

    /**
     * used to write a int value to the buffer. The value is converted to Plc format an appended to the data already written to
     * the buffer since the last flush() operation.
     * @param value 
     * @exception IOException 
     */
    public synchronized void writeINT(int value) throws IOException { //write bytes in little endian order
        if (!isOperational()){
           throw new IOException("stream not operational");
        }          
        if(value  < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new IOException("value " + value + " too less or too big for INT");
        }
        write((byte) (0xff & (value >> 8)));
        write((byte) (0xff & value));
    }

    /**
     * used to write a int value to the buffer. The value is converted to Plc format an appended to the data already written to
     * the buffer since the last flush() operation.
     * @param value 
     * @exception IOException 
     */
    public synchronized void writeDINT(int value) throws IOException { //write bytes in little endian order
        if (!isOperational()){
           throw new IOException("stream not operational");
        }          
        write((byte) (0xff & (value >> 24)));
        write((byte) (0xff & (value >> 16)));
        write((byte) (0xff & (value >> 8)));
        write((byte) (0xff & value));
    }
    
    /**
     * used to write a WORD value to the buffer. The value is converted to Plc format an appended to the data already written to
     * the buffer since the last flush() operation.
     * @param value 
     * @exception IOException 
     */
    public synchronized void writeWORD(int value) throws IOException { //write bytes in little endian order
        if (!isOperational()){
           throw new IOException("stream not operational");
        }
        if(value  < 0 || value > 0xffff) {
            throw new IOException("value " + value + " too less or too big for WORD");
        }
        write((byte) (0xff & (value >> 8)));
        write((byte) (0xff & value));
    }    
    
    /**
     * used to write a DWORD value to the buffer. The value is converted to Plc format an appended to the data already written to 
     * the buffer since the last flush() operation.
     * @param value 
     * @exception IOException 
     */
    public synchronized void writeDWORD(long value) throws IOException { //write the lower 4 bytes in little endian order
        if (!isOperational()){
           throw new IOException("stream not operational");
        }
        if (value > 4294967295L)
            throw new IOException("Plc LONG value out of range :" + value);
        write((byte) (0xff & (value >> 24)));
        write((byte) (0xff & (value >> 16)));
        write((byte) (0xff & (value >> 8)));
        write((byte) (0xff & value));
    }

    /**
     * write byte b to the output stream
     * @param b: byte value to be written
     * @throws IOException
     */
    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    /**
     * used to check, if the output stream is currently operational
     * @return
     */
    public boolean isOperational() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    /**
     * enable/disable the generation of debug information
     * @param debug
     */
    public void setDebug(boolean debug){
        this.debug = debug;
    }
}
