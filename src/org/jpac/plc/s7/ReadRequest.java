/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : ReadRequest.java
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

import org.jpac.IndexOutOfRangeException;
import org.jpac.plc.WrongOrdinaryException;
import java.io.IOException;
import org.jpac.plc.AddressOutOfRangeException;
import org.jpac.plc.ValueOutOfRangeException;

/**
 * represents a read request. Can be added to a an instance of {@link ReadMultipleData} and will contain the
 * data supplied by the plc on return.
 */
public class ReadRequest extends org.jpac.plc.ReadRequest{

    public enum RESULT {
        DATAOK                   (0),
        //result codes supplied by the PLC
        INVALIDPERIPHERAL        (1),     // specified peripheral not available
        MULTIPLEBITSNOTSUPPORTED (6), 	  // CPU supplies only 1 bit per request
        ITEMNOTAVAILIBLE200      (3),     // data item not available (200 series specific ???)
        ITEMNOTAVAILABLE         (10),    // data item not availalbe
        ADDRESSOUTOFRANGE        (5),	  // address out of range
        WRITEDATASIZEMISMATCH    (7),     // attempt to write a chunk of data which does not match the size of the target data item
        //result codes generated internally
        INVALIDLENGTHSEMANTICS   (100),   // received unknown data format specification
        INVALIDDATAHEADER        (101),   // received unknown data header
        INVALIDDATALENGTH        (102),   // received invalid data length
        NORESULT                 (-1);


        private final int n;

        RESULT(int n){
            this.n = n;
        }
        /**
         * returns the ordinary of the enum value
         * @return the ordinary of the enum value
         */
        public int toInt(){
            return this.n;
        }

        /**
         * returns a value according to a given ordinary
         * @param  an ordinary
         * @throws WrongOrdinaryException, if the given ordinary does not exist
         * @return the enum value according to the given ordinary
         */
        public static RESULT getValue(int n){
            RESULT match = null;
            for (RESULT p : RESULT.values()){
                if (p.toInt() == n) {
                    match = p;
                    break;
                }
            }
            if (match == null){
                match = NORESULT;
            }
            return match;
        }
    }

    public enum AREA {
        NOAREA (0x00),
        DB     (0x84);

        private final int n;

        AREA(int n){
            this.n = n;
        }
        /**
         * returns the ordinary of the enum value
         * @return the ordinary of the enum value
         */
        public int toInt(){
            return this.n;
        }

        /**
         * returns a value according to a given ordinary
         * @param  an ordinary
         * @throws WrongOrdinaryException, if the given ordinary does not exist
         * @return the enum value according to the given ordinary
         */
        public static AREA getValue(int n)throws WrongOrdinaryException{
            AREA match = null;
            for (AREA p : AREA.values()){
                if (p.toInt() == n) {
                    match = p;
                    break;
                }
            }
            if (match == null){
                throw new WrongOrdinaryException("invalid ordinary: " + n);
            }
            return match;
        }
    }

    public enum DATAFORMAT {
        NOFORMAT        (0),
        LENGTHINBITS    (4),
        ONEBYTEPERBIT   (3),
        LENGTHINBYTES   (9);

        private final int n;

        DATAFORMAT(int n){
            this.n = n;
        }
        /**
         * returns the ordinary of the enum value
         * @return the ordinary of the enum value
         */
        public int toInt(){
            return this.n;
        }

        /**
         * returns a value according to a given ordinary
         * @param  an ordinary
         * @throws WrongOrdinaryException, if the given ordinary does not exist
         * @return the enum value according to the given ordinary
         */
        public static DATAFORMAT getValue(int n){
            DATAFORMAT match = null;
            for (DATAFORMAT p : DATAFORMAT.values()){
                if (p.toInt() == n) {
                    match = p;
                    break;
                }
            }
            if (match == null){
                match = NOFORMAT;
            }
            return match;
        }
    }

