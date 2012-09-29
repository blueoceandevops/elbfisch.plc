/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : InputStream.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : InputStream is used to convert an incoming input stream of
 *             binary data received from the Plc controller into java native
 *             datatypes:
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
 *             In addition swapping of the byte order is done to convert from
 *             little endian to big endian<br>
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
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
package org.jpac.plc.s7;

import java.io.*;
import org.apache.log4j.Logger;

/**
 * InputStream is used to convert an incoming input stream of<br>
 * binary data received from the Plc controller into java native<br>
 * datatypes:<br>
 * Suppported plc datatypes: BYTE,WORD,DWORD,INT,DINT,STRING<br>
 * <br>
 * In addition swapping of the byte order is done to convert from<br>
 * little endian to big endian<br>
 */
public class InputStream extends java.io.FilterInputStream {
    static Logger Log = Logger.getLogger("jpac.plc.s7");

    private byte[] buf = new byte[8];
    
    private final int MAXWAITTIME =    5000; //max. period of time to wait for
                                             //from the Plc controller in ticks (see ONETICK)
    private final int ONETICK     =       1; //duration of one tick in milliseconds
    
    /**
     * indicates that the stream is meant to be operational
     * is set to false, if an IOException occurs while
     * accessing the stream.
     */
    private boolean      operational = true;
    

    private boolean               debug;      //indicates debug mode
    
    public InputStream(java.io.InputStream in){
        super(in);
        operational = true;
        debug       = false;
    }

    /**
    * used to read a byte value from the input stream.
    * @throws java.io.IOException might be thrown due to an I/O error
    * @return the byte value read from the stream
    */
    @Override
    public int read() throws IOException
    {
     if (!isOperational()){
         throw new IOException("stream not operational");
     }
     waitForBytes(1);
     readFully(buf, 0, 1);
     return convertToByte(buf);
    }

    /**
    * used to read a byte value from the input stream.
    * @throws java.io.IOException might be thrown due to an I/O error
    * @return the byte value read from the stream
    */
    public byte readBYTE() throws IOException
    {
    if (!isOperational()){
        throw new IOException("stream not operational");
    }
    waitForBytes(1);
    readFully(buf, 0, 1);
    return convertToByte(buf);
    }


    /**
    * used to read an int value from the input stream. While reading data
    * conversion from the Plc format to java format is done.
    * @throws java.io.IOException might be thrown due to an I/O error
    * @return the int value read from the stream
    */
    public int readINT() throws IOException
    {
    if (!isOperational()){
        throw new IOException("stream not operational");
    }
    int int_rc = readWORD();
    //adjust sign before return
    return int_rc > Short.MAX_VALUE ? int_rc | 0xFFFF0000 : int_rc;
    }

    /**
    * used to read an int value from the input stream. While reading data
    * conversion from the Plc format to java format is done.
    * @throws java.io.IOException might be thrown due to an I/O error
    * @return the int value read from the stream
    */
    public int readDINT() throws IOException
    {
    if (!isOperational()){
        throw new IOException("stream not operational");
    }
    waitForBytes(4);
    readFully(buf, 0, 4);
    int int_rc = convertToInt(buf);
    return int_rc; //convertToInt(buf);
    }

    /**
    * used to read a word value (2 Byte) from the input stream. While reading data
    * conversion from the Plc format to java format is done.
    * @throws java.io.IOException might be thrown due to an I/O error
    * @return the int value read from the stream
    */
    public int readWORD() throws IOException
    {
    if (!isOperational()){
        throw new IOException("stream not operational");
    }
    waitForBytes(2);
    readFully(buf, 0, 2);
    //System.out.printf(" read word %x %x",buf[0],buf[1]);
    return convertToUnsignedShort(buf);
    }

    /**
    * used to read a long value from the input stream. While reading data
    * conversion from the Plc format to java format is done.
    * @throws java.io.IOException might be thrown due to an I/O error
    * @return the long value read from the stream
    */
    public long readDWORD() throws IOException
    {
    if (!isOperational()){
        throw new IOException("stream not operational");
    }
    waitForBytes(4);
    readFully(buf, 0, 4);
    return convertToLong(buf);
    }

