/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : ReadRequest.java
 * VERSION   : $Revision: 1.6 $
 * DATE      : $Date: 2012/09/12 12:04:40 $
 * PURPOSE   : represents a read request
 * AUTHOR    : Andreas Ulbrich, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: ReadRequest.java,v $
 * LOG       : Revision 1.6  2012/09/12 12:04:40  ulbrich
 * LOG       : Stand fuer DaubThermoRoll R.1.0.0.0b
 * LOG       :
 * LOG       : Revision 1.5  2012/07/11 11:20:30  nouza
 * LOG       : Staende zusammengefuehrt
 * LOG       :
 * LOG       : Revision 1.4  2012/03/19 10:03:38  ulbrich
 * LOG       : mapResponseData() in Abhaengigkeit vom Fun ctionCode mit ByteSwapping implementiert
 * LOG       :
 * LOG       : Revision 1.3  2012/03/14 14:38:41  ulbrich
 * LOG       : modbus: Analog In & Output is implemented to be handled via register access
 * LOG       :
 * LOG       : Revision 1.2  2012/03/13 15:14:22  ulbrich
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
import org.jpac.plc.modbus.Address.AREA;
import org.jpac.plc.modbus.util.Modbus;
import org.jpac.plc.s7.ReadMultipleData;

/**
 * represents a read request. Can be added to a an instance of {@link ReadMultipleData} and will contain the
 * data supplied by the plc on return.
 */
public class ReadRequest extends org.jpac.plc.ReadRequest {
    private final byte[]       buffer;

