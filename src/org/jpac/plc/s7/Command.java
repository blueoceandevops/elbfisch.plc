/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Command.java
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
import org.apache.log4j.Logger;

/**
 * implements some common features and concepts used by several transactional commands
 * 
 */
public abstract class Command {
    static Logger Log = Logger.getLogger("jpac.plc.s7");

    class PDU{
        Header     header;
        Parameters parameters;
        Data       data;

        private boolean    debug;

        class Header{
            private final byte HEADERTAG       = 0x32;
            private final byte RESULTOK        = 0;

            private int   tag;                   //always the tag HEADERTAG
            private int   type;                  //
            private int   transactionNumber;
            private int   parameterLength;
            private int   dataLength;
            private int   receivedDataLength;
            private int   resultCode;

            private boolean debug;

            public Header(){
                type              = 0x00;
                transactionNumber = 1234;
                parameterLength   = 0;
                dataLength        = 0;
                resultCode        = 0;
                debug             = false;
            }
            public void write(Connection conn) throws IOException{
                if (isDebug()) Log.debug("   writing header ...");
                conn.getOutputStream().write(HEADERTAG);
                if (isDebug()) Log.debug("     tag: " + HEADERTAG);
                conn.getOutputStream().write(getType());
                if (isDebug()) Log.debug("     type: " + getType());
                conn.getOutputStream().writeWORD(0x00);//spare
                conn.getOutputStream().writeWORD(getTransactionNumber());
                if (isDebug()) Log.debug("     transaction number " + getTransactionNumber());
                conn.getOutputStream().writeWORD(getParameterLength());
                if (isDebug()) Log.debug("     parameter length " + getParameterLength());
                conn.getOutputStream().writeWORD(getDataLength());
                if (isDebug()) Log.debug("     data length " + getDataLength());
                if (getType() == 2 || getType() == 3){
                    conn.getOutputStream().writeWORD(getResultCode());
                    if (isDebug()) Log.debug("     result code: " + getResultCode());
                }
                //if some data is to be appended to the header, don't forget to adjust getLength()
                if (isDebug()) Log.debug("  header written ");
            }
            public void read(Connection conn) throws IOException{
                //if some data is to be appended to the header, don't forget to adjust getLength()
                if (isDebug()) Log.debug("   reading header ...");
                int rxTag = conn.getInputStream().read();
                if (isDebug()) Log.debug("     tag: " + rxTag);
                if (rxTag != HEADERTAG){
                    throw new IOException("received invalid PDU header: " + rxTag);
                }
                int rxType = conn.getInputStream().read();
                if (isDebug()) Log.debug("     type: " + rxType);
                conn.getInputStream().readWORD();//spare
                int rxTransactionNumber  = conn.getInputStream().readWORD();
                if (isDebug()) Log.debug("     transaction number " + rxTransactionNumber);
                int rxParameterLength = conn.getInputStream().readWORD();
                if (isDebug()) Log.debug("     parameter length " + rxParameterLength);
                int rxDataLength = conn.getInputStream().readWORD();
                if (isDebug()) Log.debug("     data length " + rxDataLength);
                if (rxType == 2 || rxType == 3){
                    resultCode = conn.getInputStream().readWORD();
                    if (isDebug()) Log.debug("     result code: " + getResultCode());
                    if (resultCode != RESULTOK){
                        throw new IOException("received error result from PLC: " + resultCode);
                    }
                }
                if (isDebug()) Log.debug("  header read");
            }
            public int getSendLength(){
                return (type == 2 || type == 3) ? 12 : 10;
            }
            public int getReceiveLength(){
                //return expected receive length for the apriori calculation of the pdu length
                return (type == 2 || type == 3) ? 12 : 10;
            }
            public int getTag() {
                return tag;
            }
            public void setTag(int tag) {
                this.tag = tag;
            }
            public int getType() {
                return type;
            }
            public void setType(int type) {
                this.type = type;
            }
            public int getTransactionNumber() {
                return transactionNumber;
            }
            public void setTransactionNumber(int transactionNumber) {
                this.transactionNumber = transactionNumber;
            }
            public int getParameterLength() {
                return parameterLength;
            }
            public void setParameterLength(int parameterLength) {
                this.parameterLength = parameterLength;
            }
            public int getDataLength() {
                return dataLength;
            }
            public void setDataLength(int dataLength) {
                this.dataLength = dataLength;
            }
            public int getReceivedDataLength() {
                return receivedDataLength;
            }
            public void setReceivedDataLength(int dataLength) {
                this.receivedDataLength = dataLength;
            }
            public int getResultCode() {
                return resultCode;
            }
            public void setResultCode(int resultCode) {
                this.resultCode = resultCode;
            }
            public boolean isDebug() {
                return conn.isDebug();
            }
        }
        abstract class Parameters{
            final byte ACCESSBITS    = (byte)0x01;
            final byte ACCESSBYTES   = (byte)0x02;
            final byte ACCESSWORDS   = (byte)0x04;
            final byte AREADB        = (byte)0x84;

