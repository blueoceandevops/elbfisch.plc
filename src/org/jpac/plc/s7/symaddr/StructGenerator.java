/**
 * PROJECT   : jPac PLC communication library
 * MODULE    : StructGenerator.java
 * VERSION   : $Revision: $
 * DATE      : $Date: $
 * PURPOSE   : 
 * AUTHOR    : Bernd Schuster, MSK Gesellschaft fuer Automatisierung mbH, Schenefeld
 * REMARKS   : -
 * CHANGES   : CH#n <Kuerzel> <datum> <Beschreibung>
 * LOG       : $Log: StructGenerator.java,v $
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

import org.jpac.plc.AddressException;
import org.jpac.plc.BitRxTx;
import org.jpac.plc.ByteRxTx;
import org.jpac.plc.DintRxTx;
import org.jpac.plc.DwordRxTx;
import org.jpac.plc.IntRxTx;
import org.jpac.plc.StringRxTx;
import org.jpac.plc.WordRxTx;
import org.jpac.plc.s7.symaddr.Type.SupportedTypes;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 *
 * @author berndschuster
 */
public class StructGenerator {

    private static HashSet<String> generatedClasses = new HashSet<String>(10); //used to avoid generating source files more than once
    
    private class Declaration{
        protected String sourceCode;
        public void write(BufferedWriter writer) throws IOException{
            writer.write(sourceCode);
        }
    }

    private class Header extends Declaration{
        public Header(StructType type){
            sourceCode = new String(headerTemplate);
            sourceCode = sourceCode.replace(PROJECTNAMETAG, projectName);
            sourceCode = sourceCode.replace(VARIABLETAG, type.getIdentifier());
            sourceCode = sourceCode.replace(PACKAGENAMETAG, packageName);
            sourceCode = sourceCode.replace(REVISIONTAG,"\u0024Revision\u0024");
            sourceCode = sourceCode.replace(DATETAG,"\u0024Date\u0024");
        }
    }

    private class Imports extends Declaration{
        HashSet<String> additionalImports;
        
        public Imports(){
            sourceCode = new String(importTemplate);
            additionalImports = new HashSet<String>(5);
        }

        public void add(String className){
            this.additionalImports.add(className);
        }
        
        @Override
        public void write(BufferedWriter writer) throws IOException{
            for(String additionalImport : additionalImports){
                sourceCode += "import " + additionalImport + ";\n";
            }
            super.write(writer);
        }
    }

    private class ClassBody extends Declaration{

        public ClassBody(StructType type) throws IOException, AddressException{
            sourceCode = new String(classBodyTemplate);
            sourceCode = sourceCode.replace(VARIABLETAG, type.getIdentifier());
            sourceCode = sourceCode.replace(VARIABLESIZE,Integer.toString(type.getSize()));
            sourceCode = sourceCode.replace(FIELDDECLARATIONS,genFieldDeclarations(type));
            sourceCode = sourceCode.replace(ACCESSORDECLARATIONS,genAccessorDeclarations(type));
        }
        