    private final int  READREQUESTLENGTH              = 12;
    private final int  READREQUESTRECEIVEHEADERLENGTH = 4;

    private boolean    externalData;

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
    public ReadRequest(DATATYPE dataType, int db, int byteAddress, int bitAddress, int dataOffset, int dataLength, org.jpac.plc.Data data) throws ValueOutOfRangeException, IndexOutOfRangeException{
        super(dataType, new Address(db, byteAddress, bitAddress, dataLength), dataOffset, data);
        if (dataType == DATATYPE.BIT && address.getSize() != 1){
            throw new ValueOutOfRangeException("exactly one bit per bitwise request can be accessed");
        }
        externalData = true;
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
        if (dataType == DATATYPE.BIT && address.getSize() != 1){
            throw new ValueOutOfRangeException("exactly one bit per bitwise request can be accessed");
        }
        externalData = false;
    }

    /**
     * useful, if the Data item is supplied externally
     * @param dataType actually two data types are supported: DATATYPE.BIT for accessing BOOL type data items and DATATYPE.BYTE for all other data types
     * @param address a fully qualified address of the data item to be retrieved (@link Address}
     * @param dataOffset
     * @param dataOffset the offset of the data item inside the local copy of the data (see parameter "data")
     * @throws ValueOutOfRangeException thrown, if the combination of the given parameters is inconsistent
     */
    public ReadRequest(DATATYPE dataType, org.jpac.plc.Address address, int dataOffset, org.jpac.plc.Data data) throws ValueOutOfRangeException, IndexOutOfRangeException{
        super(dataType, address, dataOffset, data);
        if (dataType == DATATYPE.BIT && address.getSize() != 1){
            throw new ValueOutOfRangeException("exactly one bit per bitwise request can be accessed");
        }
        externalData = true;
    }

    /**
     * used to write the read request to the plc as part of a ISO data packet
     * @param conn a valid connection to the plc
     * @throws IOException
     */
    @Override
    public void write(org.jpac.plc.Connection conn) throws IOException {
        Connection ownConn = (Connection) conn;
        if (conn.isDebug()) Log.debug("       write request parameters ...");
        //write out the request
        ownConn.getOutputStream().write(0x12);
        ownConn.getOutputStream().write(0x0a);
        ownConn.getOutputStream().write(0x10);
        if (ownConn.isDebug()) Log.debug("         some data ...");
        ownConn.getOutputStream().write(getDataType().toInt());
        if (ownConn.isDebug()) Log.debug("         data type " + getDataType());
        ownConn.getOutputStream().writeWORD(getDataLength());
        if (ownConn.isDebug()) Log.debug("         size of data block " + getDataLength());
        ownConn.getOutputStream().writeWORD(getDb());
        if (ownConn.isDebug()) Log.debug("         DB " + getDb());
        ownConn.getOutputStream().write(AREA.DB.toInt());
        int bitOffset = 0;
        if (getByteAddress() != Address.NA)
            bitOffset = getByteAddress() * 8;
        if (getBitAddress() != Address.NA)
            bitOffset += getBitAddress();
        ownConn.getOutputStream().write((bitOffset & 0x00FF0000) >> 16); //most significant byte of the 3 Byte bit address
        ownConn.getOutputStream().writeWORD(bitOffset & 0x0000FFFF);     //2 trailing bytes of the 3 Byte bit address

        if (ownConn.isDebug()){
            String LogString = "";
            switch(getDataType()){
                case BIT:  LogString = "         bit address " + bitOffset;
                           break; 
                case BYTE: LogString = "         byte address " + bitOffset / 8;
                           break;
                //not supported yet
                //case WORD: LogString = "         byte address " + bitOffset / 8;
                //           break;
            }
            Log.debug(LogString);
            Log.debug("       request parameters written ...");
        }
    }

