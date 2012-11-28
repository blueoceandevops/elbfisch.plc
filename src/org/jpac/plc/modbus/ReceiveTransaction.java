/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : ReceiveTransaction.java
 * VERSION   : $Revision: 1.3 $
 * DATE      : $Date: 2012/09/12 12:04:40 $
 * PURPOSE   : represents a receive transaction
 * AUTHOR    : Andreas Ulbrich, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: ReceiveTransaction.java,v $
 * LOG       : Revision 1.3  2012/09/12 12:04:40  ulbrich
 * LOG       : Stand fuer DaubThermoRoll R.1.0.0.0b
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
import java.io.IOException;
import org.jpac.EventTimedoutException;
import org.jpac.JPac;
import org.jpac.ProcessEvent;
import org.jpac.ProcessException;

/**
 * Class for transacting a modbus request on a wago plc
 * @author Ulbrich
 */
public class ReceiveTransaction extends org.jpac.plc.ReceiveTransaction {
    public long timeout;
    private ProcessEvent modbusAnswerAvailable;
    
    class BytesAvailableEvent extends ProcessEvent {
        @Override
        public boolean fire() throws ProcessException {
            boolean rt = false;
            if(getConnection() != null) {
                try {
                    rt = ((org.jpac.plc.modbus.Connection)getConnection()).getInputStream().available() != 0;
                } catch (IOException ex) {
                    Log.error("IOException in BytesAvailableEvent", ex);
                }
            }
            return rt;
        }
        
    }
    
    public ReceiveTransaction(Connection conn) {
        super(conn);
        modbusAnswerAvailable = new BytesAvailableEvent();
        timeout = 3 * JPac.getInstance().getCycleTime();
    }

    @Override
    public void transact() throws IOException {
        while(!requestQueue.isEmpty()) {
            ReadRequest request = (org.jpac.plc.modbus.ReadRequest)requestQueue.poll();
            DataInputStream istream = ((org.jpac.plc.modbus.Connection)getConnection()).getInputStream();
            istream.skipBytes(istream.available());// empty input stream before new request written
            //write request message
            request.write(getConnection());
            try {
                modbusAnswerAvailable.await(timeout);
            }
            catch (ProcessException ex) {
                throw new IOException("receive error", ex);
            }
            //read response message
            request.read(getConnection());
            
        }
        Log.debug("Modbus-ReceiveTransaction transacted");
    }

    @Override
    public void transact(int waitCycles) throws IOException, ProcessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