    /**
    * used to read a string value from the input stream. While reading data
    * conversion from the Plc format to java format is done.
    * @return the string value read from the stream
    * @param len expected length of the string to read
    * @throws java.io.IOException might be thrown due to an I/O error
    */
    public String readSTRING(int len) throws IOException
    {
    String str = "";
    int maxlen = 0;
    int strlen = 0;
    if (!isOperational()){
        throw new IOException("stream not operational");
    }

    waitForBytes(len+2);
    //read max length of the string
    readFully(buf, 0, 1);
    maxlen = (int)buf[0];
    //read actual length of the string
    readFully(buf, 0, 1);
    strlen = (int)buf[0];
    if (len != maxlen || strlen > maxlen)
        throw new IOException("string format error len = " + len + ", maxlen = " + maxlen + "strlen: " + strlen);
    for (int i = 0; i < strlen; i++){
        readFully(buf, 0, 1);
        str += convertToChar(buf);
    }
    //skip padding chars
    for (int i = strlen ;i < maxlen; i++){
        readFully(buf, 0, 1);
    }
    return str;
    }

    /**
     * reads buffer.length bytes from the input stream to buffer.
     * @param buffer the buffer the received data is stored in
     * @throws IOException
     */
    public void readFully(byte[] buffer) throws IOException
    {
        readFully(buffer, 0, buffer.length);
    }
    
    /**
     * reads len bytes from the input stream and places them inside buffer starting at offset
     * @param buffer the buffer the received data is stored in
     * @param offset byte offset inside the buffer
     * @param len number of bytes to be read from the input stream
     * @throws IOException
     */
    public void readFully(byte[] buffer, int offset, int len) throws IOException
    {
        if (!isOperational()){
            throw new IOException("stream not operational");
        }
        int numread = 0;
        if (len < 0)
          throw new IndexOutOfBoundsException("Index: "+len);
        while (len > 0){
            try{
                numread = in.read(buffer, offset, len);
            }
            catch(IOException exc){
                setOperational(false);
                throw exc;
            }
            if (numread < 0){
              setOperational(false);
              throw new EOFException();
            }
            len -= numread;
            offset += numread;
        }
    }

    /**
    * used to convert an char (1 byte) value from Plc to java format
    * @return the char value
    * @param buf byte buffer from which to take the bytes to be converted to
    * the target type
    */
    protected char convertToChar(byte[] buf)
    {
    return (char) buf[0];
    }

    /**
    * used to convert an byte value from Plc to java format
    * @return the byte value
    * @param buf byte buffer from which to take the bytes to be converted to
    * the target type
    */
    protected byte convertToByte(byte[] buf)
    {
    return buf[0];
    }

    /**
    * used to convert an int value from Plc to java format
    * @return the int value
    * @param buf byte buffer from which to take the bytes to be converted to
    * the target type
    */
    protected int convertToInt(byte[] buf)
    { //swap byte order
    return (((buf [ 0 ] & 0xff) << 24) | ((buf [ 1 ] & 0xff) << 16) |
            ( (buf [ 2 ] & 0xff) <<  8) |  (buf [ 3 ] & 0xff));
    }

    /**
    * used to convert an word value 2 bytes) from Plc to java format
    * @return the int value
    * @param buf byte buffer from which to take the bytes to be converted to
    * the target type
    */
    protected int convertToUnsignedShort(byte[] buf)
    { //swap byte order
      return ( ( (buf [ 0 ] & 0xff) <<  8) |  (buf [ 1 ] & 0xff));
    }

    /**
    * used to convert a long value from Plc to java format
    * @return the long value
    * @param buf byte buffer from which to take the bytes to be converted to
    * the target type
    */
    protected long convertToLong(byte[] buf)
    { //swap byte order
    return(((long) (buf [ 0 ] & 0xff) << 24) | ((long) (buf [ 1 ] & 0xff) << 16) |
           ((long) (buf [ 2 ] & 0xff) <<  8) | ((long) (buf [ 3 ] & 0xff)      ));
    }

    /**
    * used to wait, until the given amount of bytes are available on the stream<br>
    * if a timeout occurs, an IOException is thrown<br>
    * (see instance variables MAXWAITTIME, ONETICK for further information<br>
    * @param number of bytes to wait for
    * @exception IOException
    */
    protected void waitForBytes(int n)throws IOException
    {
    if (!isOperational()){
        throw new IOException("stream not operational");
    }
    int ticks = 0;
    while((available() < n) && (ticks++ < this.MAXWAITTIME)){
        try{Thread.currentThread().sleep(this.ONETICK);}catch(InterruptedException exc){};
    }
    if (ticks >= MAXWAITTIME){
        //if the Plc controller does not answer in time
        //abort reception
        setOperational(false);
        throw new IOException("incomplete data packet received from Plc controller");
    }
    }

    /**
     * used to remove all pending bytes inside the input streams
     * @exception IOException
     * @return the double value
     */
    public int clear() throws IOException {
         int nbytes = available();
         if (nbytes > 0) {
             skip(nbytes);
         }
         return nbytes;
    }

    /**
     * indicates, if the input stream is operational
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
