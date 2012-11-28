/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Address.java
 * VERSION   : $Revision: 1.2 $
 * DATE      : $Date: 2012/03/14 14:38:41 $
 * PURPOSE   : Class for modbus addresses
 * AUTHOR    : Andreas Ulbrich, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: Address.java,v $
 * LOG       : Revision 1.2  2012/03/14 14:38:41  ulbrich
 * LOG       : modbus: Analog In & Output is implemented to be handled via register access
 * LOG       :
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

import org.jpac.IndexOutOfRangeException;;

/**
 * Implements the address of an MODBUS data item inside a data block
 */
public class Address extends org.jpac.plc.Address {
    /**
     * Java Enum for specifying the area in the modbus hardwareconfiguration where to read from / write to
     */
    public enum AREA {
        PHYSICAL_INPUT_ANALOG, 
        PHYSICAL_OUTPUT_ANALOG, 
        PHYSICAL_INPUT_DIGITAL,
        PHYSICAL_OUTPUT_DIGITAL,
        PFC_INPUT,
        PFC_OUTPUT,
        NOVRAM;
    };

    /**
     * Java Enum containing the offsets which to add to address values for exact modbus reference in requests.
     * This offset is depending on {@link org.jpac.plc.modbus.Address.AREA AREA}
     */
    public enum ModbusInternalMemoryOffset {
        BIT_OUTPUTS_ACCESS(0x0200),
        BIT_PFC_OUTPUT_ACCESS(0x1000),
        BIT_PFC_INPUT_ACCESS(0x2000),
        REGISTER_OUTPUTS_ACCESS (0x0200),
        REGISTER_PFC_OUTPUT_ACCESS(0x0100),
        REGISTER_PFC_INPUT_ACCESS(0x0300),
        NOVRAM(0x3000),
        BIT_INPUTS_ACCESS_AREA2 (0x8000),
        BIT_OUTPUTS_ACCESS_AREA2(0x9000),
        REGISTER_INPUTVAR_ACCESS_AREA2 (0x6000),
        REGISTER_OUTPUTVAR_ACCESS_AREA2(0x7000);

        private final int n;

        ModbusInternalMemoryOffset(int n){
            this.n = n;
        }
        /**
         * returns the ordinary of the enum value
         * @return the ordinary of the enum value
         */
        public int toInt(){
            return this.n;
        }
    }

    /**
     * The area where to read from / write to
     */
    AREA area;


    /**
     * @param area address area in which the data to be accessed resides
     * @param byteIndex the byte offset inside the datablock (DB): Any positive number or {@link Address#NA}, if not applicable inside a given context
     * @param bitIndex the bit offset inside the byte defined by byteIndex: 0..7 or {@link Address#NA}, if not applicable inside a given context
     * @param size the number of bytes occupied by the data item referenced by this address
     */
    public Address(AREA area, int byteIndex, int bitIndex, int size) throws IndexOutOfRangeException {
        super(byteIndex, bitIndex, size);
        if (bitIndex < NA || bitIndex > 15)
            throw new IndexOutOfRangeException();
        this.area = area;
    }

    /**
     * Getter method for returning the area to read from / write to
     * @return enum representing the area to read from / write to
     */
    public AREA getArea(){
        return this.area;
    }
}