/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : StructType.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : <???>
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: StructType.java,v $
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
import org.jpac.plc.s7.symaddr.lexer.Lexer;
import org.jpac.plc.s7.symaddr.lexer.LexerException;
import org.jpac.plc.s7.symaddr.node.AArrayType;
import org.jpac.plc.s7.symaddr.node.AArraydecl;
import org.jpac.plc.s7.symaddr.node.AArraydim;
import org.jpac.plc.s7.symaddr.node.AMultipleArraydimList;
import org.jpac.plc.s7.symaddr.node.ASingleArraydimList;
import org.jpac.plc.s7.symaddr.node.AStringType;
import org.jpac.plc.s7.symaddr.node.AStruct;
import org.jpac.plc.s7.symaddr.node.AStructType;
import org.jpac.plc.s7.symaddr.node.AUdtType;
import org.jpac.plc.s7.symaddr.node.AVar;
import org.jpac.plc.s7.symaddr.node.AVariablesTypeAssignment;
import org.jpac.plc.s7.symaddr.node.Node;
import org.jpac.plc.s7.symaddr.node.PArraydimList;
import org.jpac.plc.s7.symaddr.node.Start;
import org.jpac.plc.s7.symaddr.parser.Parser;
import org.jpac.plc.s7.symaddr.parser.ParserException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 *
 * @author berndschuster
 */
public class StructType extends Type{

    protected class ArrayDimensions{
        public int[] dimension  = new int[10];
        public int[] firstIndex = new int[10];
        public int[] lastIndex  = new int[10];
        public int   numberOfDimensions = 0;

        @Override
        public String toString(){
            StringBuffer str = new StringBuffer(" ARRAY[");
            for(int i = 0; i < numberOfDimensions; i++){
                str = str.append(dimension[i]);
                if (i < numberOfDimensions -1){
                    str = str.append(',');
                }
            }
            str = str.append("] OF ");
            return str.toString();
        }
    }


    public class Field{
        private String          identifier;
        private ArrayDimensions arrayDims;
        private Type            type;
        private int             byteOffset;             //byte offset inside the struct
        private int             bitOffset;              //bit offset inside the current byte

        public Field(String identifier, ArrayDimensions arrayDims, Type type, int byteOffset, int bitOffset){
            this.identifier           = identifier;
            this.arrayDims            = arrayDims;
            this.type                 = type;
            this.byteOffset           = byteOffset;
            this.bitOffset            = bitOffset;
        }

        public String getIdentifier() {
            return identifier;
        }

        public Type getType() {
            return type;
        }

        public int getByteOffset() {
            return byteOffset;
        }

        public int getBitOffset() {
            return bitOffset;
        }

        public ArrayDimensions getArrayDimensions(){
            return arrayDims;
        }

        public boolean isArray(){
            return arrayDims != null;
        }

        public int getSize() {
            int factor = 1;
            if (isArray()){
                //this is an array
                switch(type.getTypeId()){
                    case BOOL://TODO check for multidimensional arrays
                         //calculate blocksize of the last dimension by rounding up to the next byte boundary
                         factor = (arrayDims.dimension[arrayDims.numberOfDimensions-1] + 7)/8;
                         //and multiply this with the preceding dimensions
                         for (int i = 0; i < arrayDims.numberOfDimensions - 1; i++){
                             factor *= arrayDims.dimension[i];
                         }
                         //round up data block occupied by this array to word boundary
                         factor = factor + factor % 2;
                         break;
                    case BYTE://TODO check for multidimensional arrays
                    case CHAR://TODO check for multidimensional arrays
                         factor = 1;
                         for (int i = 0; i < arrayDims.numberOfDimensions; i++){
                             factor *= arrayDims.dimension[i];
                         }
                         //round up data block occupied by this array to word boundary
                         factor = factor + factor % 2;
                         break;
                    default:
                         factor = 1;
                         for (int i = 0; i < arrayDims.numberOfDimensions; i++){
                             factor *= arrayDims.dimension[i];
                         }
                }
            }
            return factor * type.getSize();
        }

        protected Address getAddress() throws AddressException{
            Address address = null;
            try{
                address = new Address(Address.NA,byteOffset,getType().getTypeId() == SupportedTypes.BOOL ? bitOffset : Address.NA, getSize());
            }
            catch(IndexOutOfRangeException exc){
                Log.error("Error:", exc);
                throw new AddressException("index out of range");
            }
            return address;
        }

