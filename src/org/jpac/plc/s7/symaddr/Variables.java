/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Variables.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : <???>
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: Variables.java,v $
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

package org.jpac.plc.s7.symaddr;

import org.jpac.IndexOutOfRangeException;
import org.jpac.Signal;
import org.jpac.plc.s7.Address;
import org.jpac.plc.AddressException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.log4j.Logger;
import xBaseJ.CharField;
import xBaseJ.DBF;

/**
 *
 * @author berndschuster
 */
public class Variables{
    static Logger Log = Logger.getLogger("jpac.plc.s7.symaddr");

    private final String SYMLISTFILE = "SYMLIST.DBF";

    private Hashtable<String,Variable> listByIdentifier;
    private Hashtable<String,Variable> listByTypeSymbol;
    private Types                      types;
    private String                     symListDirectory;

    public Variables(String symListDirectory) throws AddressException{
        listByIdentifier      = new Hashtable<String,Variable>();
        listByTypeSymbol      = new Hashtable<String,Variable>();
        types                 = new Types();
        this.symListDirectory = symListDirectory;
        readVariableDeclarations();
    }

    public void add(Variable variable) throws VariableAlreadyExistsException{
        //variable is kept inside two hashmaps, so that it can retrieved by
        //identifier or by the types symbol (DBxxxx)
        String key = variable.getIdentifier().toUpperCase();
        if (listByIdentifier.containsKey(key))
            throw new VariableAlreadyExistsException(variable);
        listByIdentifier.put(key, variable);
        key = variable.getType().getSymbol();
        if (listByTypeSymbol.containsKey(key))
            throw new VariableAlreadyExistsException(variable);
        listByTypeSymbol.put(key, variable);
        Log.info("  adding var " + variable);
    }

    public Variable get(String key){
        //read out variable by identifier ...
        Variable var = listByIdentifier.get(key.toUpperCase());
        if (var == null){
            //... or by type symbol
            var = listByTypeSymbol.get(key.toUpperCase());
        }
        return var;
    }

    private void readVariableDeclarations() throws AddressException{
        try {
            //first read in and initialize the listByIdentifier of primitive and structured types
            types.readSubblks(symListDirectory);
            types.compileSubblks();
            readSymList();
        } catch (Exception exc) {
            Log.error("Error",exc);
            throw new AddressException("error occured while reading the subblocks");
        }
    }

    private void readSymList() throws AddressException{
        final String N_SKZ   = "N_SKZ";
        final String N_OPIEC = "N_OPIEC";
        final String _SKZ   = "_SKZ";
        final String _OPIEC = "_OPIEC";
        final String DB      = "DB";
        final String UDT     = "UDT";
        String subblkTyp;
        String identifier  = null;
        Type   type        = null;
        String skz;
        String opiec;
        try{
            DBF symListTable = new DBF(symListDirectory + File.separatorChar + SYMLISTFILE);
            //read out all entries related to DB's and UDT's
            Log.info("reading symbol list ...");
            for (int i = 1; i <= symListTable.getRecordCount(); i++){
                symListTable.read();
                //dbf files may contain dead (deleted) records
                if (!symListTable.deleted()){
                    //check, if SKZ field is names N_SKZ or _SKZ
                    try{
                        symListTable.getField(N_SKZ);
                        //field names are preceded by an 'N'
                        skz   = N_SKZ;
                        opiec = N_OPIEC;
                    }
                    catch(xBaseJ.xBaseJException exc){
                        //field names without a preceding 'N'
                        skz   = _SKZ;
                        opiec = _OPIEC;
                    }
                    //retrieve type symbol by eliminating all leading, trailing and intermediate blanks and convert it to uppercase
                    subblkTyp = ((CharField)symListTable.getField(opiec)).get().toUpperCase().replaceAll(" *", "");
                    if (subblkTyp.startsWith(DB) || subblkTyp.startsWith(UDT)){
                       //only DB's and UDT's are processed
                       identifier = ((CharField)(symListTable.getField(skz))).get().trim();
                       type = types.get(subblkTyp);
                       if (type != null){
                           if (type.isValid()){
                               //if the type has been parsed compiled and registered correctly
                               //assign the application specific identifier ...
                               type.setIdentifier(identifier);
                               if (subblkTyp.startsWith(DB)){
                                   //... and, if it's a DB, store it as a structured variable
                                   Variable var = new Variable(identifier,(StructType)type);
                                   add(var);
                               }
                           }
                           else{
                               Log.debug(" !! SKIPPING " + identifier + "(" + subblkTyp + "): invalid type");
                           }
                        }
                       else{
                           Log.debug(" !! SKIPPING " + identifier + "(" + subblkTyp + "): type not found");
                       }
                    }
                }
            }
            Log.info("... symbol list read");
        }
        catch(Exception exc){
            Log.error("Error",exc);
            throw new AddressException("error occured while reading the symbol list");
        }  
    }

    public Address getAddress(String canonicalSymbol) throws AddressException{
        Address         address = null;
        StringTokenizer st   = new StringTokenizer(canonicalSymbol);
        Variable        var  = get(st.nextToken("."));
        if (var == null){
            throw new AddressException("address not found: " + canonicalSymbol);
        }
        address = var.getAddress(st);
        Log.debug("retrieving address for " + canonicalSymbol + " : " + address);
        return address;
    }

    public Address getAddress(Vector<Signal> signals) throws AddressException{
        String canonicalSymbol  = new String();
        //build the fully qualified canonical symbol by the id's of the signals contained in the signal chain
        //the signal with index "0" is the signal itself, the signal with the highest index must be the connectable
        //which represents the top most data structure (the containing DB)
        canonicalSymbol = signals.get(signals.size()-1).getIdentifier();
        for (int i = signals.size() - 2; i >=0; i--)
            canonicalSymbol += '.' + signals.get(i).getIdentifier();
        return getAddress(canonicalSymbol);
    }

    protected StructType.Field getField(String canonicalSymbol) throws AddressException{
        StructType.Field field  = null;
        StringTokenizer st      = new StringTokenizer(canonicalSymbol);
        String          varName  = st.nextToken(".");
        Variable        var      = get(varName);
        if (var == null){
            throw new AddressException("variable not found: " + varName);
        }
        field = var.getField(st);
        //Log.debug("retrieving field for " + canonicalSymbol + " : " + field);
        return field;
    }

    public void dump(String fileName) throws IOException, AddressException, IndexOutOfRangeException{
        BufferedWriter   writer         = new BufferedWriter(new FileWriter(new File(fileName)));
        Vector<String>   identifierList = null;
        Address          address        = null;
        StructType.Field field          = null;
        String           typeStr        = null;
        
        for(Entry<String, Variable> entry: listByIdentifier.entrySet()){
            Variable var = entry.getValue();
            //dump variable itself
            writer.write(var.getIdentifier() + ';' + var.getType() + ';' + var.getAddress().asCSV() + '\n');
            //retrieve it's fields ...
            identifierList = var.retrieveIdentifierList();
            //... and dump them out
            for(int i = 0; i < identifierList.size(); i++){
                field   = getField(identifierList.elementAt(i));
                address = getAddress(identifierList.elementAt(i));
                //dump address information out to the CSV file ...
                typeStr = "";
                if(field.isArray())
                    typeStr = field.getArrayDimensions().toString();
                typeStr += field.getType();
                writer.write(identifierList.elementAt(i) + ';' + typeStr + ';' + address.asCSV() + '\n');
            }
        }
        writer.close();
    }

    public Types getTypes(){
        return types;
    }
}
