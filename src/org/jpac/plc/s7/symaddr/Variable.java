/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Variable.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : <???>
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: Variable.java,v $
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
import org.jpac.plc.s7.Address;
import org.jpac.plc.AddressException;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
 *
 * @author berndschuster
 */
public class Variable {
    static Logger Log = Logger.getLogger("jpac.plc.s7.symaddr");

    private StructType   type;
    private String identifier;
    public Variable(String identifier, StructType type){
        this.identifier = identifier;
        this.type       = type;
    }

    public StructType getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString(){
        return identifier + ": " + type;
    }

    protected Address getAddress() throws AddressException{
        Address address = null;
        try{
            address = new Address(type.getBlockNumber(),Address.NA,Address.NA,type.getSize());
        }
        catch(IndexOutOfRangeException exc){
            Log.error("Error:", exc);
            throw new AddressException("index out of range");
        }
        return address;
    }

    @SuppressWarnings("empty-statement")
    protected Address getAddress(StringTokenizer canonicalSymbol) throws AddressException{
        Address address = null;
        if (canonicalSymbol.hasMoreElements()){
            // a deeper field is requested
            String fieldIdentifier = canonicalSymbol.nextToken(".");
            StructType.Field field = type.getField(fieldIdentifier);
            if(field == null) {
                Log.error("Test: " + fieldIdentifier);
            }
            //retrieve the offset address of the variable inside the DB ...
            address = field.getAddress(canonicalSymbol);
            //... and add the DB's block number
            address.setDb(type.getBlockNumber());
        }
        else{
            //just return this address
            address = getAddress();
        }
        return address;
    }

    protected StructType.Field getField(StringTokenizer canonicalSymbol) throws AddressException{
        StructType.Field field = null;
        if (canonicalSymbol.hasMoreElements()){
            // a deeper field is requested
            String fieldIdentifier = canonicalSymbol.nextToken(".");
            field = type.getField(fieldIdentifier);
            if (canonicalSymbol.hasMoreElements() && field.getType() instanceof StructType){
                //is itself part of an structured type and a deeper field is requested
                //retrieve field recursively ...
                field = ((StructType)field.getType()).getField(canonicalSymbol);
            }
        }
        else{
            throw new AddressException("not a field");
        }
        return field;
    }

    protected Vector<String> retrieveIdentifierList(){
        Vector<String>   identifierList =  new Vector<String>(10);
        type.retrieveIdentifierList(identifier, identifierList);
        return identifierList;
    }

}
