/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : ReadMultipleData.java
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

import org.jpac.plc.WrongOrdinaryException;
import java.io.IOException;
import org.jpac.plc.TooManyRequestsException;

/**
 * used to transmit a set of read requests to the plc.<br>
 * ReadMultipleData is able to send as many read request as can be contained by one PDU.<br>
 * The transmission is accomplished by calling its method transact(). After return,
 * the processed read requests contain the data supplied by the plc.
 */
public class ReadMultipleData extends Command{
    class PDU extends Command.PDU{
        class Parameters extends Command.PDU.Parameters{
            private final byte HEADERLENGTH    =  2;
            private final byte READREQUEST     = (byte)0x04;

            @Override
            public void write(Connection conn) throws IOException {
                if (isDebug()) Log.debug("  writing parameters ...");
                if (isDebug()) Log.debug("     request: " + READREQUEST);
                conn.getOutputStream().write(READREQUEST);
                conn.getOutputStream().write(requestSet.size());//number of pending requests.
                if (isDebug()) Log.debug("     number of request records: " + requestSet.size());
                //write out all pending requests
                requestSet.write(conn);
            }

            @Override
            public void read(Connection conn, int length) throws IOException {
                if (isDebug()) Log.debug("  reading parameters ...");
                int requestCode = conn.getInputStream().read();
                if (isDebug()) Log.debug("     request: " + requestCode);
                if (requestCode != READREQUEST){
                    throw new IOException("invalid request code returned from PLC: " + requestCode);
                }
                int numberOfResultRecords = 0x000000FF & conn.getInputStream().read(); // positive number 0..255
                if (isDebug()) Log.debug("     number of request records: " + numberOfResultRecords);
                if (numberOfResultRecords != requestSet.size()){
                    throw new IOException("unexpected number of result records received: " + numberOfResultRecords + " expected: " + requestSet.size());
                }
                if (isDebug()) Log.debug("  parameters read ...");
            }

            @Override
            public int getSendLength() {
                return HEADERLENGTH + requestSet.getSendParameterLength();
            }

            @Override
            public int getReceiveLength() {
                return HEADERLENGTH;
            }
        }
        class Data extends Command.PDU.Data{
            @Override
            public void read(Connection conn)throws IOException{
                if (isDebug()) Log.debug("  reading data ...");
                //read received result records
                try{
                    requestSet.read(conn);
                   }
                catch(WrongOrdinaryException exc){
                    if (isDebug()) Log.error("Error: ", exc);
                    throw new IOException(exc.getMessage());
                }
                if (isDebug()) Log.debug("  data read ...");
            }

            @Override
            public int getSendLength() {
                return requestSet.getSendDataLength();
            }

            @Override
            public int getReceiveLength() {
                int dataLength = requestSet.getReceiveDataLength();
                if (dataLength > 0){
                    //if some data is to be transmitted,
                    //then add the size of the data header
                    dataLength += HEADERLENGTH;
                }
                return dataLength;
            }

            @Override
            public void write(Connection conn) throws IOException {
                requestSet.writeData(conn);
            }
        }
        private PDU() {
            super();
            header.setType(1);
            this.parameters = new Parameters();
            this.data = new Data();
        }

    }

    RequestSet requestSet;

    public ReadMultipleData(Connection conn){
        super(conn);
        requestSet = new RequestSet();
        setPDU(new PDU());
    }
    /**
     * used to add a read request to the set of requests, to be performed
     * @param request
     * @throws TooManyRequestsException
     */
    public void addRequest(ReadRequest request) throws TooManyRequestsException{
        if ((pdu.getSendLength() + request.getSendParameterLength() + request.getReceiveParameterLength()) > conn.getMaxPDULength()){
           throw new TooManyRequestsException("send PDU exceeds size negotiated by the PLC");
        }
        if ((pdu.getReceiveLength() + request.getSendDataLength() + request.getReceiveDataLength()) > conn.getMaxPDULength()){
            throw new TooManyRequestsException("expected receive PDU exceeds size negotiated by the PLC");
        }
        requestSet.add(request);
    }

    /**
     * used to remove a given read request from the set of requests
     * @param request
     */
    public void removeRequest(ReadRequest request){
        requestSet.remove(request);
    }

    /**
     * used to clear the set of read requests
     */
    public void removeAllRequests(){
        requestSet.clear();
    }

}
