/**
 * PROJECT   : jPAC S7 communication library
 * MODULE    : Toolbox.java
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

package org.jpac.s7.toolbox;

import org.jpac.IndexOutOfRangeException;
import org.jpac.plc.AddressException;
import org.jpac.plc.BitRxTx;
import org.jpac.plc.ByteRxTx;
import org.jpac.plc.s7.Connection;
import org.jpac.plc.s7.Data;
import org.jpac.plc.DintRxTx;
import org.jpac.plc.DwordRxTx;
import org.jpac.plc.IntRxTx;
import org.jpac.plc.PlcString;
import org.jpac.plc.StringLengthException;
import org.jpac.plc.StringRxTx;
import org.jpac.plc.ValueOutOfRangeException;
import org.jpac.plc.WordRxTx;
import org.jpac.plc.s7.symaddr.StructGenerator;
import org.jpac.plc.s7.symaddr.Variables;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.Logger;
import org.jpac.plc.s7.Address;
//import org.apache.commons.cli.CommandLineParser;
//import org.apache.commons.cli.GnuParser;
//import org.apache.commons.cli.Options;

/**
 *
 * @author berndschuster
 */
public class Toolbox {

    final static int NODB        = -1;
    final static int PDUOVERHEAD = 32;

    private class InvalidCLIOptionException extends Exception{
        public InvalidCLIOptionException(String message){
            super(message);
        }
    }

    enum Cmd{HELP,READ,WRITE,DEBUG,CONNECT,SELECT,LOAD,GENERATE,EXIT,UNDEFINED;
           public static Cmd get(String symbol){
               symbol = symbol.toUpperCase().trim();
               Cmd value = UNDEFINED;
               if (symbol == null)
                  return UNDEFINED;
               if (HELP.toString().equals(symbol))
                  value = HELP;
               if (READ.toString().equals(symbol))
                  value = READ;
               if (WRITE.toString().equals(symbol))
                  value = WRITE;
               if (DEBUG.toString().equals(symbol))
                  value = DEBUG;
               if (CONNECT.toString().equals(symbol))
                  value = CONNECT;
               if (SELECT.toString().equals(symbol))
                  value = SELECT;
               if (LOAD.toString().equals(symbol))
                  value = LOAD;
               if (GENERATE.toString().equals(symbol))
                  value = GENERATE;
               if (EXIT.toString().equals(symbol))
                  value = EXIT;
               return value;
           }
    };

    enum DataType{BOOL,BYTE,CHAR,WORD,DWORD,INT,DINT,STRING,UNDEFINED;
           public static DataType get(String symbol){
               symbol = symbol.toUpperCase().trim();
               DataType value = UNDEFINED;
               if (symbol == null)
                  return UNDEFINED;
               if (BOOL.toString().equals(symbol))
                  value = BOOL;
               if (BYTE.toString().equals(symbol))
                  value = BYTE;
               if (WORD.toString().equals(symbol))
                  value = WORD;
               if (DWORD.toString().equals(symbol))
                  value = DWORD;
               if (CHAR.toString().equals(symbol))
                  value = CHAR;
               if (INT.toString().equals(symbol))
                  value = INT;
               if (DINT.toString().equals(symbol))
                  value = DINT;
               if (STRING.toString().equals(symbol))
                  value = STRING;
               return value;
           }
    };

    enum MiscTermSyms{VALUE,DEBUG,INFO,ERROR,AT,LENGTH,TO,DB,SYMLIST,FROM,ACCESSORS,FOR,PACKAGE,PATH,TITLE,UNDEFINED;
           public static MiscTermSyms get(String symbol){
               symbol = symbol.toUpperCase().trim();
               MiscTermSyms value = UNDEFINED;
               if (symbol == null)
                  return UNDEFINED;
               if (VALUE.toString().equals(symbol))
                  value = VALUE;
               if (DEBUG.toString().equals(symbol))
                  value = DEBUG;
               if (INFO.toString().equals(symbol))
                  value = INFO;
               if (ERROR.toString().equals(symbol))
                  value = ERROR;
               if (AT.toString().equals(symbol))
                  value = AT;
               if (LENGTH.toString().equals(symbol))
                  value = LENGTH;
               if (TO.toString().equals(symbol))
                  value = TO;
               if (DB.toString().equals(symbol))
                  value = DB;
               if (SYMLIST.toString().equals(symbol))
                  value = SYMLIST;
               if (FROM.toString().equals(symbol))
                  value = FROM;
               if (ACCESSORS.toString().equals(symbol))
                  value = ACCESSORS;
               if (FOR.toString().equals(symbol))
                  value = FOR;
               if (PACKAGE.toString().equals(symbol))
                  value = PACKAGE;
               if (PATH.toString().equals(symbol))
                  value = PATH;
               if (TITLE.toString().equals(symbol))
                  value = TITLE;
               return value;
           }
    }