        private String genFieldDeclarations(StructType type) throws AddressException, IOException{
            String fieldDeclarations = "\n";
            String arrayBrackets;
            LinkedHashMap<String,StructType.Field> fields = type.getFields();
            for(Entry<String,StructType.Field> entry: fields.entrySet()){
                StructType.Field field = entry.getValue();
                arrayBrackets = genArrayBrackets(field, false);
                switch(field.getType().getTypeId()){
                    case BOOL:
                            fieldDeclarations += "    private BitRxTx" + arrayBrackets + " \t\t" + field.getIdentifier() + ";\n";
                            imports.add(BitRxTx.class.getCanonicalName());
                            break;
                    case BYTE:
                    case CHAR:
                            fieldDeclarations += "    private ByteRxTx" + arrayBrackets + " \t\t" + field.getIdentifier() + ";\n";
                            imports.add(ByteRxTx.class.getCanonicalName());
                            break;
                    case INT:
                            fieldDeclarations += "    private IntRxTx" + arrayBrackets + " \t\t" + field.getIdentifier() + ";\n";
                            imports.add(IntRxTx.class.getCanonicalName());
                            break;
                    case WORD:
                            fieldDeclarations += "    private WordRxTx" + arrayBrackets + " \t\t" + field.getIdentifier() + ";\n";
                            imports.add(WordRxTx.class.getCanonicalName());
                            break;
                    case DINT:
                            fieldDeclarations += "    private DintRxTx" + arrayBrackets + " \t\t" + field.getIdentifier() + ";\n";
                            imports.add(DintRxTx.class.getCanonicalName());
                            break;
                    case DWORD:
                            fieldDeclarations += "    private DwordRxTx" + arrayBrackets + " \t\t" + field.getIdentifier() + ";\n";
                            imports.add(DwordRxTx.class.getCanonicalName());
                            break;
                    case STRING:
                            fieldDeclarations += "    private StringRxTx" + arrayBrackets + " \t\t" + field.getIdentifier() + ";\n";
                            imports.add(StringRxTx.class.getCanonicalName());
                            break;
                    case UDT:
                            fieldDeclarations += "    private " + field.getType().getIdentifier() + arrayBrackets + " \t\t" + field.getIdentifier() + ";\n";
                            //generate source files for nested classes found inside this structure (recursion)
                            new StructGenerator(destinationPath, projectName, packageName, variables).generateType((StructType)field.getType());
                            break;
                    default:
                            fieldDeclarations += "     //not supported yet:    " + field.getIdentifier() + ";\n";
                }
            }
            return fieldDeclarations;
        }
        
        private String genAccessorDeclarations(StructType type) throws IOException{
            String accessorDeclarations  = "";
            String accessorDeclaration   = "";
            String rxtxClass             = "";
            String accessorIdentifier    = "";

            boolean fieldTypeSupported;

            LinkedHashMap<String,StructType.Field> fields = type.getFields();
            for(Entry<String,StructType.Field> entry: fields.entrySet()){

                fieldTypeSupported = true;

                StructType.Field field = entry.getValue();
                switch(field.getType().getTypeId()){
                    case BOOL:
                            rxtxClass = "BitRxTx";
                            break;
                    case BYTE:
                    case CHAR:
                            rxtxClass = "ByteRxTx";
                            break;
                    case INT:
                            rxtxClass = "IntRxTx";
                            break;
                    case WORD:
                            rxtxClass = "WordRxTx";
                            break;
                    case DINT:
                            rxtxClass = "DintRxTx";
                            break;
                    case DWORD:
                            rxtxClass = "DwordRxTx";
                            break;
                    case STRING:
                            rxtxClass = "StringRxTx";
                            break;
                    case UDT:
                            rxtxClass = field.getType().getIdentifier();
                            break;
                    default: fieldTypeSupported = false;
                }
                if (fieldTypeSupported){
                    if (field.isArray()){
                        accessorDeclaration = new String(arrayAccessorTemplate);
                        accessorDeclaration = accessorDeclaration.replace(INDEXLIST, genIndexList(field));
                        accessorDeclaration = accessorDeclaration.replace(ASSERTIONLIST, genAssertionList(field));
                        accessorDeclaration = accessorDeclaration.replace(DIMENSIONLIST, genArrayBrackets(field,true));
                        accessorDeclaration = accessorDeclaration.replace(ACCESSEDDIMENSIONS, genAccessedDimensions(field));
                        accessorDeclaration = accessorDeclaration.replace(DIMSIZECOMPUTATION, genDimSizeComputation(field));
                    }
                    else{
                        accessorDeclaration = new String(simpleAccessorTemplate);
                    }
                    accessorDeclaration = accessorDeclaration.replace(VARIABLETAG, field.getIdentifier());
                    accessorDeclaration = accessorDeclaration.replace(RXTXCLASS, rxtxClass);
                    accessorIdentifier  = field.getIdentifier();
                    char firstChar      = Character.toUpperCase(accessorIdentifier.charAt(0));
                    accessorIdentifier  = "get" + firstChar + accessorIdentifier.substring(1);
                    accessorDeclaration = accessorDeclaration.replace(ACCESSORIDENTIFIER, accessorIdentifier);
                    accessorDeclaration = accessorDeclaration.replace(BYTEOFFSETCOMPUTATION, genByteOffsetComputation(field));
                    accessorDeclaration = accessorDeclaration.replace(OPTIONALBITOFFSET, genOptionalBitOffset(field));
                    accessorDeclaration = accessorDeclaration.replace(OPTIONALDATAOFFSET, genOptionalDataOffset(field));
                    accessorDeclaration = accessorDeclaration.replace(OPTIONALSIZE, genOptionalSize(field, rxtxClass));
                }
                else{
                    accessorDeclaration = "     //not supported yet    " + field.getIdentifier() + ";\n";
                }
                accessorDeclarations += accessorDeclaration;
            }
            return accessorDeclarations;
        }

