/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Connection.java
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

import java.net.*;
import java.io.*;
import org.apache.log4j.Logger;
import org.jpac.IndexOutOfRangeException;
import org.jpac.plc.Address;
import org.jpac.plc.Data;
import org.jpac.plc.ReadRequest;
import org.jpac.plc.Request;
import org.jpac.plc.ValueOutOfRangeException;
import org.jpac.plc.WriteRequest;

/**
 * represents a TCP/IP connection to a S7 plc. The connection uses the ISO protocol.
 *
 */
public class Connection extends org.jpac.plc.Connection{
    static Logger Log = Logger.getLogger("jpac.plc.s7");

    private static final int ISOHEADERLENGTH       = 4;    //length of the ISO header
    private static final int PROLOGLENGTH          = 3;    //length of the prolog
    private static final int VRSN                  = 0x03; //value of the first byte of the ISO header (version of the protocol)
    private static final int PLCPORT               = 102;  //S7 PLC's communicate ISO over TCP on this port
    private static final int SOMEOVERHEAD          = 32;   //TODO get exact value
    private static final int SIMPDULENGTH          = 232;  //assumed pdu length for test purposes only
    private InputStream      in;
    private OutputStream     out;
    private Socket           socket;
    private int              rack;
    private int              slot;
    private int              maxPDULength;

    private static int       transactionNumber; //used to supply an unique transaction number to every request. Might be shared amongst several connections
    
    private ReceiveTransaction   receiveTransaction;
    private TransmitTransaction  transmitTransaction;

    /**
     * an instance of Connection is created and the connection to given plc is initiated immediately
     * @param host ip address of the plc (e.g. 192.168.0.1)
     * @param rack rack id (in most cases '0')
     * @param slot slot id (in most cases '2')
     * @param debug switch on/off generation of debug information
     * @throws IOException
     */
    public Connection(String host, int rack, int slot, boolean debug) throws IOException{
        this(host, rack, slot, debug, true);
    }
    /**
     * an instance of Connection is created and the connection to given plc is initiated immediately
     * @param host ip address of the plc (e.g. 192.168.0.1)
     * @param rack rack id (in most cases '0')
     * @param slot slot id (in most cases '2')
     * @param debug switch on/off generation of debug information
     * @param autoConnect true: the Connection is automatically established on construction
     *                    false: no connect attempt is done (for standalone test purposes only)
     * @throws IOException
     */
    public Connection(String host, int rack, int slot, boolean debug, boolean autoConnect) throws IOException{
        super(host, PLCPORT, debug, autoConnect);
        this.rack         = rack;
        this.slot         = slot;
        this.maxPDULength = 0;
        if (autoConnect){
            try{
                initialize();
            }
            catch(IOException exc){
                connected  = false;
                throw exc;
            }
        }
        else{
            this.maxPDULength = SIMPDULENGTH;  
        }
    }
            
    /**
     *  used to initialize the connection.
     * @throws IOException
     */
    protected synchronized void initialize() throws IOException{
        //try to establish a connection to a emScon controller as a client
        // Create an unbound socket
        socket = new Socket();
        // This method will block no more than timeoutMs.
        // If the timeout occurs, SocketTimeoutException is thrown.
        int timeoutMs = 10000;   // 10 seconds
        if (isDebug()) Log.debug("try to connect to PLC ..." + host + " on port " + port);
        socket.connect(new InetSocketAddress(host,port), timeoutMs);
        initPlcStreams();
        openISOConnection();
        NegotiatePDULength negCmd = new NegotiatePDULength(this);
        negCmd.transact();
        setMaxPDULength(negCmd.getMaxPDULength());
        if (isDebug()) Log.info("connected to PLC. Max. PDU length : " + getMaxPDULength());
    }


    /**
     * initialization of the plc input and output streams
     */
    private void initPlcStreams() throws IOException {
        //initialize input and output streams
        if (isDebug()) Log.debug("create socket input stream");
        in  = new InputStream(socket.getInputStream());
        if (isDebug()) Log.debug("create socket output stream");
        out = new OutputStream(socket.getOutputStream());
        in.setDebug(isDebug());
        out.setDebug(isDebug());
        connected = true;
    }

    /**
     * use to close an existing connection.
     */
    public synchronized void close() throws IOException{
        this.connected = false;
        in.close();
        this.in.setOperational(false);
        out.close();
        this.out.setOperational(false);
        try {
             getSocket().close();
        }
        catch(IOException ex) {
            if(!ex.getMessage().equals("connection does not exist")) {
                throw new IOException(ex);
            }
        }
        if (isDebug()) Log.info("connection to PLC closed");
    }
    
    public InputStream getInputStream() throws IOException{
        if (!connected){
            throw new IOException("connection does not exist");
        }
        return in;
    }

    public OutputStream getOutputStream() throws IOException {
        if (!connected){
            throw new IOException("connection does not exist");
        }        
        return out;
    }

