/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Type.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : <???>
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: Type.java,v $
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
import org.jpac.plc.s7.symaddr.analysis.DepthFirstAdapter;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 *
 * @author berndschuster
 */
public class Type extends DepthFirstAdapter{
    static Logger Log = Logger.getLogger("jpac.plc.s7.symaddr");
    
    public enum SupportedTypes {BOOL(1),  // size is at least 1 byte
                                BYTE(1),
                                WORD(2),
                                DWORD(4),
                                CHAR(1),
                                INT(2),
                                DINT(4),
                                REAL(4),
                                S5TIME(2),
                                TIME(4),
                                TIME_OF_DAY(4),
                                DATE(2),
                                DATE_AND_TIME(8),
                                STRING(0),// size is variable
                                UDT(0),   // size is variable
                                DB(0),    // size is variable
                                UNDEFINED(0);
           private int size;

           SupportedTypes(int size){
               this.size = size;
           }

           public int getSize(){
               return this.size;
           }
           public static SupportedTypes get(String symbol){
               SupportedTypes value = UNDEFINED;
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
               if (REAL.toString().equals(symbol))
                  value = REAL;
               if (S5TIME.toString().equals(symbol))
                  value = S5TIME;
               if (TIME.toString().equals(symbol))
                  value = TIME;
               if (TIME_OF_DAY.toString().equals(symbol))
                  value = TIME_OF_DAY;
               if (DATE.toString().equals(symbol))
                  value = DATE;
               if (DATE_AND_TIME.toString().equals(symbol))
                  value = DATE_AND_TIME;
               if (STRING.toString().equals(symbol))
                  value = STRING;
               if (symbol.startsWith(UDT.toString()))
                  value = UDT;
               if (symbol.startsWith(DB.toString()))
                  value = DB;
               return value;
           }
    };
    
    private   String          symbol;          //a symbolic name for this type (conforming to IEC)
    private   String          identifier;      //optional identifier given by the application programmer
    protected int             size;            //storage occupied by instances of this type in bit's
    private   boolean         valid;           //true, if this type is fully qualified
    private   boolean         supportedType;   //true, if the type ist one of the primitives defined above
    private   SupportedTypes  typeId;

    public Type(String symbol){
        this.symbol = symbol.toUpperCase();
        SupportedTypes actualType = SupportedTypes.get(this.symbol);
        this.typeId               = actualType;
        switch(actualType){
            case UDT:
            case DB: this.size = 0;
                     this.valid = false;         //udt's and db's must be compiled to get valid
                     this.supportedType = true;
                     break;
                     
            case UNDEFINED:
                     this.size = 0;
                     this.valid = false;
                     this.supportedType = false;
                     break;

            default: this.size = actualType.getSize();
                     this.valid = true;
                     this.supportedType = true;
                     break;
        }
    }

    protected Address getAddress(StringTokenizer canonicalSymbol) throws IndexOutOfRangeException{
        //instantiate an address object and initialize it with the size of this type
        Address address = new Address(Address.NA,Address.NA,Address.NA,getSize());
        return address;
    }

    protected void setIdentifier(String identifier){
        this.identifier = identifier;
    }

    public boolean isSupportedType(){
        return supportedType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getIdentifier() {
        return identifier;
    }

    protected void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public SupportedTypes getTypeId(){
        return typeId;
    }

    /**
     * @return the valid
     */
    public boolean isValid() {
        return valid;
    }

    protected void setValid(boolean valid){
        this.valid = valid;
    }

    @Override
    public String toString(){
        return getSymbol();
    }
}