        protected Address getAddress(StringTokenizer canonicalSymbol) throws AddressException{
            Address address  = null;
            if (canonicalSymbol.hasMoreElements() && type instanceof StructType){
                //is itself part of an structured type and a deeper field is requested
                //retrieve address information recursively ...
                StructType.Field field = null;
                String fieldIdentifier = canonicalSymbol.nextToken(".");
                field   = ((StructType)type).getField(fieldIdentifier);
                if (field == null){
                    throw new AddressException("address not found: " + fieldIdentifier);
                }
                address = field.getAddress(canonicalSymbol);
                //... and add own byteOffset to it
                address.setByteIndex(address.getByteIndex() + byteOffset);
            }
            else{
                //this field is requested or it is a primitive type or a string
                //just return its address
                if (canonicalSymbol.hasMoreElements()){
                    throw new AddressException("inconsistent address qualifier");
                }
                address = getAddress();
            }
            return address;
        }

        @Override
        public String toString(){
            String str;
            if (arrayDims == null)
                str = byteOffset + "." + bitOffset + "\t" + identifier + ":" + type;
            else
                str = byteOffset + "." + bitOffset + "\t" + identifier + ":" + arrayDims + type;
            return str;
        }
    }

    private class TypeDeclaration{
        public Type type;
        public ArrayDimensions arrayDims;
    }
    
    private Types   types;                //used for recursive parsing of udt declarations
    private int     blockNumber;
    private int     currentByteOffset;
    private int     currentBitOffset;
    private boolean inBoolSequence;
    private boolean parsingErrorDetected; //true, if an error was encountered while parsing the struct
    private String  declaration;          //lexical declaration of the structured data type
    private int     nestedStructIndex;    //unique index for nested structures found inside this struct
 
    private LinkedHashMap<String,Field> fieldList; //key = toUpperCase(symbol), ordered as inserted

    //constructor for standalone invocation without types table (test environment)
    public StructType(String symbol, int blockNumber){
        super(symbol + new Integer(blockNumber).toString().trim());
        initialize();
        this.types          = null;
        this.blockNumber    = blockNumber;
    }

    //constructor for productive environment)
    public StructType(Types types, String symbol, int blockNumber){
        super(symbol + new Integer(blockNumber).toString().trim());
        initialize();
        this.types                  = types;
        this.blockNumber            = blockNumber;
    }

    private void initialize(){
        this.fieldList              = new LinkedHashMap<String,Field>();
        this.currentByteOffset      = 0;
        this.currentBitOffset       = 0;
        this.inBoolSequence         = false;
        this.parsingErrorDetected   = false;
        this.nestedStructIndex      = 0;
    }

    @Override
    public void inAStruct(AStruct node)
    {
        super.inAStruct(node);
        Log.debug("STRUCT " + getSymbol());
    }

    @Override
    public void outAStruct(AStruct node)
    {
        super.outAStruct(node);
        closeStructType();
        Log.debug("END_STRUCT " + getSymbol() + ": " + getSize());
    }

    @Override
    public void inAVar(AVar node)
    {
        super.inAVar(node);
        Log.debug("VAR " + getSymbol());
    }

    @Override
    public void outAVar(AVar node)
    {
        super.outAVar(node);
        closeStructType();
        Log.debug("END_VAR " + getSymbol() + ": " + getSize());
    }

    @Override
    public void inAStructType(AStructType node)
    {
        super.inAStructType(node);
        Log.debug(">>>> nested");
    }

    @Override
    public void outAStructType(AStructType node)
    {
        super.outAStructType(node);
        Log.debug("<<<< nested");
    }

    @Override
    public void caseAVariablesTypeAssignment(AVariablesTypeAssignment node){
        TypeDeclaration typeDeclaration = determineType(node);
        Type            type            = typeDeclaration.type;
        ArrayDimensions arrayDims       = typeDeclaration.arrayDims;

        StringTokenizer identifiers = new StringTokenizer(node.getIdentifierList().toString());
        try{
            if (!type.isValid()){
                throw new InvalidTypeException(type);
            }
            closeBoolSequenceIfNeeded(typeDeclaration);
            if (type.getSize() > 1 || arrayDims != null){
               //if the current field occupies more than 1 byte
               //adjust the byte offset to word boundary
               currentByteOffset += currentByteOffset % 2;
            }
            do{
                String identifier = identifiers.nextToken(",").trim();
                String key        = identifier.toUpperCase();
                if (fieldList.containsKey(key)){
                    throw new FieldAlreadyDeclaredException(identifier);
                }
                Field field = new Field(identifier, arrayDims, type, currentByteOffset, currentBitOffset);
                fieldList.put(key, field);// scl identifiers are not case sensitive
                Log.debug("   " + field);
                adjustCurrentOffsets(field);
            }
            while(identifiers.hasMoreTokens());
        }
        catch(InvalidTypeException exc){
            parsingErrorDetected = true;
            Log.error(exc);
        }
        catch(UnsupportedTypeException exc){
            parsingErrorDetected = true;
            Log.error(exc);
        }
        catch(FieldAlreadyDeclaredException exc){
            parsingErrorDetected = true;
            Log.error(exc);
        }
    }