        private String genArrayBrackets(StructType.Field field, boolean withDimensions){
            StringBuffer brackets = new StringBuffer("");
            if (field.isArray()){
                for (int i = 0; i < field.getArrayDimensions().numberOfDimensions; i++){
                    brackets.append("[" + (withDimensions ? field.getArrayDimensions().dimension[i] : "") +"]");
                }
            }
            return brackets.toString();
        }

        private String genIndexList(StructType.Field field){
            StringBuffer indexList = new StringBuffer("");
            int          numberOfDimensions = field.getArrayDimensions().numberOfDimensions;
            for (int i = 0; i < numberOfDimensions; i++){
                indexList.append("int index" + (i+1) + (i < (numberOfDimensions - 1) ? " ," : ""));
            }
            return indexList.toString();
        }

        private String genAssertionList(StructType.Field field){
            StringBuffer assertionList = new StringBuffer("\n");
            int          numberOfDimensions = field.getArrayDimensions().numberOfDimensions;
            for (int i = 0; i < numberOfDimensions; i++){
                assertionList.append("        assertIndexRange(" + (i+1) +", " + field.getArrayDimensions().firstIndex[i] + ", " + field.getArrayDimensions().lastIndex[i] + ", index" + (i+1) + ");\n");
            }
            assertionList.append("\n");
            return assertionList.toString();
        }

        private String genAccessedDimensions(StructType.Field field){
            StringBuffer accessedDimensions = new StringBuffer("");
            int          numberOfDimensions = field.getArrayDimensions().numberOfDimensions;
            for (int i = 0; i < numberOfDimensions; i++){
                accessedDimensions.append("[index" + (i+1) + " - " + field.getArrayDimensions().firstIndex[i] + ']');
            }
            return accessedDimensions.toString();
        }
        
        private String genIndexedOffset(StructType.Field field){
            StringBuffer indexedOffset = new StringBuffer("");
            int          numberOfDimensions = field.getArrayDimensions().numberOfDimensions;
            switch(field.getType().getTypeId()){
                case BOOL://TODO check offset calculation for multi dimensional arrays
                        //prepare statement for byte offset calculation inside the array
                        if (numberOfDimensions > 1){
                            //prepare statement for byte offset calculation inside the array
                            for (int i = 0; i < numberOfDimensions; i++){
                                indexedOffset.append("(index" + (i+1) + " - " + field.getArrayDimensions().firstIndex[i] + ")" + (i < (numberOfDimensions - 1) ? "*dimSize" + (i+2) + " + " : " / 8"));
                            }
                        }
                        else{
                            //1 dimensional array
                            indexedOffset.append("(index1 - " + field.getArrayDimensions().firstIndex[0] + ") / 8 ");
                        }
                        break;
                case BYTE:
                case CHAR:
                case STRING:
                default:
                        //prepare statement for byte offset calculation inside the array
                        indexedOffset.append("(");
                        for (int i = 0; i < numberOfDimensions; i++){
                            indexedOffset.append("(index" + (i+1) + " - " + field.getArrayDimensions().firstIndex[i] + ")" + (i < (numberOfDimensions - 1) ? "*dimSize" + (i+2) + " + " : ") * " + field.getType().getSize()));
                        }
            }
            return indexedOffset.toString();
        }

