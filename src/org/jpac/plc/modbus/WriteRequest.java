/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : WriteRequest.java
 * VERSION   : $Revision: 1.5 $
 * DATE      : $Date: 2012/09/12 12:04:40 $
 * PURPOSE   : represents a MODBUS WriteRequest
 * AUTHOR    : Andreas Ulbrich, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: WriteRequest.java,v $
 * LOG       : Revision 1.5  2012/09/12 12:04:40  ulbrich
 * LOG       : Stand fuer DaubThermoRoll R.1.0.0.0b
 * LOG       :
 * LOG       : Revision 1.4  2012/07/11 11:20:30  nouza
 * LOG       : Staende zusammengefuehrt
 * LOG       :
 * LOG       : Revision 1.3  2012/03/14 14:38:40  ulbrich
 * LOG       : modbus: Analog In & Output is implemented to be handled via register access
 * LOG       :
 * LOG       : Revision 1.2  2012/03/13 15:14:21  ulbrich
 * LOG       : modbus - ReceiveRequest & TransmitRequest - waitForByte(): timeout reduced to 10 milliseconds, implementation changed from sleep() to loop
 * LOG       :
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

import java.io.DataInputStream;
import org.jpac.IndexOutOfRangeException;
import org.jpac.plc.WrongOrdinaryException;
import org.jpac.plc.ValueOutOfRangeException;
import java.io.IOException;
import org.jpac.plc.AddressException;
import org.jpac.plc.Request;
import org.jpac.plc.modbus.util.Modbus;

/**
 * represents a writeInt request. Can be added to a an instance of {@link WriteMultipleData}.
 */