    private TypeDeclaration determineType(AVariablesTypeAssignment node){
        TypeDeclaration     typeDeclaration = new TypeDeclaration();
        Node    nodeType    = node.getType();
        Type    type        = null;
        boolean isArray     = false;
        ArrayDimensions     arrayDims = null;

        if (nodeType instanceof AArrayType){
            //type is an array of something.
            //first determine its dimensions ...
            arrayDims = handleArrayDimensions((AArrayType) nodeType);
            //... and it's "native" type
            nodeType = ((AArraydecl)((AArrayType)nodeType).getArraydecl()).getType();
            isArray = true;
            //then process the type
        }
        //handle string type
        if (nodeType instanceof AStringType){
           type = new StringType((AStringType)node.getType());
        //handle udt type
        } else if (nodeType instanceof AUdtType){
           //retrieve the struct type from hash table
           type = handleUdt(nodeType);
        } else if (nodeType instanceof AStructType){
           //retrieve the struct type from hash table
           type = handleStruct(nodeType);
        }
        //handle other types
        else{
           type = new Type(nodeType.toString().trim());
        }
        if (isArray){
            //type is an array of something
            //add it's dimensions as determined above
            typeDeclaration.arrayDims = arrayDims;
        }
        typeDeclaration.type = type;
        return typeDeclaration;
    }

    private ArrayDimensions handleArrayDimensions(AArrayType nodeType){
        ArrayDimensions arrayDims = new ArrayDimensions();
        arrayDims = handleMultipleArrayDimList(((AArraydecl)((AArrayType)nodeType).getArraydecl()).getArraydimList(), arrayDims);
        return arrayDims;
    }

    private ArrayDimensions handleMultipleArrayDimList(PArraydimList nodeType, ArrayDimensions arrayDims){
        AArraydim aArraydim;
        if (nodeType instanceof AMultipleArraydimList){
            //retrieve array dimension
            aArraydim = (AArraydim)((AMultipleArraydimList)nodeType).getArraydim();
            //and handle the MultipleArraydimList contained in this node (recursive call)
            handleMultipleArrayDimList(((AMultipleArraydimList)nodeType).getArraydimList(), arrayDims);
        }
        else{
            //must be a single arraydim
            aArraydim = (AArraydim)((ASingleArraydimList)nodeType).getArraydim();
        }
        //get indices, first remove positive signs and leading, intermediate and trailing blanks
        int firstIndex  = new Integer(aArraydim.getFirstindex().toString().replace('+', ' ').replaceAll(" *", ""));
        int lastIndex   = new Integer(aArraydim.getLastindex().toString().replace('+', ' ').replaceAll(" *", ""));
        //compute dimension and store it
        arrayDims.dimension[arrayDims.numberOfDimensions]  = lastIndex - firstIndex + 1;
        arrayDims.firstIndex[arrayDims.numberOfDimensions] = firstIndex;
        arrayDims.lastIndex[arrayDims.numberOfDimensions]  = lastIndex;
        arrayDims.numberOfDimensions++;
        return arrayDims;
    }

    private Type handleUdt(Node nodeType){
        Type   type   = null;
        String number = ((AUdtType)(nodeType)).getNumber().toString().trim();
        if (types != null){
           //if invoked inside the Types list
           //get list entry
           type = types.get(SupportedTypes.UDT.toString() + number);
           if (!type.isValid()){
              //if the declaration of the struct type has not already been parsed, do it now (recursion)
              ((StructType)type).parseSubblock();
           }
        }
        else{
           //if invoked standalone for test purposes, just return an empty structType, which is marked valid
           type = new StructType(SupportedTypes.UDT.toString(), new Integer(number.trim()));
           type.setValid(true);
        }
        return type;
    }

    private Type handleStruct(Node nodeType){
        Type   type   = null;
        //a nested struct definition found
        //instantiate a new StructType
        type = new StructType(types, getSymbol()+'$', nestedStructIndex++);
        //and let it parse it (recursion)
        ((AStructType)nodeType).apply(type);
        //give the nested structure an legal identifier
        type.setIdentifier(type.getSymbol());
        //the nested structure is a valid type
        type.setValid(true);
        return type;
    }

