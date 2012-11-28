/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : TransmitTransaction.java
 * VERSION   : $Revision: 1.2 $
 * DATE      : $Date: 2012/09/12 12:04:40 $
 * PURPOSE   : represents a transmit transaction
 * AUTHOR    : Andreas Ulbrich, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: TransmitTransaction.java,v $
 * LOG       : Revision 1.2  2012/09/12 12:04:40  ulbrich
 * LOG       : Stand fuer DaubThermoRoll R.1.0.0.0b
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

import java.io.IOException;
import org.jpac.EventTimedoutException;
import org.jpac.JPac;
import org.jpac.NextCycle;
import org.jpac.ProcessEvent;
import org.jpac.ProcessException;
import org.jpac.ShutdownRequestException;
import org.jpac.plc.WrongOrdinaryException;

/**
 * Class for transferring a write request to a wago plc
 * @author Ulbrich
 */
public class TransmitTransaction extends org.jpac.plc.TransmitTransaction {
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
    
    public TransmitTransaction(Connection conn) {
        super(conn);
        // TODO test !!!!!modbusAnswerAvailable = new BytesAvailableEvent();
        modbusAnswerAvailable = new NextCycle();
        timeout = 3 * JPac.getInstance().getCycleTime();
    }

    @Override
    public void transact() throws IOException {
        while(!requestQueue.isEmpty()) {
            WriteRequest request = (org.jpac.plc.modbus.WriteRequest)requestQueue.poll();
            //write request message
            if (((org.jpac.plc.modbus.Connection)getConnection()).getInputStream().available() != 0){
                //TODO Log.error("write input queue enthält noch Daten !!!!! ");
                ((org.jpac.plc.modbus.Connection)getConnection()).getInputStream().skip(2000L);
            }
            request.write(getConnection());
            try {
                modbusAnswerAvailable.await(timeout);
            } 
            catch (ProcessException ex) {
                throw new IOException("receive error", ex);
            }
            try {
               //read response message
               request.read(getConnection());
            } catch (WrongOrdinaryException ex) {
               Log.error("Error: ", ex);
            }
        }
        Log.debug("Modbus-TransmitTransaction transacted");
    }

    @Override
    public void transact(int waitCycles) throws IOException, ProcessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