        private String genOptionalSize(StructType.Field field, String rxtxClass){
            String optionalSize = "";
            if (field.getType().getTypeId() == SupportedTypes.STRING){
                optionalSize = "            addr.setSize(" + field.getType().getSize() + ");\n";
            }
            else{
                optionalSize = "            addr.setSize(" + rxtxClass + ".getSize());\n";
            }
            return optionalSize;
        }

        private String genDimSizeComputation(StructType.Field field){
            StringBuffer dimSizeComputation = new StringBuffer("");
            int          numberOfDimensions = field.getArrayDimensions().numberOfDimensions;
            int          dimSize;
            if (numberOfDimensions > 1){
                if (field.getType().getTypeId() == SupportedTypes.BOOL)
                    //calculate the number of bytes per dimension
                    dimSize = (field.getArrayDimensions().dimension[numberOfDimensions -1] + 7)/ 8;
                else
                    dimSize = field.getArrayDimensions().dimension[numberOfDimensions -1];
                dimSizeComputation.append("            int dimSize" + (numberOfDimensions) + " = " + dimSize + ";\n");
                for (int i = numberOfDimensions - 1; i > 1; i--){
                    dimSizeComputation.append("            int dimSize" + i + " = dimSize" + (i+1) + " * " + field.getArrayDimensions().dimension[i-1] + ";\n");
                }
            }
            return dimSizeComputation.toString();
        }

        private String genByteOffsetComputation(StructType.Field field){
            StringBuffer byteOffsetComputation = new StringBuffer("            int byteOffset = getAddress().getByteIndex() + " + field.getByteOffset());
            if (field.isArray())
                byteOffsetComputation.append(" + " + genIndexedOffset(field));
            byteOffsetComputation.append(";\n");
            return byteOffsetComputation.toString();
        }

        private String genOptionalBitOffset(StructType.Field field){
            String optionalBitOffset = "";
            if (field.getType().getTypeId() == SupportedTypes.BOOL){
                if (field.isArray()){
                    int numberOfDimensions = field.getArrayDimensions().numberOfDimensions;
                    optionalBitOffset = "            addr.setBitIndex((index" + numberOfDimensions + " - " + field.getArrayDimensions().firstIndex[numberOfDimensions - 1] + ") % 8);\n";
                }
                else{
                    optionalBitOffset = "            addr.setBitIndex(" + field.getBitOffset() + ");\n";
                }
            }
            return optionalBitOffset;
        }

        private String genOptionalDataOffset(StructType.Field field){
            String optionalDataOffset = ", byteOffset";
//            if (field.getType().getTypeId() == SupportedTypes.UDT /*TODO check|| field.getType().getTypeId() == SupportedTypes.STRING*/){
//                optionalDataOffset    = "";
//            }
            return optionalDataOffset;
        }
    }


    String     symListPath;
    String     destinationPath;
    String     projectName;
    String     packageName;
    Variables  variables;

    Header    header;
    Imports   imports;
    ClassBody classBody;
    
    public StructGenerator(String symListPath, String destinationPath, String projectName, String packageName) throws AddressException{
        this.symListPath     = symListPath;
        this.destinationPath = destinationPath;
        this.projectName     = projectName;
        this.packageName     = packageName;

        variables            = new Variables(symListPath);
    }

    public StructGenerator(String destinationPath, String projectName, String packageName, Variables variables) throws AddressException{
        this.symListPath     = null;
        this.destinationPath = destinationPath;
        this.projectName     = projectName;
        this.packageName     = packageName;
        this.variables       = variables;
    }

    public void generateType(StructType type) throws IOException, AddressException{
        //generate a given class only once per run
        if (!generatedClasses.contains(type.getSymbol())){
            //open destination file
            String path = destinationPath + File.separatorChar + packageName.replace('.', File.separatorChar);
            //create nested directory for java sources
            File pathFile = new File(path);
            if (!pathFile.exists())
                pathFile.mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(path + File.separatorChar + type.getIdentifier() + ".java" )));
            //create header
            header = new Header(type);
            //creating import directives
            imports = new Imports();
            classBody = new ClassBody(type);