    private void adjustCurrentOffsets(Field field) throws UnsupportedTypeException{
        Type type = field.getType();
        if(type.isSupportedType()){
            if (field.getArrayDimensions() != null){
                //types defined as arrays always return the correct number of bytes
                currentByteOffset += field.getSize();
            }
            else{
                //check for non array types
                switch (SupportedTypes.get(type.getSymbol())){
                    case BOOL:
                         if (currentBitOffset == 7){
                            //next bit will be bit 0 of next byte
                            currentByteOffset++;
                            currentBitOffset = 0;
                         }
                         else{
                            //next bit will be next bit in current byte
                            currentBitOffset++;
                         }
                         inBoolSequence = true;//bool is part of bool sequence
                         break;
                    default:
                         currentByteOffset += type.getSize();
                         break;
                }
            }
        }
        else{
            parsingErrorDetected = true;
            throw new UnsupportedTypeException(type.getSymbol());
        }
    }
    private void closeStructType(){
        if (inBoolSequence){
            closeBoolSequence();
        }
        //adjust the byte offset to word boundary
        currentByteOffset += currentByteOffset % 2;
        setSize(currentByteOffset);
        setValid(parsingErrorDetected);
    }
    
    private void closeBoolSequence(){
         //sequence of bool declarations ended: close bool sequence and adjust byte offset
         if (currentBitOffset > 0){
             currentByteOffset++;
         }
         currentBitOffset = 0;
         inBoolSequence   = false;
    }

    private void closeBoolSequenceIfNeeded(TypeDeclaration typeDeclaration) throws UnsupportedTypeException{
        if(typeDeclaration.type.isSupportedType()){
            if ((SupportedTypes.get(typeDeclaration.type.getSymbol()) != SupportedTypes.BOOL) || typeDeclaration.arrayDims != null){
                if (inBoolSequence){
                    closeBoolSequence();
                }
            }
        }
        else{
            parsingErrorDetected = true;
            throw new UnsupportedTypeException(typeDeclaration.type.getSymbol());
        }
    }

    public void parseSubblock(){
       try{
           //read lexical sub block definition
           Lexer lexer = new Lexer (new PushbackReader(new BufferedReader(new StringReader(getDeclaration())), 1024));
           Parser parser = new Parser(lexer);
           Start ast = parser.parse();
           ast.apply(this);
           //if everything worked fine, this type declaration is valid
           setValid(true);
       }
       catch(IOException exc){
            parsingErrorDetected = true;
            Log.error("Error found in lexical subblk declaration for " + getSymbol() + ":", exc);
       }
       catch(ParserException exc){
            parsingErrorDetected = true;
            //some subblocks can not be processed by jpac. These will be omitted
            Log.debug("Error found in lexical subblk declaration for " + getSymbol() + ":" + exc);
       }
       catch(LexerException exc){
            parsingErrorDetected = true;
            //some subblocks can not be processed by jpac. These will be omitted
            Log.debug("Error found in lexical subblk declaration for " + getSymbol() + ":" + exc);
       }
    }

    @Override
    protected Address getAddress(StringTokenizer canonicalSymbol) throws IndexOutOfRangeException{
        //instantiate an address object and initialize it with the size of this type
        Address address = super.getAddress(canonicalSymbol);
        
        return address;
    }

    public int getBlockNumber(){
        return blockNumber;
    }
    
    public Field getField(String identifier){
        return fieldList.get(identifier.toUpperCase());
    }

    protected Field getField(StringTokenizer canonicalSymbol) throws AddressException{
        Field field  = null;
        String fieldIdentifier = canonicalSymbol.nextToken(".");
        field = getField(fieldIdentifier);
        if (canonicalSymbol.hasMoreElements() && field.getType() instanceof StructType){
            //is itself part of an structured type and a deeper field is requested
            //retrieve field recursively ...
            field   = ((StructType)field.getType()).getField(canonicalSymbol);
            if (field == null){
                throw new AddressException("field not found: " + canonicalSymbol);
            }
        }
        else{
            //this field is requested or it is a primitive type or a string
            //just return its address
            if (canonicalSymbol.hasMoreElements()){
                throw new AddressException("inconsistent address qualifier");
            }
        }
        return field;
    }

    public LinkedHashMap<String,Field> getFields(){
        return fieldList;
    }

    public String getDeclaration() {
        return declaration;
    }

    public void setDeclaration(String declaration) {
        this.declaration = declaration;
    }

    void retrieveIdentifierList(String parentsIdentifier, Vector<String> identifierList){
        String qualifiedIdentifier = null;

        for(Entry<String,Field> entry: fieldList.entrySet()){
            Field field = entry.getValue();
            qualifiedIdentifier = parentsIdentifier + '.' + field.getIdentifier();
            identifierList.add(qualifiedIdentifier);
            if (field.getType() instanceof StructType){
                //... and recurse to deeper structures, if needed
                ((StructType)field.getType()).retrieveIdentifierList(qualifiedIdentifier, identifierList);
            }
        }
    }

}