public class WriteRequest extends org.jpac.plc.WriteRequest {
    private final byte[]    bitMask = {(byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80};

   /**
     * @param dataType actually two data types are supported: DATATYPE.BIT for accessing BOOL type data items and DATATYPE.BYTE for all other data types
     * @param address a fully qualified address of the data item to be retrieved (@link Address}
     * @param dataOffset the offset of the data item inside the local copy of the data (see parameter "data")
     * @throws ValueOutOfRangeException thrown, if the combination of the given parameters is inconsistent
     */
    public WriteRequest(org.jpac.plc.Request.DATATYPE dataType, org.jpac.plc.Address address, int dataOffset, org.jpac.plc.Data data) throws ValueOutOfRangeException, IndexOutOfRangeException{
            super(dataType, address, dataOffset, data);
    }
    /**
     * Method to calculate the reference value given to the write request in order to write the data at the right modbus address in the plc
     * @return the reference value which should be passed to the modbus protocol
     * @throws ValueOutOfRangeException
     */
    public int getReference() throws ValueOutOfRangeException {
        int retVal = 0;
        if(this.getDataType() == Request.DATATYPE.BIT) {
            retVal = this.getByteAddress() * 8 + this.getBitAddress();
        }
        else if(this.getDataType() == Request.DATATYPE.BYTE) {
            retVal = this.getByteAddress() * 8;
        }
        else {
            retVal = this.getByteAddress() % 2 == 0 ? this.getByteAddress() / 2 : ((this.getByteAddress() / 2) + 1);
        }
        
        switch(((Address)getAddress()).getArea())
        {
            case PHYSICAL_INPUT_DIGITAL:
                //no offset
                break;
            case PHYSICAL_OUTPUT_DIGITAL:
                if(this.dataType == Request.DATATYPE.BIT || this.dataType == Request.DATATYPE.BYTE) {
                    retVal += Address.ModbusInternalMemoryOffset.BIT_OUTPUTS_ACCESS.toInt();
                } else  {
                    retVal += Address.ModbusInternalMemoryOffset.REGISTER_OUTPUTS_ACCESS.toInt();
                }
                break;
            case PFC_INPUT:
                if(this.dataType == Request.DATATYPE.BIT || this.dataType == Request.DATATYPE.BYTE) {
                    retVal += Address.ModbusInternalMemoryOffset.BIT_PFC_INPUT_ACCESS.toInt();
                } else  {
                    retVal += Address.ModbusInternalMemoryOffset.REGISTER_PFC_INPUT_ACCESS.toInt();
                }
                break;
            case PFC_OUTPUT:
                if(this.dataType == Request.DATATYPE.BIT || this.dataType == Request.DATATYPE.BYTE) {
                    retVal += Address.ModbusInternalMemoryOffset.BIT_PFC_OUTPUT_ACCESS.toInt();
                } else  {
                    retVal += Address.ModbusInternalMemoryOffset.REGISTER_PFC_OUTPUT_ACCESS.toInt();
                }
                break;
            case NOVRAM:
                retVal += Address.ModbusInternalMemoryOffset.NOVRAM.toInt();
                break;
            default:
                 throw new ValueOutOfRangeException("invalid address object found: " + this.getAddress().toString());
        }
        return retVal;
    }

    /**
     * Method for receiving the length of the request in bytes
     * @return the length of the write request in bytes
     */
    @Override
    public int getDataLength() {
       switch(this.getDataType()) {
            case BIT:
            case BYTE:
            case WORD:
            case DWORD:
                return this.getAddress().getSize();
            default:
                return 0;
        }
    }

    /**
     * Method for returning the length of the request in bits
     * @return the length of the write request in bits
     */
    public int getDataLengthInBits() {
       switch(this.getDataType()) {
            case BIT:
                return this.getAddress().getSize();
            case BYTE:
            case WORD:
            case DWORD:
                return this.getAddress().getSize() * 8;
            default:
                return 0;
        }
    }

    /**
     * Method for receiving the length of the request in bytes
     * @return the length of the write request in bytes
     */
    public int getDataLengthInBytes() {
       switch(this.getDataType()) {
            case BIT:
                return 1;
            case BYTE:
            case WORD:
            case DWORD:
                return this.getAddress().getSize();
            default:
                return 0;
        }
    }
 
    /**
     * @return the length of the parameter portion of the writeInt request
     */
    @Override
    public int getSendParameterLength() {
        // nothing to do for modbus
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *
     * @return the length of the parameter portion received from the plc
     */
    @Override
    public int getReceiveParameterLength() {
        // nothing to do for modbus
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @return the length of the data portion received from the plc
     */
    @Override
    public int getReceiveDataLength() {
       // nothing to do for modbus
       throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @return the length of the data portion, send to the plc
     */
    @Override
    public int getSendDataLength() {
        // nothing to do for modbus
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Method returns zthe function code according to the area of the address / request
     * @return <code>15</code> or <
     */
    private int getFunctionCode() {
        switch(((org.jpac.plc.modbus.Address)this.getAddress()).getArea()) {
            case PHYSICAL_INPUT_DIGITAL:
            case PHYSICAL_OUTPUT_DIGITAL:
            case PFC_INPUT:
            case PFC_OUTPUT:
                return Modbus.MODBUS_FUNCTIONCODE_WRITECOILS;
            case PHYSICAL_INPUT_ANALOG:
            case PHYSICAL_OUTPUT_ANALOG:
            case NOVRAM:
                return Modbus.MODBUS_FUNCTIONCODE_WRITEMULTIPLEREGISTERS;
            default:
                return 0;
        }
    }

    private short calculateNumberOfUnitsToRead() {
        short numberOfUnits;
        switch(((org.jpac.plc.modbus.Address)getAddress()).getArea()) {
            case NOVRAM:
                // length of request to load is base for count of registers to load
                numberOfUnits = (short) (getDataLength() / 2);
                if((getByteAddress() % 2 != 0) && (getDataLength() % 2 != 0)) {
                    // if bytes to load start at an odd byte address and the request length is further odd we have to load two additional registers and loose the
                    // not needed bytes while mapping
                    numberOfUnits += 2;
                }
                else if((getByteAddress() % 2 != 0) || (getDataLength() % 2 != 0)) {
                    // odd start address or odd length -> load only one additional register
                    numberOfUnits++;
                }
                break;
            default:
                // PHYSICAL_INPUT, PHYSICAL_OUTPUT, PFC_INPUT, PFC_OUTPUT
                if(getDataType() == Request.DATATYPE.BIT) {
                    numberOfUnits = (short) getDataLength() ;
                } else {
                    numberOfUnits = (short) (getDataLength() * 8);
                }
                break;
        }
        return numberOfUnits;
    }

    /**
    * used to wait, until the given amount of bytes are available on the stream<br>
    * if a timeout occurs, an IOException is thrown<br>
    * (see instance variables MAXWAITTIME, ONETICK for further information<br>
    * @param number of bytes to wait for
    * @exception IOException
    */
    private void waitForBytes(DataInputStream stream, int n)throws IOException
    {
        long actual_nanotime   = System.nanoTime();
        long max_wait_nanotime = actual_nanotime + 1000000000L; // TODO : + 10000000L;
        while((stream.available() < n) && System.nanoTime() < max_wait_nanotime);
        //System.out.println("wait time: " + (System.nanoTime() - actual_nanotime));
        if (System.nanoTime() > max_wait_nanotime){
            throw new IOException("incomplete data packet received from Plc controller");
        }
    }

    @Override
    public void write(org.jpac.plc.Connection conn) throws IOException {
        // write request to output stream
        Connection ownConn = (org.jpac.plc.modbus.Connection)conn;//cast connection to modbus connection
        ownConn.getOutputStream().writeShort(Modbus.getNextTransactionID());
        ownConn.getOutputStream().writeShort(0); // protocol identifier is always 0 for MODBUS/TCP
        ownConn.getOutputStream().writeShort(getDataLength() + 7);
        ownConn.getOutputStream().writeByte(0); // unit identifier is not used and will therefore always be 0
        ownConn.getOutputStream().writeByte(getFunctionCode());
         // empty input stream before new request written
        ownConn.getInputStream().skipBytes(ownConn.getInputStream().available());
        writeData(conn);
        ownConn.getOutputStream().flush();
    }

    @Override
    public void writeData(org.jpac.plc.Connection conn) throws IOException {
        try {
            Connection ownConn = (org.jpac.plc.modbus.Connection)conn;//cast connection to modbus connection
            ownConn.getOutputStream().writeShort(this.getReference());
            switch(((Address)getAddress()).getArea()) {
                case NOVRAM:
                    switch(this.getDataType()) {
                        case BIT:
                        case BYTE:
                            throw new IOException("Write request to NOVRAM area must not have BIT or BYTE type");
                        case WORD:
                        case DWORD:
                        ownConn.getOutputStream().writeShort(this.getDataLengthInBytes() / 2);
                        ownConn.getOutputStream().writeByte(this.getDataLengthInBytes());
                        ownConn.getOutputStream().write(getData().getBytes(), getByteAddress(), getDataLengthInBytes());
                        break;
                    }
                case PHYSICAL_INPUT_DIGITAL:
                case PHYSICAL_OUTPUT_DIGITAL:
                case PFC_INPUT:
                case PFC_OUTPUT:
                    ownConn.getOutputStream().writeShort(this.getDataLengthInBits());
                    ownConn.getOutputStream().writeByte(this.getDataLengthInBytes());
                    switch(this.getDataType()) {
                        case BIT:
                            byte[] bitByte = new byte[1];
                            try { bitByte[0] = getData().getBIT(0, 0) ? (byte)1 : (byte)0; }
                            catch(AddressException ex) { throw new IOException(ex); }
                            ownConn.getOutputStream().write(bitByte, 0, 1);
                            break;
                        case BYTE:
                        case WORD:
                        case DWORD:
                            ownConn.getOutputStream().write(getData().getBytes(), getByteAddress(), getDataLengthInBytes());
                    }
                    break;
            }
        }
        catch(ValueOutOfRangeException ex) {
            Log.error("ValueOutOfRange: ", ex);
            throw new IOException(ex);
        }
    }

    @Override
    public void read(org.jpac.plc.Connection conn) throws IOException, WrongOrdinaryException {
        Connection ownConn = (org.jpac.plc.modbus.Connection)conn;//cast connection to modbus connection
        // wait for response sequence header to load
        //read to byte length of message
//        try { waitForBytes(ownConn.getInputStream(), 9); }
//        catch(IOException ex) {
////            Log.error("Invalid modbus protocol found while reading ReadRequest response header: ", ex);
//            Log.error("read failed");
//            throw ex;
//        }
        //read to byte length of message
        int transID = ownConn.getInputStream().readUnsignedShort();
        int protocolID = ownConn.getInputStream().readUnsignedShort();
        int field_length = ownConn.getInputStream().readUnsignedShort();
        int unitID = ownConn.getInputStream().readUnsignedByte();
        int functionID = ownConn.getInputStream().readUnsignedByte();
        int byte_count = ownConn.getInputStream().readUnsignedByte();
        if(!(functionID == Modbus.MODBUS_FUNCTIONCODE_WRITECOILS || functionID == Modbus.MODBUS_FUNCTIONCODE_WRITEMULTIPLEREGISTERS)) {
            // Modbus exception received
           throw new IOException("Modbus response for functionID " + functionID + " returned with error code " + byte_count);
        }
    }
}