    //Options         options;
    BufferedReader  stdin;
    String          ip;
    int             db;
    Connection      conn;

    Cmd             cmd;
    DataType        dataType;
    int             byteAddress;
    int             bitAddress;
    long            value;
    String          stringValue;
    int             length;
    Data            data;
    String          symListPath;
    String          accessorPackage;
    String          accessorPath;
    int             accessorDb;
    String          accessorTitle;
    Variables       variables;
    StructGenerator structGenerator;
    static Logger   logger;

    
    private Toolbox() throws IOException{
        //options         = prepareCLIOptions();
        stdin           = new BufferedReader (new InputStreamReader (System.in));
        ip              = null;
        db              = NODB;
        conn            = null;
        
        cmd             = Cmd.UNDEFINED;
        dataType        = DataType.UNDEFINED;
        byteAddress      = 0;
        bitAddress       = 0;
        value           = 0;
        stringValue     = null;
        length          = 0;
        data            = null;
        accessorPackage = null;
        accessorPath    = null;
        accessorDb      = NODB;
        accessorTitle   = null;
        variables       = null;
        structGenerator = null;
        //setup logging
        logger = Logger.getRootLogger();
        SimpleLayout layout = new SimpleLayout();
        ConsoleAppender consoleAppender = new ConsoleAppender( layout );
        logger.addAppender( consoleAppender );
        logger.setLevel( Level.ERROR);
    }

    private void doIt(String[] args) throws IOException{
        try{
            // no command line options, yet
            // options = prepareCLIOptions();
            // create the parser
            //CommandLineParser parser = new GnuParser();
                // parse the command line arguments
                // no command line options, yet
                // CommandLine line = parser.parse( options, args );

                do{
                    try {
                        readCommand();
                        switch(cmd){
                            case READ:
                                    doReadCommand();
                                    break;
                            case WRITE:
                                    doWriteCommand();
                                    break;
                            case CONNECT:
                                    doConnectCommand();
                                    break;
                            case SELECT:
                                    doSelectCommand();
                                    break;
                            case LOAD:
                                    doLoadCommand();
                                    break;
                            case GENERATE:
                                    doGenerateCommand();
                                    break;
                            case EXIT:
                                    System.out.println("exiting ... ");
                                    break;
                            case HELP:
                            case UNDEFINED:
                                    System.out.println("\n" +
                                                       "HELP\n" +
                                                       "READ  <datatype> AT <byteoffset>[.<bitoffset>]\n" +
                                                       "WRITE <datatype> AT <byteoffset>[.<bitoffset>] VALUE {<value> | '<string>' LENGTH <length>}\n" +
                                                       "CONNECT TO <ip>\n" +
                                                       "SELECT DB <number>\n" +
                                                       "LOAD SYMLIST FROM <path>\n" +
                                                       "GENERATE ACCESSORS FOR DB <number> PACKAGE <package> PATH <path> [TITLE '<title>']\n" +
                                                       "DEBUG DEBUG|INFO|ERROR\n" +
                                                       "EXIT\n\n" +
                                                       "datatype=BOOL|BYTE|CHAR|INT|DINT|WORD|DWORD|STRING\n");
                                    break;
                        }
                    }
                    catch(Exception exc) {
                        exc.printStackTrace();
                        System.out.println("error occured while executing a command: " + exc.getMessage());
                    }
                }
                while(cmd != Cmd.EXIT);
        }
        //catch(InvalidCLIOptionException exc) {
        //    System.out.println("invalid command line option :" + exc.getMessage());
        //    HelpFormatter formatter = new HelpFormatter();
        //    formatter.printHelp( "S7Access", options );
        //}
        finally{
            if (conn != null){
                conn.close();
            }
        }

    }

//    private static Options prepareCLIOptions(){
//        Options options = new Options();
//
//        Option help  = new Option( "help", "print this message" );
//        @SuppressWarnings("static-access")
//        Option ip   = OptionBuilder.withArgName( "ip address" )
//                                .hasArg()
//                                .withDescription(  "ip address of the PLC (e.g. 192.168.0.1)" )
//                                .create( "ip" );
//        @SuppressWarnings("static-access")
//        Option db   = OptionBuilder.withArgName( "data block (DB)" )
//                                .hasArg()
//                                .withDescription(  "data block (e.g. 10)" )
//                                .create( "db" );
//        options.addOption(db);
//        options.addOption(help);
//        options.addOption(ip);
//
//        return options;
//    }