            header.write(writer);
            imports.write(writer);
            classBody.write(writer);
            writer.close();
            generatedClasses.add(type.getSymbol());
        }
    }

    public void generate(String variableName) throws UnsupportedTypeException, IOException, AddressException{
        //all nested classes will be generated newly
        generatedClasses.clear();
        //generate source file for requested variable
        generateType(variables.get(variableName).getType());
    }
    private final String REVISIONTAG                = "<revision>";
    private final String DATETAG                    = "<date>";
    private final String PROJECTNAMETAG             = "<projectName>";
    private final String VARIABLETAG                = "<structType>";
    private final String PACKAGENAMETAG             = "<packageName>";
    private final String FIELDDECLARATIONS          = "<fieldDeclarations>";
    private final String VARIABLESIZE               = "<variableSize>";
    private final String ACCESSORDECLARATIONS       = "<accessorDeclarations>";
    private final String ACCESSORIDENTIFIER         = "<accessorIdentifier>";
    private final String RXTXCLASS                  = "<RxTxClass>";
//    private final String VARIABLEOFFSET             = "<variableOffset>";
    private final String INDEXLIST                  = "<indexList>";
    private final String DIMENSIONLIST              = "<dimensionList>";
    private final String ASSERTIONLIST              = "<assertionList>";
    private final String ACCESSEDDIMENSIONS         = "<accessedDimensions>";
//    private final String INDEXEDOFFSET              = "<indexedOffset>";
    private final String OPTIONALSIZE               = "<optionalSize>";
    private final String OPTIONALBITOFFSET          = "<optionalBitOffset>";
    private final String OPTIONALDATAOFFSET         = "<optionalDataOffset>";
    private final String DIMSIZECOMPUTATION         = "<dimsizecomputation>";
    private final String BYTEOFFSETCOMPUTATION      = "<byteOffsetComputation>";

    private final String headerTemplate = 
            "/**\n" +
            "* PROJECT   : " + PROJECTNAMETAG  + "\n" +
            "* MODULE    : " + VARIABLETAG  + ".java \n" +
            "* VERSION   : " + REVISIONTAG + "\n" +
            "* DATE      : " + DATETAG + "\n" +
            "* PURPOSE   : implements the S7 data item " + VARIABLETAG  + "\n" +
            "* REMARKS   : this code was automatically generated by the struct generator\n" +
            "*             of the jPAC S7 communication library\n" +
            "* LOG       : $Log$\n" +
            "*\n" +
            "* The jPac S7 communication library is free software: you can redistribute it and/or modify\n" +
            "* it under the terms of the GNU General Public License as published by\n" +
            "* the Free Software Foundation, either version 3 of the License, or\n" +
            "* (at your option) any later version.\n" +
            "*\n" +
            "* The jPac S7 communication library is distributed in the hope that it will be useful,\n" +
            "* but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
            "* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
            "* GNU General Public License for more details.\n" +
            "*\n" +
            "* You should have received a copy of the GNU General Public License\n" +
            "* along with the jPac S7 communication library. If not, see <http://www.gnu.org/licenses/>.\n" +
            "*\n" +
            "*/\n" +
            "\n" +
            "package " + PACKAGENAMETAG + ";\n" +
            "\n";

    private final String importTemplate =
            "import org.jpac.IndexOutOfRangeException;\n" +
            "import org.jpac.plc.AddressException;\n" +
            "import org.jpac.plc.Connection;\n" +
            "import org.jpac.plc.Address;\n" +
            "import org.jpac.plc.Data;\n" +
            "import org.jpac.plc.LobRxTx;\n";

    private final String classBodyTemplate =
            "\n" +
            "public class " + VARIABLETAG +" extends LobRxTx{\n" +
            FIELDDECLARATIONS +
            "\n" +
            "    //constructor for standalone use\n" +
            "    public " + VARIABLETAG +"(Connection conn, Address address) throws IndexOutOfRangeException{\n" +
