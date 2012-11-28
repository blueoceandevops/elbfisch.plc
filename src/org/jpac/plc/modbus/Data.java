/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Data.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : MODBUS Data object
 * AUTHOR    : Andreas Ulbrich, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: Data.java,v $
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


package org.jpac.plc.modbus;

import org.jpac.plc.AddressException;
import org.jpac.plc.ValueOutOfRangeException;

/**
 * used as a data storage for data interchanged with a plc.<br>
 * Implements a byte array and some accessor methods for<br>
 * several plc side datatypes.
 */
public class Data extends org.jpac.plc.Data {
    public Data(byte[] bytes){
        super(bytes);
    }

    /**
     * used to read a word value. The value is treated as an unsigned 16 bit value: 0..65535
     * @param byteIndex byte offset inside the data buffer
     * @return the word value
     * @throws AddressException
     */
    @Override
    public int getWORD(int byteIndex) throws AddressException {
        if (byteIndex < 0 || byteIndex + 1 >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid");
        }
        int highByte = (getBytes()[byteIndex + 1] & 0x000000FF) << 8;
        return highByte + (getBytes()[byteIndex] & 0x000000FF);
    }

    /**
     * used to set a word value. The value is treated as an unsigned 16 bit value: 0..65535
     * @param byteIndex byte offset inside the data buffer
     * @param value the value
     * @throws AddressException
     */
    @Override
    public void setWORD(int byteIndex, int value) throws AddressException, ValueOutOfRangeException {
        if (value > 0x0000FFFF || value < 0){
            throw new ValueOutOfRangeException("value: " + value);
        }
        if (byteIndex < 0 || byteIndex + 1 >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid");
        }
        getBytes()[byteIndex + 1] = (byte)(value >> 8);
        getBytes()[byteIndex]     = (byte)value;
    }


    /**
     * used to set an int value. The value is treated as a signed 16 bit value: -32,768 to 32,767
     * @param byteIndex byte offset inside the data buffer
     * @param value the value
     * @throws AddressException
     */
    @Override
    public void setINT(int byteIndex, int value) throws AddressException, ValueOutOfRangeException {
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE){
            throw new ValueOutOfRangeException("value: " + value);
        }
        if (byteIndex < 0 || byteIndex + 1 >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid");
        }
        getBytes()[byteIndex + 1] = (byte)(value >> 8);
        getBytes()[byteIndex]     = (byte)value;
    }

    /**
     * used to read a dword value. The value is treated as an unsigned 32 bit value: 4,294,967,295
     * @param byteIndex byte offset inside the data buffer
     * @return the value
     * @throws AddressException
     */
    @Override
    public long getDWORD(int byteIndex) throws AddressException {
        if (byteIndex < 0 || byteIndex + 3 >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid");
        }
        long value =                (bytes[byteIndex + 3] & 0x000000FF);
             value = (value << 8) + (bytes[byteIndex + 2] & 0x000000FF);
             value = (value << 8) + (bytes[byteIndex + 1] & 0x000000FF);
             value = (value << 8) + (bytes[byteIndex] & 0x000000FF);
        return value;
    }

    /**
     * used to set a dword value. The value is treated as an unsigned 32 bit value: 4,294,967,295
     * @param byteIndex byte offset inside the data buffer
     * @throws AddressException
     */
    @Override
    public void setDWORD(int byteIndex, long value) throws AddressException, ValueOutOfRangeException {
        if (value > 0xFFFFFFFFL || value < 0){
            throw new ValueOutOfRangeException("value: " + value);
        }
        if (byteIndex < 0 || byteIndex + 3 >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid");
        }
        bytes[byteIndex]     = (byte)value;
        bytes[byteIndex + 1] = (byte)(value >>  8);
        bytes[byteIndex + 2] = (byte)(value >> 16);
        bytes[byteIndex + 3] = (byte)(value >> 24);
    }

    /**
     * used to read a dint value. The value is treated as an signed 32 bit value: −2,147,483,648 .. 2,147,483,647
     * @param byteIndex byte offset inside the data buffer
     * @return the value
     * @throws AddressException
     */
    @Override
    public int getDINT(int byteIndex) throws AddressException {
        if (byteIndex < 0 || byteIndex + 3 >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid");
        }
        int value =                (bytes[byteIndex + 3] & 0x000000FF);
            value = (value << 8) + (bytes[byteIndex + 2] & 0x000000FF);
            value = (value << 8) + (bytes[byteIndex + 1] & 0x000000FF);
            value = (value << 8) + (bytes[byteIndex]     & 0x000000FF);
        return value;
    }

    /**
     * used to set a dint value. The value is treated as an signed 32 bit value: −2,147,483,648 .. 2,147,483,647
     * @param byteIndex byte offset inside the data buffer
     * @throws AddressException
     */
    @Override
    public void setDINT(int byteIndex, int value) throws AddressException {
        if (byteIndex < 0 || byteIndex + 3 >= getBytes().length){
            throw new AddressException("byte index " + byteIndex + " invalid");
        }
        bytes[byteIndex + 3] = (byte)(value >> 24);
        bytes[byteIndex + 2] = (byte)(value >> 16);
        bytes[byteIndex + 1] = (byte)(value >>  8);
        bytes[byteIndex]     = (byte)value;
    }
}