    private final int MAXWAITTIME =    5000; //max. period of time to wait for
                                             //from the Plc controller in ticks (see ONETICK)
    private final int ONETICK     =       1; //duration of one tick in milliseconds
    private final byte[]    bitMask = {(byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80};

    private long maxWaitForBytesTime;
 
    /**
     * useful, if the Data item is supplied externally
     * @param dataType actually two data types are supported: DATATYPE.BIT for accessing BOOL type data items and DATATYPE.BYTE for all other data types
     * @param db the datablock inside the plc, which contains the data to be read
     * @param byteAddress the byte address of the data inside the data block (db)
     * @param bitAddress the bit address of data inside the byte addressed by "byteAddress". Applicable, if the data to be read is of the plc type BOOL
     * @param dataOffset the offset of the data item inside the local copy of the data (see parameter "data")
     * @param dataLength the length of the data item, to be retrieved
     * @param data a local copy of the data, retrieved from the plc
     * @throws ValueOutOfRangeException thrown, if the combination of the given parameters is inconsistent
     * @throws IndexOutOfRangeException thrown, if one of the address of offset values are out of range.
     */
    public ReadRequest(DATATYPE dataType, AREA area, int byteAddress, int bitAddress, int dataOffset, int dataLength, Data data) throws ValueOutOfRangeException, IndexOutOfRangeException{
        if (dataType == DATATYPE.BIT && dataLength != 1){
            throw new ValueOutOfRangeException("exactly one bit per bitwise request can be accessed");
        }
        this.dataType    = dataType;
        this.data        = data;
        this.address     = new Address(area, byteAddress, bitAddress, dataLength);
        this.dataOffset  = dataOffset;
        buffer = new byte[Modbus.MAX_MESSAGE_LENGTH];
        maxWaitForBytesTime = 0;
    }

    /**
     * If an instance of ReadRequest is instantiated by use of this constructor, an appropriate "Data" object is instantiated internally
     * @param dataType actually two data types are supported: DATATYPE.BIT for accessing BOOL type data items and DATATYPE.BYTE for all other data types
     * @param address a fully qualified address of the data item to be retrieved (@link Address}
     * @param dataOffset the offset of the data item inside the local copy of the data (see parameter "data")
     * @throws ValueOutOfRangeException thrown, if the combination of the given parameters is inconsistent
     */
    public ReadRequest(DATATYPE dataType, org.jpac.plc.Address address, int dataOffset) throws ValueOutOfRangeException, IndexOutOfRangeException{
        super(dataType, address, dataOffset, null);
        buffer = new byte[Modbus.MAX_MESSAGE_LENGTH];
        maxWaitForBytesTime = 0;
    }

    /**
     * useful, if the Data item is supplied externally
     * @param dataType actually two data types are supported: DATATYPE.BIT for accessing BOOL type data items and DATATYPE.BYTE for all other data types
     * @param address a fully qualified address of the data item to be retrieved (@link Address}
     * @param dataOffset
     * @param dataOffset the offset of the data item inside the local copy of the data (see parameter "data")
     * @throws ValueOutOfRangeException thrown, if the combination of the given parameters is inconsistent
     */
    public ReadRequest(DATATYPE dataType, org.jpac.plc.Address address, int dataOffset, Data data) throws ValueOutOfRangeException, IndexOutOfRangeException{
        super(dataType, address, dataOffset, data);
        buffer = new byte[Modbus.MAX_MESSAGE_LENGTH];
        maxWaitForBytesTime = 0;
    }

    public int getReference() throws ValueOutOfRangeException {
        int retVal = this.getByteAddress() * 8 + (this.getBitAddress() == Address.NA ? 0 : this.getBitAddress());

        switch(((Address)getAddress()).getArea())
        {
            case PHYSICAL_INPUT_ANALOG:
                // we have to read registers
                retVal = this.getByteAddress() / 2;
            case PHYSICAL_INPUT_DIGITAL:
                //no offset
                break;
            case PHYSICAL_OUTPUT_ANALOG:
                // we have to read registers
                retVal = this.getByteAddress() / 2;
            case PHYSICAL_OUTPUT_DIGITAL:
                retVal += Address.ModbusInternalMemoryOffset.BIT_OUTPUTS_ACCESS.toInt();
                break;
            case PFC_INPUT:
                retVal += Address.ModbusInternalMemoryOffset.BIT_PFC_INPUT_ACCESS.toInt();
                break;
            case PFC_OUTPUT:
                retVal += Address.ModbusInternalMemoryOffset.BIT_PFC_OUTPUT_ACCESS.toInt();
                break;
            case NOVRAM:
                // object address has to be recalculated due to register access on NOVRAM-Area
                retVal = this.getByteAddress() / 2;
                retVal += Address.ModbusInternalMemoryOffset.NOVRAM.toInt();
                break;
            default:
                 throw new ValueOutOfRangeException("invalid address object found: " + this.getAddress().toString());
        }
        return retVal;
    }

    /**
     * Getter methode for receiving the length of request in bytes
     * @return the dataLength of the request in bytes or <code>0</code> if unsupported request type
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
     * used to write the read request to the plc as part of modbus protocol
     * @param conn a valid connection to the plc
     * @throws IOException
     */
    @Override
    public void write(org.jpac.plc.Connection conn) throws IOException {
        // write request to output stream
        Connection ownConn = (org.jpac.plc.modbus.Connection)conn;//cast connection to modbus connection
        ownConn.getOutputStream().writeShort(Modbus.getNextTransactionID());
        ownConn.getOutputStream().writeShort(0); // protocol identifier is always 0 for MODBUS/TCP
        ownConn.getOutputStream().writeShort(6); // write reamainig bytes in this MODBUS protocol header

        ownConn.getOutputStream().writeByte(0); // unit identifier is not used and will therefore always be 0
        ownConn.getOutputStream().writeByte(getFunctionCode()); // write function code of read request
        writeData(conn); // write MODBUS request data
//        ownConn.getInputStream().skipBytes(ownConn.getInputStream().available());// empty input stream before new request written
        ownConn.getOutputStream().flush(); // flushing the data to the peer now
    }

    /**
     * used to read the data replied by the plc as part of a MODBUS protocol request
     * @param conn a valid connection to the plc
     * @throws IOException
     * @throws WrongOrdinaryException thrown, if the data returned by the plc is inconsistent
     */
    @Override
    public void read(org.jpac.plc.Connection conn) throws IOException {
        Connection ownConn = (org.jpac.plc.modbus.Connection)conn;//cast connection to modbus connection
        // read modbus response from input stream
        synchronized(buffer) {
            // wait for response sequence header to load
            //read to byte length of message
            // TODO: jetzt auf Transaction-Ebene pruefen
//            try { waitForBytes(ownConn.getInputStream(), 9); }
//            catch(IOException ex) {
//                Log.error("Invalid modbus protocol found while reading ReadRequest response header: ", ex);
//                throw ex;
//            }
            //read to byte length of message           
            int transID = ownConn.getInputStream().readUnsignedShort(); // read transaction id of the request
            int protocolID = ownConn.getInputStream().readUnsignedShort(); // read the protocol id of the request
            int field_length = ownConn.getInputStream().readUnsignedShort(); // read remaining length of the request in byte
            int unitID = ownConn.getInputStream().readUnsignedByte(); // read unit id of the request (everytime 0)
            int functionID = ownConn.getInputStream().readUnsignedByte(); // reread the function code of the request
            int byte_count = ownConn.getInputStream().readUnsignedByte(); // read the count of data returned by the request in byte

            // check for valid request function code, it must be 2 or 3.
            if(!(functionID == Modbus.MODBUS_FUNCTIONCODE_READINPUTDISCRETES || functionID == Modbus.MODBUS_FUNCTIONCODE_READMULTIPLEREGISTERS)) {
                // Modbus exception received
                throw new IOException("Modbus response for functionID " + functionID + " returned with error code " + byte_count);
            }
            
            try { waitForBytes(ownConn.getInputStream(), byte_count);} // try to read count of data from the peer
            catch(IOException ex) {
                Log.error("Invalid modbus protocol found while reading ReadRequest response data: ", ex);
                throw ex;
            }
            ownConn.getInputStream().read(buffer, 0, byte_count); // now really read the data and store them in buffer array
            try {
                mapResponseData(buffer, this, functionID); // map the buffer to requests data object
            }
            catch(Exception ex) {
                Log.error("Error while mapping read data into Data object: ", ex);
                throw new IOException(ex);
            }
      }
    }

    @Override
    public void writeData(org.jpac.plc.Connection conn) throws IOException {
        try {
            Connection ownConn = (org.jpac.plc.modbus.Connection)conn;//cast connection to modbus connection
            ownConn.getOutputStream().writeShort(this.getReference());
            ownConn.getOutputStream().writeShort(calculateNumberOfUnitsToRead());
        }
        catch(ValueOutOfRangeException ex) {
            Log.error("ValueOutOfRange: ", ex);
            throw new IOException(ex);
        }
    }

    @Override
    public int getSendParameterLength() {
        // nothing to do for modbus
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getReceiveParameterLength() {
        // nothing to do for modbus
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getSendDataLength() {
        // nothing to do for modbus
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getReceiveDataLength() {
        // nothing to do for modbus
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Method to select the function code depending on the requests area
     * @return the function code corresponding to the requests area
     */
    private int getFunctionCode() {
        switch(((org.jpac.plc.modbus.Address)this.getAddress()).getArea()) {
            case PHYSICAL_INPUT_DIGITAL:
            case PHYSICAL_OUTPUT_DIGITAL:
            case PFC_INPUT:
            case PFC_OUTPUT:
                return Modbus.MODBUS_FUNCTIONCODE_READINPUTDISCRETES;
            case PHYSICAL_INPUT_ANALOG:
            case PHYSICAL_OUTPUT_ANALOG:
            case NOVRAM:
                return Modbus.MODBUS_FUNCTIONCODE_READMULTIPLEREGISTERS;
            default:
                return 0;
        }
    }
    /**
     * Method to calculate the number of request type specific units  to read from plc
     * @return the number of bits (modbus discretes) / registers to read from plc
     */
    private short calculateNumberOfUnitsToRead() {
        short numberOfUnits;
        switch(((org.jpac.plc.modbus.Address)getAddress()).getArea()) {
            case PHYSICAL_INPUT_ANALOG:
            case PHYSICAL_OUTPUT_ANALOG:
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
        long max_wait_nanotime = actual_nanotime + 1000000000L;//10000000L;TODO nur für Test !!!!!!
        while((stream.available() < n) && System.nanoTime() < max_wait_nanotime);
        long tmp_waitForByteTime = System.nanoTime() - actual_nanotime;
        if(maxWaitForBytesTime < tmp_waitForByteTime) {
            maxWaitForBytesTime = tmp_waitForByteTime;
            Log.error("MaxWaitForBytesTime: " + maxWaitForBytesTime);
        }
        //System.out.println("wait time: " + (System.nanoTime() - actual_nanotime));
        if (System.nanoTime() > max_wait_nanotime){
            throw new IOException("incomplete data packet received from Plc controller");
        }
    }

    /**
     * Method to map the data received from plc to the request standard data object
     * @param buf - the byte array containing the data received from plc
     * @param request - the read request where the data should be filled /mapped into
     * @throws AddressException - in case of invalid address object passed to request
     */
    private void mapResponseData(byte[] buf, ReadRequest request, int functionId) throws AddressException {
        synchronized (getData()) {
            switch(request.getDataType()){
                case BIT:
                    int bit_address = getBitAddress();
                    int request_size = getDataLength();
                    int actualCount = 0;
                    int byteOffsetNextBytes = 0;
                    if(functionId == Modbus.MODBUS_FUNCTIONCODE_READMULTIPLEREGISTERS) {
                        while(actualCount  < request_size) {
                            getData().setBIT(request.getDataOffset() + byteOffsetNextBytes, bit_address,
                                             byteOffsetNextBytes % 2 == 0 ? (buf[byteOffsetNextBytes + 1] & bitMask[bit_address]) != 0 :
                                                                            (buf[byteOffsetNextBytes] & bitMask[bit_address]) != 0);
                            bit_address++;
                            if(bit_address > 7) {
                                bit_address = 0;
                                byteOffsetNextBytes++;
                            }
                            actualCount++;
                        }
                    }
                    else {                        
                        while(actualCount  < request_size) {
                            getData().setBIT(request.getDataOffset() + byteOffsetNextBytes, bit_address,
                                             (buf[byteOffsetNextBytes] & bitMask[bit_address]) != 0);
                            bit_address++;
                            if(bit_address > 7) {
                                bit_address = 0;
                                byteOffsetNextBytes++;
                            }
                            actualCount++;
                        }
                    }
                    break;
                case BYTE:
                case WORD:
                case DWORD:
                    if(functionId == Modbus.MODBUS_FUNCTIONCODE_READMULTIPLEREGISTERS) {
                        swapRegisterBytes(buf, getData().getBytes(), request);
                    }
                    else {
                        System.arraycopy(buf, 0, getData().getBytes(), request.getDataOffset(), request.getDataLength());
                    }
                    break;
            }
        }
    }

    private void swapRegisterBytes(byte[] buf, byte[] data_bytes, ReadRequest request) {
        for(int i = 0; i < request.getDataLength(); i+=2) {
            // swap byte order
            data_bytes[i] = buf[i + 1];
            data_bytes[i + 1] = buf[i];
        }
    }
}