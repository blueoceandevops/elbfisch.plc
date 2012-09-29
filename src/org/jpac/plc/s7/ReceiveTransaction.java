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

package org.jpac.plc.s7;

import org.jpac.ProcessException;
import org.jpac.plc.Request;
import org.jpac.plc.TooManyRequestsException;
import java.io.IOException;

/**
 *
 * @author Ulbrich
 */
public class ReceiveTransaction extends org.jpac.plc.ReceiveTransaction {
    private ReadMultipleData rxCmd;

    public ReceiveTransaction(Connection conn) {
        super(conn);
        rxCmd = new ReadMultipleData(conn);
    }

    @Override
    public void transact() throws IOException {
        rxCmd.transact();
    }

    @Override
    public void addRequest(Request request) throws TooManyRequestsException {
        // pass request to the transacting s7 command
        rxCmd.addRequest((ReadRequest)request);
    }

    @Override
    public void removeAllRequests() {
        // pass request to the transacting s7 command
        rxCmd.removeAllRequests();
    }

    @Override
    public void transact(int waitCycles) throws IOException, ProcessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
