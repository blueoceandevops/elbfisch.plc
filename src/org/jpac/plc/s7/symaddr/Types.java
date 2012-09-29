/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Types.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : <???>
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: Types.java,v $
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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import xBaseJ.CharField;
import xBaseJ.DBF;
import xBaseJ.MemoField;
import xBaseJ.xBaseJException;

/**
 *
 * @author berndschuster
 */
public class Types{
    static Logger Log = Logger.getLogger("jpac.plc.s7.symaddr");

    private final String SUBBLKFILE = "SUBBLK.DBF";

    enum SubBlockTypes {UDT("00001"),
                        DB ("00006"),
                        OTHER("other");
         String subblkType;

         SubBlockTypes(String subblkType){
             this.subblkType = subblkType;
         }

         public static SubBlockTypes get(String id){
             SubBlockTypes sbt = OTHER;
             if (id.equals(UDT.subblkType)){
                 sbt = UDT;
             }else if (id.equals(DB.subblkType)){
                 sbt = DB;
             }
             return sbt;
         }
    }
    private Hashtable<String,Type> list;
    
    public Types(){
        list = new Hashtable<String,Type>();
        //add primitive types to the list
        list.put(Type.SupportedTypes.BOOL.toString(),new Type(Type.SupportedTypes.BOOL.toString()));
        list.put(Type.SupportedTypes.BYTE.toString(),new Type(Type.SupportedTypes.BYTE.toString()));
        list.put(Type.SupportedTypes.WORD.toString(),new Type(Type.SupportedTypes.WORD.toString()));
        list.put(Type.SupportedTypes.DWORD.toString(),new Type(Type.SupportedTypes.DWORD.toString()));
        list.put(Type.SupportedTypes.CHAR.toString(),new Type(Type.SupportedTypes.CHAR.toString()));
        list.put(Type.SupportedTypes.REAL.toString(),new Type(Type.SupportedTypes.REAL.toString()));
        list.put(Type.SupportedTypes.INT.toString(),new Type(Type.SupportedTypes.INT.toString()));
        list.put(Type.SupportedTypes.DINT.toString(),new Type(Type.SupportedTypes.DINT.toString()));
        list.put(Type.SupportedTypes.S5TIME.toString(),new Type(Type.SupportedTypes.S5TIME.toString()));
        list.put(Type.SupportedTypes.TIME.toString(),new Type(Type.SupportedTypes.TIME.toString()));
        list.put(Type.SupportedTypes.TIME_OF_DAY.toString(),new Type(Type.SupportedTypes.TIME_OF_DAY.toString()));
        list.put(Type.SupportedTypes.DATE.toString(),new Type(Type.SupportedTypes.DATE.toString()));
        list.put(Type.SupportedTypes.DATE_AND_TIME.toString(),new Type(Type.SupportedTypes.DATE_AND_TIME.toString()));    
    }

    public void add(Type type) throws TypeAlreadyExistsException{
        String key = type.getSymbol().toUpperCase();//SCL is not case sensitive
        if (list.containsKey(key)){
            throw new TypeAlreadyExistsException(type);
        }
        else{
            list.put(key, type);
        }
    }

    public Type get(String symbol){
        return list.get(symbol.toUpperCase());
    }

    public Enumeration<Type> getElements(){
        return list.elements();
    }

    public void readSubblks(String symListDirectory) throws xBaseJException, IOException, TypeAlreadyExistsException{
        DBF subBlocksTable = new DBF(symListDirectory + File.separatorChar + SUBBLKFILE);
        String subblkTyp;
        int    blockNumber = 0;
        for (int i = 1; i <= subBlocksTable.getRecordCount(); i++){
            subBlocksTable.read();
            if (!subBlocksTable.deleted()){//dbf files may contain dead (deleted) records ...
                subblkTyp   = ((CharField)subBlocksTable.getField("SUBBLKTYP")).get();
                SubBlockTypes subBlockType = SubBlockTypes.get(subblkTyp);
                if (subBlockType != SubBlockTypes.OTHER){
                   //accept UDT's and DB's only
                   //read lexical sub block definition
                   blockNumber = new Integer(((CharField)subBlocksTable.getField("BLKNUMBER")).get().trim());
                   StructType type = new StructType(this, subBlockType.toString(), blockNumber);
                   type.setDeclaration(((MemoField)subBlocksTable.getField("MC5CODE")).get().trim());
                   //append new subblock type to list
                   add(type);
                }
            }
        }
    }

    public void assignIdentifier(String symbol, String identifier){
        get(symbol).setIdentifier(identifier);
    }

    public void compileSubblks(){
         for (Enumeration<Type> e = list.elements() ; e.hasMoreElements() ;) {
             Type type = e.nextElement();
             if (type instanceof StructType && !type.isValid()){
                 ((StructType)type).parseSubblock();
             }
     }

    }
}
