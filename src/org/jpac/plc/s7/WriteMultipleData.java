/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : WriteMultipleData.java
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
import org.jpac.plc.TooManyRequestsException;

/**
 * used to transmit a set of write requests to the plc.<br>
 * WriteMultipleData is able to send as many write requests as can be contained by one PDU.<br>
 * The transmission is accomplished by calling its method transact().
 */
public class WriteMultipleData extends Command{
    class PDU extends Command.PDU{
        class Parameters extends Command.PDU.Parameters{
            private final byte HEADERLENGTH         = 2;
            private final byte WRITEREQUEST         = (byte)0x05;

            @Override
            public void write(Connection conn) throws IOException {
                if (isDebug()) Log.debug("  writing parameters ...");
                if (isDebug()) Log.debug("     request: " + WRITEREQUEST);
                conn.getOutputStream().write(WRITEREQUEST);
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
                if (requestCode != WRITEREQUEST){
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
            private final int  RECEIVEDATALENGTH    = 1;

            @Override
            public void read(Connection conn)throws IOException{
                if (isDebug()) Log.debug("  reading data ...");
                int dataSize = conn.getInputStream().available();
                //clear input stream for successive requests
                conn.getInputStream().clear();
                if (dataSize != getReceiveLength()){
                    throw new IOException("received invalid number of data items : " + dataSize);
                }
                if (isDebug()) Log.debug("  data read (ignored) ...");
            }

            @Override
            public void write(Connection conn) throws IOException {
                if (isDebug()) Log.debug("  writing data ...");
                requestSet.writeData(conn);
                if (isDebug()) Log.debug("  data written ...");
            }

            @Override
            public int getSendLength() {
                return requestSet.getSendDataLength();
            }

            @Override
            public int getReceiveLength() {
                return requestSet.size() * RECEIVEDATALENGTH;
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

    /**
     * @param conn a valid connection to a plc
     */
    public WriteMultipleData(Connection conn){
        super(conn);
        requestSet = new RequestSet();
        setPDU(new PDU());
    }

    public void addRequest(WriteRequest request) throws TooManyRequestsException{
        if ((pdu.getSendLength() + request.getSendParameterLength() + request.getSendDataLength()) > conn.getMaxPDULength()){
            throw new TooManyRequestsException("send PDU exceeds size negotiated by the PLC");
        }
        if ((pdu.getReceiveLength() + request.getReceiveParameterLength()+ request.getReceiveDataLength()) > conn.getMaxPDULength()){
            throw new TooManyRequestsException("expected receive PDU exceeds size negotiated by the PLC");
        }
        requestSet.add(request);
    }

    /**
     * used to remove the given request from the set of requests
     * @param request a request
     */
    public void removeRequest(WriteRequest request){
        requestSet.remove(request);
    }

    /**
     * used to remove all requests from the request set
     */
    public void removeAllRequests(){
        requestSet.clear();
    }

}
