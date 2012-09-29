/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : Modbus.java
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

package org.jpac.plc.modbus.util;

/**
 *
 * @author Andreas Ulbrich
 */
public class Modbus {
   /**
   * default port number of Modbus
   */
  public static final int DEFAULT_PORT = 502;

  /**
   * maximum message length in bytes
   */
  public static final int MAX_MESSAGE_LENGTH = 256;

  /**
   * default transaction identifier
   */
  public static final int DEFAULT_TRANSACTION_ID = 0;

  /**
   * default protocol identifier
   */
  public static final int DEFAULT_PROTOCOL_ID = 0;

  /**
   * default unit identifier
   */
  public static final int DEFAULT_UNIT_ID = 0;


  /**
   * default setting for I/O operation timeouts
   * in milliseconds
   */
  public static final int DEFAULT_TIMEOUT = 10;

  /**
   * default amount of retires for opening
   * a connection
   */
  public static final int DEFAULT_RETRIES = 3;

  /**
   * maximum value of the transaction identifier.
   */
  public static final int MAX_TRANSACTION_ID = (Short.MAX_VALUE * 2);

  public static final int MODBUS_FUNCTIONCODE_READINPUTDISCRETES = 2;
  public static final int MODBUS_FUNCTIONCODE_READMULTIPLEREGISTERS = 3;
  public static final int MODBUS_FUNCTIONCODE_WRITECOILS = 15;
  public static final int MODBUS_FUNCTIONCODE_WRITEMULTIPLEREGISTERS = 16;

  private static int transactionID = Modbus.DEFAULT_TRANSACTION_ID;

  /**
   * Toggles the transaction identifier, to ensure
   * that each transaction has a distinctive
   * identifier.<br>
   * When the maximum value of 65535 has been reached,
   * the identifiers will start from zero again.
   */
  public static int getNextTransactionID() {
    int rtTransactionId = transactionID;
    if (transactionID == Modbus.MAX_TRANSACTION_ID) {
        transactionID = 0;
    } else {
        transactionID++;
    }
    return rtTransactionId;
  }
}
