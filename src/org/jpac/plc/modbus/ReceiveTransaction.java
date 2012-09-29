/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : ReceiveTransaction.java
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

package org.jpac.plc.modbus;

import java.io.IOException;
import org.jpac.NthCycle;
import org.jpac.ProcessException;
import org.jpac.plc.WrongOrdinaryException;

/**
 * Class for transacting a modbus request on a wago plc
 * @author Ulbrich
 */
public class ReceiveTransaction extends org.jpac.plc.ReceiveTransaction {
    NthCycle nthCycle;
    int      waitCycles;    
    public ReceiveTransaction(Connection conn) {
        super(conn);
        waitCycles = 0;
    }

    @Override
    public void transact() throws IOException {
        while(!requestQueue.isEmpty()) {
            ReadRequest request = (org.jpac.plc.modbus.ReadRequest)requestQueue.poll();
            //write request message
            request.write(getConnection());
            //read response message
            request.read(getConnection());
            
        }
        if (Log.isDebugEnabled()) Log.debug("Modbus-ReceiveTransaction transacted");
    }

    @Override
    public void transact(int waitCycles) throws IOException, ProcessException {
        if (nthCycle == null){
            nthCycle = new NthCycle(waitCycles);
        }
        else{
            nthCycle.setN(waitCycles);
        }
        while(!requestQueue.isEmpty()) {
             ReadRequest request = (org.jpac.plc.modbus.ReadRequest)requestQueue.poll();
             //write request message
             request.write(getConnection());
             nthCycle.await();
             if (((org.jpac.plc.modbus.Connection)getConnection()).getInputStream().available() == 0){
                 //if the plc did not respond in time, throw an IO exception
                 throw new IOException("transmit transaction timed out");
             }
             //read response message
             request.read(getConnection());
        }
        if(Log.isDebugEnabled()) Log.debug("Modbus-TransmitTransaction transacted");
    }
}