    /**
     * used to read the data replied by the plc as part of an ISO data packet
     * @param conn a valid connection to the plc
     * @throws IOException
     * @throws WrongOrdinaryException thrown, if the data returned by the plc is inconsistent
     */
    @Override
    public void read(org.jpac.plc.Connection conn) throws IOException{
        DATAFORMAT lengthSemantics;
        Connection ownConn = (Connection)conn;//cast connection to S7 connection
        if (ownConn.isDebug()) Log.debug("       reading request record ...");
        setResult(RESULT.NORESULT);
        int tag = ownConn.getInputStream().read();
        if (ownConn.isDebug()) Log.debug("         tag: " + tag);
        if (tag == READRESULTHEADERTAG){
            //response seems to be a valid block of data
            lengthSemantics = DATAFORMAT.getValue(ownConn.getInputStream().read());
            if (ownConn.isDebug()) Log.debug("         length format: " + lengthSemantics);
            int bufferLength = ownConn.getInputStream().readWORD();//read out length of user data
            switch(lengthSemantics){
                case LENGTHINBITS:
                     bufferLength /= 8;
                     break;
                case ONEBYTEPERBIT:
                case LENGTHINBYTES:
                     break;
                default:
                     throw new IOException("invalid length semantics: " + lengthSemantics);
            }
            if (ownConn.isDebug()) Log.debug("         buffer length [byte] : " + bufferLength);
            if (bufferLength != getDataLength()){
                setResult(RESULT.INVALIDDATALENGTH);
                throw new IOException("unexpected size of data record. Received : " + bufferLength + " expected: " + getDataLength());
            }
            //allocate a buffer to store it, if not supplied externally
            if (!externalData){
                if (getData() == null){
                    setData(conn.generateDataObject(getDataOffset() + bufferLength));
                } else if (!externalData && getDataOffset() + bufferLength > getData().getBytes().length){
                    //if the received data does not fit into the buffer, allocate a new one
                    setData(conn.generateDataObject(getDataOffset() + bufferLength));
                }
            }
            //read data into buffer
            ((org.jpac.plc.s7.Data)getData()).read(ownConn, getDataOffset(), bufferLength);
            if (ownConn.isDebug()) Log.debug("         data ... ");
            if (((getDataLength() % 2) == 1) && (ownConn.getInputStream().available() >= 1)){
                //if data length is odd, read one fill byte
                ownConn.getInputStream().read();
                if (ownConn.isDebug()) Log.debug("         one fill byte ");
            }
            //result record read in successfully
            setResult(RESULT.DATAOK);
        }
        else{
            ownConn.getInputStream().clear();//clear inputstream
            if (RESULT.getValue(tag) == RESULT.ADDRESSOUTOFRANGE){
               throw new AddressOutOfRangeException("PLC unable to access address " + getAddress().toString());
            }
            throw new IOException("error received from PLC : " + RESULT.getValue(tag) + " (" + tag + ')');
        }
        if (ownConn.isDebug()) Log.debug("       request record read.");
    }

    private int getDb(){
        return ((Address)address).getDb();
    }

    /**
     * @return the length of the parameter block of the read request
     */
    @Override
    public int getSendParameterLength() {
        return READREQUESTLENGTH;
    }

    /**
     * @return the length of the data item, sent to plc
     */
    @Override
    public int getSendDataLength() {
        return 0;
    }

    /**
     * @return the length of the parameter block received from the plc
     */
    @Override
    public int getReceiveParameterLength() {
        return 0;
    }

    /**
     * @return the length of the data item received from the plc
     */
    @Override
    public int getReceiveDataLength() {
        int len = READREQUESTRECEIVEHEADERLENGTH + getDataLength();
        if ((getDataLength() % 2) == 1){
            //if data length is odd, a fill byte will be transmitted by the PLC
            len++;
        }
        return len;
    }

    /**
     * used to write a data item to the plc as part of the read request (Actually nothing is written)
     * @param conn
     * @throws IOException
     */
    @Override
    public void writeData(org.jpac.plc.Connection conn) throws IOException {
        //nothing to write
    }
}