//            "        super(conn, db, new Data(new byte[getSize()]));\n" +
            "        super(conn, address, 0, null);\n" +
            "        setData(conn.generateDataObject(getSize()));\n" +
            "    }\n" +
            "\n" +
            "    //constructor for use inside a structure\n" +
            "    public " + VARIABLETAG +"(Connection conn, Address address, int dataOffset, Data data) throws IndexOutOfRangeException{\n" +
//            "        super(conn, db, byteOffset, byteOffset, getSize(), data);\n" +
            "        super(conn, address, dataOffset, data);\n" +
            "    }\n" +
            "\n" +
            "    protected void assertIndexRange(int IndexNumber, int setFirstIndex, int setLastIndex, int actualIndex) throws IndexOutOfRangeException{\n" +
            "        if (actualIndex < setFirstIndex || actualIndex > setLastIndex)\n" +
            "            throw new IndexOutOfRangeException(\"expecting [\" + setFirstIndex + \"..\" + setLastIndex + \"] for index\" + IndexNumber + \", found: \" + actualIndex);\n" +
            "    }\n" +
            "\n" +
            "    public static int getSize(){\n" +
            "        return " + VARIABLESIZE +";\n" +
            "    }\n" +
            "\n" +
            ACCESSORDECLARATIONS +
            "}\n";
    
    private final String simpleAccessorTemplate =
            "\n" +
            "    @SuppressWarnings(\"empty-statement\")\n" +
            "    public " + RXTXCLASS + " " + ACCESSORIDENTIFIER + "() throws AddressException, IndexOutOfRangeException{\n" +
            "        if ("+ VARIABLETAG + " == null){\n" +
            BYTEOFFSETCOMPUTATION +
            "            Address addr = null;\n" +
            "            try{addr = (Address)getAddress().clone();}catch(CloneNotSupportedException exc){};\n" +
            "            addr.setByteIndex(byteOffset);\n" +
            OPTIONALBITOFFSET +
            OPTIONALSIZE      +
//            "            " + VARIABLETAG + " = new " + RXTXCLASS + "(getConnection(), getAddress(), byteOffset" + OPTIONALBITOFFSET + OPTIONALDATAOFFSET + OPTIONALSIZE + ", getData());\n" +
            "            " + VARIABLETAG + " = new " + RXTXCLASS + "(getConnection(), addr" + OPTIONALDATAOFFSET + ", getData());\n" +
            "        }\n" +
            "        return " + VARIABLETAG + ";\n" +
            "    }\n";

    private final String arrayAccessorTemplate =
            "\n" +
            "    @SuppressWarnings(\"empty-statement\")\n" +
            "    public " + RXTXCLASS + " " + ACCESSORIDENTIFIER + "(" + INDEXLIST + ") throws AddressException, IndexOutOfRangeException{\n" +
            ASSERTIONLIST +
            "        if ("+ VARIABLETAG + " == null){\n" +
            "            " + VARIABLETAG + " = new " + RXTXCLASS + DIMENSIONLIST + ";\n" +
            "        }\n" +
            "        //check, if the item already has been accessed\n" +
            "        if ("+ VARIABLETAG + ACCESSEDDIMENSIONS + " == null){\n" +
            "            //if not, instantiate a new representation\n" +
            "            //and store it for subsequent uses\n" +
            DIMSIZECOMPUTATION +
            BYTEOFFSETCOMPUTATION +
            "            Address addr = null;\n" +
            "            try{addr = (Address)getAddress().clone();}catch(CloneNotSupportedException exc){};\n" +
            "            addr.setByteIndex(byteOffset);\n" +
            OPTIONALBITOFFSET +
            OPTIONALSIZE      +
            "            "+ VARIABLETAG + ACCESSEDDIMENSIONS + " = new " + RXTXCLASS + "(getConnection(), addr" + OPTIONALDATAOFFSET + ", getData());\n" +
            "        }\n" +
            "        return "+ VARIABLETAG + ACCESSEDDIMENSIONS + ";\n" +
            "    }\n";

}