            protected int     length;

            public Parameters(){
            }

            public abstract void write(Connection conn)throws IOException;
            public abstract void read(Connection conn, int length)throws IOException;
            public abstract int getSendLength();
            public abstract int getReceiveLength();

            public boolean isDebug() {
                return conn.isDebug();
            }
        }

        abstract class Data{
            final int HEADERTAG              =  -1;
            final int LENGTHINBITS           =   4;
            final int ONEBYTEPERBIT          =   3;
            final int LENGTHINBYTES          =   9;
            final int DATAOK                 =   0;
            final int INVALIDLENGTHSEMANTICS =   1;
            final int INVALIDDATAHEADER      =   2;
            final int HEADERLENGTH           =   4;

            private byte[] buffer;       //buffer used to store the user data
            private int    bufferLength; //length of the user data block
            private int    length;       //length of the data block including the header

            private int lengthSemantics; //length information inside the header can have different semantics, as there are
                                         //length in bytes, length in bits, one bit is represented in one byte ...
            private int result;

            public Data(){
                bufferLength = 0;
                length       = 0;
            }
            abstract public void write(Connection conn)throws IOException;
            abstract public void read(Connection conn)throws IOException;
            abstract public int  getSendLength();
            abstract public int  getReceiveLength();

            public void setBufferLength(int length){
                bufferLength = length;
            }
            public int getBufferLength(){
                return bufferLength;
            }
            public int setBufferLength(){
                return bufferLength;
            }
            public byte[] getBuffer(){
                return buffer;
            }
            public void setBuffer(byte[] buffer){
                this.buffer = buffer;
            }
            public int getLengthFormat(){
                return lengthSemantics;
            }
        }
        
        PDU(){
            header     = new Header();
        }
        public void write(Connection conn) throws IOException{
            //initialize header
            header.setParameterLength(parameters.getSendLength());
            header.setDataLength(data.getSendLength());
            header.setTransactionNumber(conn.getUniqueTransactionNumber());//get an "unique" transaction number
            //write data blocks
            if (isDebug()) Log.debug(" writing PDU (" + getClass().getName() + ") ...");
            header.write(conn);
            parameters.write(conn);
            data.write(conn);
            if (isDebug()) Log.debug(" PDU written");
        }
        public void read(Connection conn) throws IOException{
            if (isDebug()) Log.debug(" reading PDU ...");
            header.read(conn);
            parameters.read(conn, header.getParameterLength());
            data.read(conn);
            if (isDebug()) Log.debug(" PDU read");
        }

        public int getSendLength(){
            return header.getSendLength() + parameters.getSendLength() + data.getSendLength();
        }
        public int getReceiveLength(){
            return header.getReceiveLength() + parameters.getReceiveLength() + data.getReceiveLength();
        }

        public boolean isDebug() {
            return conn.isDebug();
        }

    }

    Connection               conn;    //connection to the PLC
    PDU                      pdu;

    public Command(Connection conn){
        this.conn = conn;
    }
    /**
     * used to perform a transaction. <br>
     * A PDU containing request information is sent to the plc.<br>
     * transact() returns, after the the plc's has sent it's acknowledgement
     * @throws IOException
     */
    public void transact() throws IOException{
        if (isDebug()) Log.debug("beginning " + getClass().getName() + ".transact() ...");
        conn.writeISOHeader(pdu.getSendLength() + conn.getPrologLength());
        conn.writeProlog();
        pdu.write(conn);
        //store actual transactionnumber for comparison with the received one
        int actualTransactionNumber = pdu.header.getTransactionNumber();
        //force all bytes to be written to the PLC
        conn.getOutputStream().flush();
        //wait for / receive response from the PLC
        int len = conn.readISOHeader();
        conn.readProlog();
        pdu.read(conn);
        //check consistency of the transaction number
        if (pdu.header.getTransactionNumber() != actualTransactionNumber){
            throw new IOException("inconsistent transaction number: transmitted : " + actualTransactionNumber + " received : " + pdu.header.getTransactionNumber());
        }
        if (isDebug()) Log.debug("ending " + getClass().getName() + ".transact()");
    }

    PDU getPDU(){
        if (pdu == null){
            pdu = new PDU();
        }
        return pdu;
    }
    void setPDU(PDU pdu){
        this.pdu = pdu;
    }
    
    public boolean isDebug() {
        return conn.isDebug();
    }
}

