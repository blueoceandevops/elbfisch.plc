/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Data.java
 * VERSION   : -
 * DATE      : -
 * PURPOSE   : 
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 *
 * This file is part of the jPac process automation controller.
 * jPac is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jPac is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the jPac If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpac.plc.s7;

import java.io.IOException;
import org.jpac.plc.AddressException;
import org.jpac.plc.PlcString;
import org.jpac.plc.StringLengthException;

/**
 * used as a data storage for data interchanged with a plc.<br>
 * Implements a byte array and some accessor methods for<br>
 * several plc side datatypes.
 */
public class Data extends org.jpac.plc.Data{

    public Data(byte[] bytes){
        super(bytes);
    }

    /**
     * used to read a string value.
     * @param byteIndex byte offset inside the data buffer
     * @return the value
     * @throws AddressException
     */
    public PlcString getSTRING(int byteIndex) throws StringLengthException, AddressException{
        if (byteIndex < 0 || byteIndex +  1 >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid");
        }
        int maxLength    = bytes[byteIndex] & 0x000000FF;
        int actualLength = bytes[byteIndex+1] & 0x000000FF;
        if (byteIndex + 1 + maxLength >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid");
        }
        byte[] bString    = new byte[maxLength];
        System.arraycopy(bytes, byteIndex+2, bString, 0, maxLength);
        PlcString plcString = new PlcString(bString,actualLength);
        return plcString;
    }

    /**
     * used to set a string value.
     * @param byteIndex byte offset inside the data buffer
     * @throws AddressException
     */
    @Override
    public void setSTRING(int byteIndex, PlcString value) throws AddressException{
        if (byteIndex < 0 || byteIndex + 1 + value.getMaxLength() >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid or string too long: max. Length: " + value.getMaxLength());
        }
        bytes[byteIndex] = (byte)value.getMaxLength();
        bytes[byteIndex+1] = (byte)value.getActualLength();
        System.arraycopy(value.toString().getBytes(), 0, bytes, byteIndex+2, value.toString().length());
    }


    /**
     * used to read a chunk of data from the plc. Data is stored beginning at offset '0' inside the data buffer
     * @param conn an open connection to the plc
     * @param numOfBytes number of bytes to receive
     * @throws IOException
     */
    public void read(Connection conn, int numOfBytes) throws IOException{
        conn.getInputStream().read(bytes, 0, numOfBytes);
}

    /**
     * used to read a chunk of data from the plc. Data is stored beginning at a given offset inside the data buffer
     * @param conn an open connection to the plc
     * @param numOfBytes number of bytes to receive
     * @throws IOException
     */
    public void read(Connection conn, int offset, int numOfBytes) throws IOException{
        conn.getInputStream().read(bytes, offset, numOfBytes);
    }

    /**
     * used to write a chunk of data to the plc. Data is written beginning at offset '0' inside the data buffer
     * @param conn an open connection to the plc
     * @param numOfBytes number of bytes to receive
     * @throws IOException
     */
    public void write(Connection conn, int numOfBytes) throws IOException{
        conn.getOutputStream().write(bytes, 0, numOfBytes);
    }

    /**
     * used to write a chunk of data to the plc. Data is written beginning at the given offset inside the data buffer
     * @param conn an open connection to the plc
     * @param numOfBytes number of bytes to receive
     * @throws IOException
     */
    public void write(Connection conn, int offset, int numOfBytes) throws IOException{
        conn.getOutputStream().write(bytes, offset, numOfBytes);
    }

}