    private void readCommand() throws IOException{
        System.out.print("cmd>");
        String cmdStr = stdin.readLine();
        Pattern termSymPattern    = Pattern.compile("[a-zA-Z]+");
        Pattern numberPattern     = Pattern.compile("[0-9]+");
        Pattern anyPattern        = Pattern.compile("\\S+");
        Pattern stringPattern     = Pattern.compile("\\'.*\\'");
        Matcher termSym           = termSymPattern.matcher(cmdStr);
        Matcher numberMatcher     = numberPattern.matcher(cmdStr);
        Matcher anyMatcher        = anyPattern.matcher(cmdStr);
        Matcher stringMatcher     = stringPattern.matcher(cmdStr);
        
        termSym.find();
        cmd = Cmd.get(termSym.group());
        switch(cmd){
            case READ:
            case WRITE:
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    dataType = DataType.get(termSym.group());
                    if (dataType == DataType.UNDEFINED){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.AT.toString())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!numberMatcher.find(termSym.end())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    byteAddress = Integer.parseInt(numberMatcher.group());
                    if (dataType == DataType.BOOL){
                        if (cmdStr.length() == numberMatcher.end() || cmdStr.charAt(numberMatcher.end()) != '.' || !numberMatcher.find(numberMatcher.end()+1)){
                            cmd = Cmd.UNDEFINED;
                            break;
                        }
                        bitAddress = Integer.parseInt(numberMatcher.group());
                    }
                    if (cmd == Cmd.WRITE){
                        if (!termSym.find(numberMatcher.end())){
                            cmd = Cmd.UNDEFINED;
                            break;
                        }
                        if (!termSym.group().toUpperCase().equals(MiscTermSyms.VALUE.toString())){
                            cmd = Cmd.UNDEFINED;
                            break;
                        }
                        if (dataType == DataType.STRING){
                            if (!stringMatcher.find(termSym.end())){
                                cmd = Cmd.UNDEFINED;
                                break;
                            }
                            stringValue = stringMatcher.group().replaceAll("'", "");
                            if (!termSym.find(stringMatcher.end())){
                                cmd = Cmd.UNDEFINED;
                                break;
                            }
                            if (!termSym.group().toUpperCase().equals(MiscTermSyms.LENGTH.toString())){
                                cmd = Cmd.UNDEFINED;
                                break;
                            }
                            if (!numberMatcher.find(termSym.end())){
                                cmd = Cmd.UNDEFINED;
                                break;
                            }
                            length = Integer.parseInt(numberMatcher.group());
                        }
                        else{
                            if (!anyMatcher.find(termSym.end())){
                                cmd = Cmd.UNDEFINED;
                                break;
                            }
                            value = Long.parseLong(anyMatcher.group());
                        }
                    }
                    break;
            case CONNECT:
                    ip = null;
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.TO.toString())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!anyMatcher.find(termSym.end())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    ip = anyMatcher.group();
                    break;
            case SELECT:
                    db = NODB;
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.DB.toString())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!numberMatcher.find(termSym.end())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    db = Integer.parseInt(numberMatcher.group());
                    break;
            case LOAD:
                    symListPath = null;
                    variables   = null;
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.SYMLIST.toString()) ){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.FROM.toString()) ){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!anyMatcher.find(termSym.end())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    symListPath = anyMatcher.group();
                    break;
            case GENERATE:
                    accessorDb = NODB;
                    accessorPackage = null;
                    accessorPath = null;
                    accessorTitle = null;
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.ACCESSORS.toString()) ){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.FOR.toString()) ){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.DB.toString())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!numberMatcher.find(termSym.end())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    accessorDb = Integer.parseInt(numberMatcher.group());
                    if (!termSym.find(numberMatcher.end())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.PACKAGE.toString())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!anyMatcher.find(termSym.end())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    accessorPackage = anyMatcher.group();
                    if (!termSym.find(anyMatcher.end())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!termSym.group().toUpperCase().equals(MiscTermSyms.PATH.toString())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    if (!anyMatcher.find(termSym.end())){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    accessorPath = anyMatcher.group();
                    if (termSym.find(anyMatcher.end())){
                        if (!termSym.group().toUpperCase().equals(MiscTermSyms.TITLE.toString())){
                            cmd = Cmd.UNDEFINED;
                            break;
                        }
                        if (!stringMatcher.find(termSym.end())){
                            cmd = Cmd.UNDEFINED;
                            break;
                        }
                        accessorTitle = stringMatcher.group().replaceAll("'", "");
                    }
                    else{
                        accessorTitle = "Accessor classes for DB" + accessorDb;
                    }
                    break;
            case DEBUG:
                    if (!termSym.find()){
                        cmd = Cmd.UNDEFINED;
                        break;
                    }
                    Level level = Level.OFF;
                    switch(MiscTermSyms.get(termSym.group().toUpperCase())){
                        case DEBUG:
                             level = Level.DEBUG;
                             break;
                        case INFO:
                             level = Level.INFO;
                             break;
                        case ERROR:
                             level = Level.ERROR;
                             break;
                        default:
                             cmd = Cmd.UNDEFINED;
                             break;
                    }
                    logger.setLevel(level);
                    if (conn != null){
                        conn.setDebug(level == Level.DEBUG);
                    }
                    System.out.println("debug level set to " + level);
                    break;
            case EXIT:
            case UNDEFINED:
                    break;
        }
    }

    private void doReadCommand() throws IOException, AddressException, StringLengthException, IndexOutOfRangeException{
        if (conn == null){
            throw new IOException("no connection to plc");
        }
        if (db == NODB){
            throw new IOException("no db selected");
        }
        switch(dataType){
            case BOOL:
//                BitRxTx bitrxtx = new BitRxTx(conn, db, byteAddress, bitAddress, 0, data);
                BitRxTx bitrxtx = new BitRxTx(conn, new Address(db, byteAddress, bitAddress, 1), 0, data);
                boolean flag = bitrxtx.read().is(true);
                System.out.println(dataType + " at DB" + db + "." + byteAddress + "." + bitAddress + " = " + flag);
                break;
            case BYTE:
            case CHAR:
                bitAddress = Address.NA;
                ByteRxTx byterxtx = new ByteRxTx(conn,  new Address(db, byteAddress, bitAddress, 1), 0, data);
                value = byterxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + (value & 0xFF));
                break;
            case WORD:
                bitAddress = Address.NA;
                WordRxTx wordrxtx = new WordRxTx(conn,  new Address(db, byteAddress, bitAddress, 2), 0, data);
                value = wordrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + (value & 0xFFFF));
                break;
            case INT:
                bitAddress = Address.NA;
                IntRxTx intrxtx = new IntRxTx(conn,  new Address(db, byteAddress, bitAddress, 2), 0, data);
                value = intrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + value);
                break;
            case DWORD:
                bitAddress = Address.NA;
                DwordRxTx dwordrxtx = new DwordRxTx(conn,  new Address(db, byteAddress, bitAddress, 4), 0, data);
                value = dwordrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + value);
                break;
            case DINT:
                bitAddress = Address.NA;
                DintRxTx dintrxtx = new DintRxTx(conn,  new Address(db, byteAddress, bitAddress, 4), 0, data);
                value = dintrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + value);
                break;
            case STRING:
                bitAddress = Address.NA;
                ByteRxTx maxlenrxtx = new ByteRxTx(conn,  new Address(db, byteAddress, bitAddress, 1), 0, data);
                length = maxlenrxtx.read().get();
                StringRxTx stringrxtx = new StringRxTx(conn,  new Address(db, byteAddress, bitAddress, length + 2), 0, data);
                PlcString str = stringrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = '" + str + "'");
                break;
        }
    }

    private void doWriteCommand() throws IOException, AddressException, ValueOutOfRangeException, StringLengthException, IndexOutOfRangeException{
        if (conn == null){
            throw new IOException("no connection to plc");
        }
        if (db == NODB){
            throw new IOException("no db selected");
        }
        switch(dataType){
            case BOOL:
                //write value
                BitRxTx rxtx = new BitRxTx(conn, new Address(db, byteAddress, bitAddress, 1), 0, data);
                rxtx.set(value == 1).write();
                //and read it back
                boolean flag = rxtx.read().is(true);
                System.out.println(dataType + " at DB" + db + "." + byteAddress + "." + bitAddress + " set to " + flag);
                break;
            case BYTE:
            case CHAR:
                bitAddress = Address.NA;
                ByteRxTx byterxtx = new ByteRxTx(conn, new Address(db, byteAddress, bitAddress, 1), 0, data);
                byterxtx.set((byte)value).write();
                value = byterxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + (value & 0xFF));
                break;
            case WORD:
                bitAddress = Address.NA;
                WordRxTx wordrxtx = new WordRxTx(conn, new Address(db, byteAddress, bitAddress, 2), 0, data);
                wordrxtx.set((int)value).write();
                value = wordrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + (value & 0xFFFF));
                break;
            case INT:
                bitAddress = Address.NA;
                IntRxTx intrxtx = new IntRxTx(conn, new Address(db, byteAddress, bitAddress, 2), 0, data);
                intrxtx.set((int)value).write();
                value = intrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + value);
                break;
            case DWORD:
                bitAddress = Address.NA;
                DwordRxTx dwordrxtx = new DwordRxTx(conn, new Address(db, byteAddress, bitAddress, 4), 0, data);
                dwordrxtx.set(value).write();
                value = dwordrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + value);
                break;
            case DINT:
                bitAddress = Address.NA;
                DintRxTx dintrxtx = new DintRxTx(conn, new Address(db, byteAddress, bitAddress, 4), 0, data);
                dintrxtx.set((int)value).write();
                value = dintrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = " + value);
                break;
            case STRING:
                bitAddress = Address.NA;
                StringRxTx stringrxtx = new StringRxTx(conn, new Address(db, byteAddress, bitAddress, length + 2), 0, data);
                stringrxtx.set(new PlcString(stringValue,length)).write();
                PlcString str = stringrxtx.read().get();
                System.out.println(dataType + " at DB" + db + "." + byteAddress + " = \"" + str + "\"");
                break;
        }
    }

    private void doConnectCommand() throws IOException {
        try{
            if (conn != null){
                conn.close();
                System.out.println("previous connection to plc closed ");
                data = null;
            }
            conn = new Connection(ip, 0, 2, logger.isDebugEnabled());
            data = new Data(new byte[conn.getMaxPDULength()-PDUOVERHEAD]);
            conn.setDebug(logger.isDebugEnabled());
            System.out.println("connected to " + ip);
        }
        catch(IOException exc){
            conn = null;
            System.out.println("error occured while connecting to " + ip + " : " + exc.getMessage());
        }
    }

    private void doSelectCommand() {
        System.out.println("DB" + db + " selected");
    }

    private void doLoadCommand(){
        try{
            variables = new Variables(symListPath);
            System.out.println("Symlist loaded");
        }
        catch(Exception exc){
            System.out.println("error occured while loading the symlist");
        }
    }

    private void doGenerateCommand() {
        try{
            if (variables == null){
                throw new Exception("no symlist loaded");
            }
            structGenerator = new StructGenerator(accessorPath, accessorTitle, accessorPackage, variables);
            structGenerator.generate("DB"+accessorDb);
            System.out.println("accessor classes generated");
        }
        catch(Exception exc){
            System.out.println("error occured while generating accessor classes for DB" + accessorDb + " : "+ exc.getMessage());
        }
    }

    public static void main(String[] args) {
        try{
            Toolbox s7Access = new Toolbox();
            s7Access.doIt(args);
        }
        catch(Exception exc){
            exc.printStackTrace();
        }
    }
}
