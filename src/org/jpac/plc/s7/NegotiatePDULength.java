/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : NegotiatePDULength.java
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

/**
 * command used to query the maximum length of a PDU (protocol data unit) supported by the particular plc
 *
 */
public class NegotiatePDULength extends Command{
    class PDU extends Command.PDU{
        class Parameters extends Command.PDU.Parameters{
            public final byte NEGOTIATEPDULENGTH = (byte)0xf0;
            public final byte NEGPDULENSENDPARAMLEN      = 8; //length of the parameter block
            public final byte NEGPDULENRECEIVEDPARAMLEN  = 8; //length of the parameter block

            private int maxPDULength;

            @Override
            public void write(Connection conn) throws IOException {
                if (isDebug()) Log.debug("  writing parameters ...");
                if (isDebug()) Log.debug("     request: " + NEGOTIATEPDULENGTH);
                conn.getOutputStream().write(NEGOTIATEPDULENGTH);
                conn.getOutputStream().write(0x00);
                conn.getOutputStream().write(0x00);
                conn.getOutputStream().write(0x01);
                conn.getOutputStream().write(0x00);
                conn.getOutputStream().write(0x01);
                conn.getOutputStream().write(0x03);
                conn.getOutputStream().write(0xc0);
                if (isDebug()) Log.debug("     data ... ");
                if (isDebug()) Log.debug("  parameters written");
            }

            @Override
            public void read(Connection conn, int length) throws IOException {
                this.length = length;
                if (length != getReceiveLength()){
                    throw new IOException("protocol error: invalid length of parameter block");
                }
                if (isDebug()) Log.debug("  reading parameters ...");
                conn.getInputStream().skip(6);
                if (isDebug()) Log.debug("     some data ...");
                setMaxPDULength(conn.getInputStream().readWORD());
                if (isDebug()) Log.debug("     max PDU length " + getMaxPDULength());
                if (isDebug()) Log.debug("  parameters read");
            }

            public int getSendLength(){
                return NEGPDULENSENDPARAMLEN;
            }

            public int getReceiveLength(){
                return NEGPDULENRECEIVEDPARAMLEN;
            }

            public int getMaxPDULength() {
                return maxPDULength;
            }
            public void setMaxPDULength(int maxPDULength) {
                this.maxPDULength = maxPDULength;
            }
        }
        class Data extends Command.PDU.Data{
            @Override
            public int getSendLength() {
                return 0; //the send PDU does not contain any data
            }

            @Override
            public int getReceiveLength() {
                return 0;
            }

            @Override
            public void write(Connection conn) throws IOException {
                //nothing to transmit
            }

            @Override
            public void read(Connection conn) throws IOException {
                //nothing to receive
            }
        }

        private PDU() {
            super();
            header.setType(1);
            parameters = new Parameters();
            data       = new Data();
        }

    }
    /**
     * @param conn an open connection to the plc
     */
    public NegotiatePDULength(Connection conn){
        super(conn);
        setPDU(new PDU());
    }

    /**
     *
     * @return the maximum length of the PDU supported by this plc
     */
    public int getMaxPDULength() {
        return ((PDU.Parameters)getPDU().parameters).getMaxPDULength();
    }

}
