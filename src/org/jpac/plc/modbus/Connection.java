/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Connection.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : Connections to modbus PLC
 * AUTHOR    : Andreas Ulbrich, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: Connection.java,v $
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.jpac.IndexOutOfRangeException;
import org.jpac.plc.Data;
import org.jpac.plc.ReadRequest;
import org.jpac.plc.Request.DATATYPE;
import org.jpac.plc.ValueOutOfRangeException;
import org.jpac.plc.WriteRequest;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import org.apache.log4j.Logger;
import org.jpac.plc.modbus.util.Modbus;

/**
 * represents a TCP/IP connection to a MODBUS plc.
 *
 */
public class Connection extends org.jpac.plc.Connection {
    static Logger Log = Logger.getLogger("jpac.plc");

    private Socket                                socket;
    private DataInputStream                       in;
    private DataOutputStream                      out;
    private ReceiveTransaction                    receiveTransaction;
    private TransmitTransaction                   transmitTransaction;
        
    /**
     * an instance of Connection is created and the connection to given plc is initiated immediately
     * @param host ip address of the plc (e.g. 192.168.0.1)
     * @param rack rack id (in most cases '0')
     * @param slot slot id (in most cases '2')
     * @param debug switch on/off generation of debug information
     * @throws IOException
     */
    public Connection(String host, int port, boolean debug) throws IOException {
        super(host, port, debug);

        try{
            initialize();
        }
        catch(IOException exc){
            connected  = false;
            throw exc;
        }
    }
            
    /**
     *  used to initialize the connection.
     * Method instantiates the socket to modbus system and installs the in and ouptput stream
     *
     * @throws IOException in case of io error in thge connection or streams
     */
    @Override
    public synchronized void initialize() throws IOException {
        InetAddress addr = InetAddress.getByName(host);
        try {
            // create a tcp/ip socket for basic connectivity
            socket = new Socket(addr, Modbus.DEFAULT_PORT);
            try {
              socket.setSoTimeout(Modbus.DEFAULT_TIMEOUT);
            } catch (IOException ex) {
              /// nothing to do here
            }

            // prepare streams here
             in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            
            connected = true;
            if (isDebug()) {
                Log.debug("Connected to " + addr.toString() + ":" + socket.getPort());
            }
        } catch (Exception ex) {
            throw new IOException("Error: " , ex);
        }
    }

    /**
     * use to close an existing connection.
     */
    @Override
    public synchronized void close() throws IOException{
        connected = false;
        socket.close();
        if (isDebug()) Log.info("connection to MODBUS-PLC closed");
    }
     
    @Override
    public ReadRequest generateReadRequest(DATATYPE dataType, org.jpac.plc.Address addr, int dataOffset, org.jpac.plc.Data data) throws ValueOutOfRangeException, IndexOutOfRangeException{
        return new org.jpac.plc.modbus.ReadRequest(dataType, addr, dataOffset, (org.jpac.plc.modbus.Data) data);
    }

    @Override
    public WriteRequest generateWriteRequest(DATATYPE dataType, org.jpac.plc.Address addr, int dataOffset, org.jpac.plc.Data data) throws ValueOutOfRangeException, IndexOutOfRangeException{
        return new org.jpac.plc.modbus.WriteRequest(dataType, addr, dataOffset, data);
    }

    @Override
    public void write(int maxLength) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void write(byte[] stringBytes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int read() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void read(byte[] stringBytes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public org.jpac.plc.ReceiveTransaction generateReceiveTransaction() {
        if(receiveTransaction == null) {
            receiveTransaction =  new org.jpac.plc.modbus.ReceiveTransaction(this);
        }
        return receiveTransaction;
    }

    @Override
    public org.jpac.plc.TransmitTransaction generateTransmitTransaction() {
        if(transmitTransaction == null) {
            transmitTransaction =  new TransmitTransaction(this);
        }
        return transmitTransaction;
    }

    @Override
    public int getMaxTransferLength() {
        return 512; // return a chunk size of 512 bytes
    }

    @Override
    public Data generateDataObject(int size) {
        return new org.jpac.plc.modbus.Data(new byte[size]);
    }

    /**
     * Getter-methode for calling the input stream from {@link org.jpac.plc.modbus.Connection}.
     * @return the {@link java.io.InputStream} hold in the connection
     * @throws IOException - n case of i/o error in input stream
     */
    public DataInputStream getInputStream() throws IOException {
        if (!connected){
            throw new IOException("connection does not exist");
        }
        return in;
    }

    /**
     * Getter-methode for calling the output stream from {@link org.jpac.plc.modbus.Connection}.
     * @return the {@link java.io.OutputStream} hold in the connection
     * @throws IOException - n case of i/o error in output stream
     */
    public DataOutputStream getOutputStream() throws IOException {
        if (!connected){
            throw new IOException("connection does not exist");
        }
        return out;
    }
}