    public Socket getSocket() throws IOException{
        if (!connected){
            throw new IOException("connection does not exist");
        }        
        return socket;
    }
    
    /**
     * Setter for property serverSocket.
     * @param serverSocket New value of property serverSocket.
     */
    public void setSocket(Socket socket) {

        this.socket = socket;
    }
    
    public void writeISOHeader(int length) throws IOException {
            if (isDebug()) Log.debug(" writing ISO header ... ");
            out.write(VRSN);
            if (isDebug()) Log.debug("    VRSN : " + VRSN);
            out.write(0x00);
            if (isDebug()) Log.debug("    dummy : " + 0x00);
            int len = ISOHEADERLENGTH + length;
            out.writeWORD(len);

            if (isDebug()) Log.debug("    data length : " + len);
            if (isDebug()) Log.debug(" ISO header written");
    }

    public int readISOHeader() throws IOException {
            if (isDebug()) Log.debug(" reading ISO header ... ");
            int vrsn = in.read();                          //always VRSN ??
            if (isDebug()) Log.debug("    VRSN : " + vrsn);
            int dummy = in.read();                         //always 0x00 ??
            if (isDebug()) Log.debug("    dummy : " + dummy);
            int len =  in.readWORD();                      //read out length of the packet

            if (isDebug()) Log.debug("    data length : " + len);
            if (vrsn != VRSN){
                throw new IOException("attempt to connect aborted by PLC");
            }
            if (isDebug()) Log.debug(" ISO header read");
            return len;
    }

    public void writeProlog() throws IOException{
            if (isDebug()) Log.debug(" writing prolog ... ");
            out.write(0x02); //send constant bytes
            out.write(0xf0);
            out.write(0x80); //send constant bytes
            if (isDebug()) Log.debug(" prolog written");
    }

    public void readProlog() throws IOException{
            if (isDebug()) Log.debug(" reading prolog ... ");
            int p;
            p = in.read(); //read constant bytes
            p = in.read(); //read constant bytes
            p = in.read(); //read constant bytes
            if (isDebug()) Log.debug(" prolog read");
    }

    public int getPrologLength(){
        return PROLOGLENGTH;
    }

    private void openISOConnection() throws IOException {
        final int RACKSLOTLEN = 2;
        if (isDebug()) Log.debug("open ISO over TCP connection ...");
        byte[] connectPrefix  ={(byte)0x11,(byte)0xE0,(byte)0x00,(byte)0x00,(byte)0x00,
                                (byte)0x01,(byte)0x00,(byte)0xC1,(byte)0x02,(byte)0x01,
                                (byte)0x00,(byte)0xC2,(byte)0x02};
        byte[] connectPostfix ={(byte)0xC0,(byte)0x01,(byte)0x09};
        if (isDebug()) Log.debug("  write open request ...");
        writeISOHeader(connectPrefix.length + RACKSLOTLEN + connectPostfix.length);
        out.write(connectPrefix);
        out.write(rack+1);
        out.write(slot);
        out.write(connectPostfix);
        if (isDebug()) Log.debug("  open request written");

        readISOHeader();
        in.clear();
        if (isDebug()) Log.debug("  open request accepted by PLC.");
    }
    public synchronized int getUniqueTransactionNumber(){
        return transactionNumber = ++transactionNumber < 0x7FFF ? transactionNumber : 0;
    }

    /**
     * @return the maximum PDU length negotiated by the plc
     */
    public int getMaxPDULength() {
        return maxPDULength;
    }

    /**
     * used to set the maximum PDU length
     * @param a positive number
     */
    private void setMaxPDULength(int maxPDULength) {
        this.maxPDULength = maxPDULength;
    }


    public ReadRequest generateReadRequest(Request.DATATYPE datatype, Address address, int dataOffset, Data data) throws ValueOutOfRangeException, IndexOutOfRangeException{
        return new org.jpac.plc.s7.ReadRequest(datatype, address, dataOffset, data);
    }


    public WriteRequest generateWriteRequest(Request.DATATYPE datatype, Address address, int dataOffset, Data data) throws ValueOutOfRangeException, IndexOutOfRangeException{
        return new org.jpac.plc.s7.WriteRequest(datatype, address, dataOffset, data);
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
    public ReceiveTransaction generateReceiveTransaction() {
        if(receiveTransaction == null) {
            receiveTransaction =  new ReceiveTransaction(this);
        }
        return receiveTransaction;
    }

    @Override
    public TransmitTransaction generateTransmitTransaction() {
        if(transmitTransaction == null) {
            transmitTransaction =  new TransmitTransaction(this);
        }
        return transmitTransaction;
    }

    @Override
    public int getMaxTransferLength() {
        return getMaxPDULength() - SOMEOVERHEAD;
    }

    @Override
    public Data generateDataObject(int size) {
        return new org.jpac.plc.s7.Data(new byte[size]);
    }
}
