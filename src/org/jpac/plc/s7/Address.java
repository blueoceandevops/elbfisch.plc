/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Address.java
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

import org.jpac.IndexOutOfRangeException;


/**
 * Implements the address of an S7 data item inside a data block (DB)
 */
public class Address extends org.jpac.plc.Address{
    /**
     * denotes, that the declaration of a valid value is not applicable for a part of the address in the given context
     */
    private int db;

    /**
     * @param db the number of the datablock (DB): Any positive number or {@link Address#NA}, if not applicable inside a given context
     * @param byteIndex the byte offset inside the datablock (DB): Any positive number or {@link Address#NA}, if not applicable inside a given context
     * @param bitIndex the bit offset inside the byte defined by byteIndex: 0..7 or {@link Address#NA}, if not applicable inside a given context
     * @param size the number of bytes occupied by the data item referenced by this address
     */
    public Address(int db, int byteIndex, int bitIndex, int size) throws IndexOutOfRangeException {
        super(byteIndex, bitIndex, size);
        if (db < NA)
            throw new IndexOutOfRangeException();        
        this.db        = db;
    }
    
    @Override
    public String toString() {
        String address = (getDb() != NA ? getDb() : "-") + ";" + super.toString();
        return getClass().getSimpleName() + "(" + address + ")";
    }

    /**
     * @return a string representation of the object as a character separated string (';')
     */
    public String asCSV() {
        return (getDb() != NA ? getDb() : "") + ";" + super.asCSV();
    }

    public int getDb() {
        return db;
    }

    public void setDb(int db) {
        this.db = db;
    }

    @Override
    public Object clone() throws CloneNotSupportedException{
        Address cloned;
        cloned = (Address)super.clone();
        cloned.setDb(db);
        return cloned;
    }
}