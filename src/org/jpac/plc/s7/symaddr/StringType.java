/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : StringType.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : <???>
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: StringType.java,v $
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

import org.jpac.plc.s7.symaddr.node.AStringType;
import org.jpac.plc.s7.symaddr.node.AStringdecl;


/**
 *
 * @author berndschuster
 */
public class StringType extends Type{
    private int length;
    public StringType(AStringType node){
        super(SupportedTypes.STRING.toString());
        length = new Integer(((AStringdecl)node.getStringdecl()).getNumber().toString().trim());
        //determine over all size of the string in bytes
        //a plc string contains 2 byte of maintenance data
        size = length + 2;
    }

    public int getLength() {
        return length;
    }
}
