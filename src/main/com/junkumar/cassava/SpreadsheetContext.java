package com.junkumar.cassava;

import com.junkumar.cassava.expression.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * SpreadsheetContext objects will hold all the actual data slurped from the csv.
 *
 * Spreadsheet cells are stored in a HashMap. The unevaluated text representation is stored as is.
 * The cells (Expressions) are evaluated on demand when required for activities such as printing.
 * This postponed/lazy evaluation will allow for the construction a more general spreadsheet editor over time.
 *
 * Expressions are the fundamental units stored in cells in the spreadsheet.
 * Some Expressions are simple terminal ones that contain ints/floats/references to other cells/Expressions.
 * Other Expressions are complex and are currently limited to Binary operations.
 * Expression is designed per the Composite pattern.
 *
 * The overall design is based on the Gang-Of-Four Interpreter design pattern.
 * Factory methods are used to create and manage Expressions and are in the context class.
 *
 * The implementation emphasizes simplicity and hence does not implement a full lexer and parser.
 * They are jointly implemented via regexes which are sufficient for the 4 binary operators desired.
 *
 * This primary class is the "Context" class in the Interpreter pattern.
 */
public class SpreadsheetContext {
    /**
     * Stored representation within the "cells" HashMap member will only be interpreted at runtime.
     * Evaluation is delayed to the runtime that would use the underlying value which could be unsafe/inconsistent.
     */
    private final HashMap<String, Expression> cells;
    private final String delimiter = ",";
    private int rowCount, columnCount;
    private final AddressReferenceResolver addressReferenceResolver;

    // package-private for testing
    public SpreadsheetContext() {
        rowCount = columnCount = 0;
        cells = new HashMap<>();
        addressReferenceResolver = new AddressReferenceResolver();
    }

    public SpreadsheetContext(BufferedReader reader) {
        this();
        try {
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                String[] list = line.split(delimiter);
                if (row == 1) { // first row seen, register number of columns
                    columnCount = list.length;
                    if (columnCount > 26) // hard coded for single alpha column names
                        throw new IllegalArgumentException("No more than 26 columns allowed due to cell naming being constrained from 'A' to 'Z'");
                } else {
                    if (columnCount != list.length)
                        throw new IllegalArgumentException("Inconsistent number of columns - " + columnCount + " expected");
                }
                int column = 1;
                for (String l: list) {
                    Address a = new Address(row, column);
                    put(a, l);
                    column++;
                }
                row++;
            }
            rowCount = row-1; //final number of rows in file
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.print("Bad File during file reads");
            System.exit(2);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.print("Bad IO during file reads");
            System.exit(2);
        }
    }


    public int rowCount() { return rowCount; }
    public int columnCount() { return columnCount; }

    // Factory method(s) for Expression creation (hence static)
    // package-private for testing.
    //
    // For simplicity's sake this is both a lexer and a parser and operates in a single phase.
    // This is possible due to the limited number of operations supported.
    // This also means this is not very extensible and will need a rewrite to add more operators.
    //
    // Operator evaluation for expressions will proceed in the "P E M D A S" order - * / + -
    // All 4 operators are left-associative. This implies that we can implement a
    // simpler uniform parser using regular expressions rather a full-blown recursive descent algorithm
    //
    static Expression parse(String cellString) {
        // check for allowed characters - A-Z, 0-9, . etc.. will be delegated to the binary/unary parsers

        // the lower priority +- operators need to be checked before */
        if (cellString.matches(".+(\\+|\\-)" + ".+")) {
            String op;
            if (cellString.matches(".+\\" + "+" + ".+"))
                op = "+";
            else
                op = "-";
            return parseBinary(cellString, op);
        }

        if (cellString.matches(".+(\\*|\\/)" + ".+")) {
            String op;

            if (cellString.matches(".+\\" + "*" + ".+"))
                op = "*";
            else
                op = "/";
            return parseBinary(cellString, op);
        }

        // If code gets here, this implies cellString had none of the binary operators
        return parseTerminal(cellString);
    }

    // Factory method(s) for Expression creation (hence static)
    // package-private for testing.
    static Expression parseBinary(String cellString, String operator) {
        if (null == cellString || "".equals(cellString)) throw new IllegalArgumentException("string parsed for binary expr cannot be null or blank");
        if (null == operator || "".equals(operator)) throw new IllegalArgumentException("Operator for binary expr cannot be null or blank");

        cellString = cellString.stripLeading().stripTrailing();
        operator = operator.stripLeading().stripTrailing();

        Pattern binaryOp = Pattern.compile("\\" + operator);
        String[] allMatches = binaryOp.split(cellString);
        String lastMatch = allMatches[allMatches.length-1];
        String[] rest = Arrays.copyOfRange(allMatches, 0, allMatches.length-1);
        String restOfString = String.join(operator, rest);

        if (allMatches.length == 0) {
            return null;
        }
        if (allMatches.length == 1) {
            throw new IllegalArgumentException("Malformed cellString - " + cellString);
        }

        switch (operator) {
            case "-":
                return new SubtractOperation(parse(restOfString), parse(lastMatch));
            case "+":
                return new AddOperation(parse(restOfString), parse(lastMatch));
            case "*":
                return new MultiplyOperation(parse(restOfString), parse(lastMatch));
            case "/":
                return new DivideOperation(parse(restOfString), parse(lastMatch));
            default:
                System.err.println("Unsupported Operator - " + operator);
                throw new IllegalArgumentException();
        }
    }

    // Factory method(s) for Expression creation (hence static)
    // package-private for testing.
    static Expression parseTerminal(String cellString) {
        if (null == cellString) throw new IllegalArgumentException("string parsed for unary expr cannot be null");

        // Support the common typo of having leading and trailing whitespace in cells
        // This would only make sense in a numeric spreadsheet like ours that does not support raw strings.
        // This also means that a csv cell with a bunch of spaces will be stored and interpreted as "" - NullLeaf
        cellString = cellString.stripLeading().stripTrailing();

        if (Pattern.matches("(\\-)*\\d+", cellString)) {
            int i = Integer.parseInt(cellString);
            return new IntegerLeaf(i);
        } else if(Pattern.matches("(\\-)*\\d*\\.\\d+", cellString)) {
            float f = Float.parseFloat(cellString);
            return new FloatLeaf(f);
        } else if(Pattern.matches("^[A-Z]\\d+", cellString)) {
            return new Address(cellString);
        } else if(Objects.equals(cellString, "")) {
            return new NullLeaf();
        } else {
            throw new IllegalArgumentException("Unsupported Terminal value - " + cellString);
        }
    }

    public Expression contents(Address address) {
        return cells.get(address.toString());
    }

    /**
     * @param address - unsafe address within sheet. caller should supply valid address
     */
    float get(Address address) throws IllegalArgumentException {
        Expression e = contents(address);
        addressReferenceResolver.clear();
        return e.eval(this);
    }

    /**
     * @param address - unsafe address within sheet.Caller should supply valid address
     *
     * This is the version of get() that will fully evaluate expressions before stringifying.
     * This implies nulls will trigger exceptions
     */
    String getStrict(Address address, String format) throws IllegalArgumentException {
        float v = get(address);
        return String.format(format, v);
    }

    /**
     * @param address - unsafe address within sheet. caller should supply valid address
     *
     * Use default formatting of float with 2 digits. eg- 123.xx
     */
    String getStrict(Address address) throws IllegalArgumentException {
        return getStrict(address, "%.2f");
    }


    /**
     * @param address - unsafe address within sheet. caller should supply valid address
     *
     * This is the version of get() that will selectively evaluate nulls to blank strings
     * only when the mulls are terminal.
     */
    String getFormatted(Address address, String format) throws IllegalArgumentException {
        Expression e = contents(address);
        addressReferenceResolver.clear();
        return e.evaluatedString(this, format);
    }

    /**
     * @param address - unsafe address within sheet. caller should supply valid address
     *
     * Use default formatting of float with 2 digits. eg- 123.xx
     */
    String getFormatted(Address address) throws IllegalArgumentException {
        return getFormatted(address, "%.2f");
    }

    /**
     * @param address - unsafe address within sheet. caller should supply valid address
     * @param str - stored representation which only be interpreted at runtime.
     *            Potential evaluation is unsafe/inconsistent.
     * @return build for chaining
     */
    SpreadsheetContext put(Address address, String str) {
        cells.put(address.toString(), parse(str));
        if ( address.rowNumber() > rowCount() ) {
            rowCount = address.rowNumber();
        }
        if (address.columnNumber() > columnCount() ) {
            columnCount = address.columnNumber();
        }
        return this;
    }

    SpreadsheetContext put(String address, String str) {
        Address a = new Address(address);
        return put(a, str);
    }

    /**
     * @param writer - Could be a file
     * @param getMethodReference - one of the get methods which implement desired
     *                          behavior for Expression evaluation/stringification.
     * @throws RuntimeException - for bad cells that cannot be evaluated
     *
     * This is a strict version that will only work if all cells can be
     * 1. evaluated correctly
     * 2. do not have bad references - circular or self references
     */
    public void printStream(Writer writer, Function<Address, String> getMethodReference) throws RuntimeException, IOException {
        for (int row = 1; row <= rowCount(); row++) {
            for (int col = 1; col <= columnCount(); col++) {
                Address a = new Address(row, col);
                try {
                    String placeholder = getMethodReference.apply(a);
                    writer.write(placeholder);
                } catch (Exception e) {
                    // send it forward
                    throw new RuntimeException("Cannot print cells that cannot be evaluated correctly", e);
                }
                if (col != columnCount()) {
                    writer.write(delimiter);
                }
            }
            // try to work across mac and windows
            writer.write(System.getProperty("line.separator"));
        }
        writer.flush();
    }

    // This will manage the state associated with reference resolution.
    // State needs to be reset every time a new "root" evaluation will be
    // started to recursively evaluate Expressions.
    // Only Address Expressions will access this state.
    private class AddressReferenceResolver {
        private Address evalRoot;
        private final Set<Address> refsSoFar;

        AddressReferenceResolver() {
            evalRoot = null;
            refsSoFar = new HashSet<>();
        }

        // Must clear every time a new resolution starts which will always be from here.
        void clear() {
            evalRoot = null;
            refsSoFar.clear();
        }

        boolean addWithCheck(Address a) throws IllegalStateException {
            if (evalRoot == null) {
                evalRoot = a;
                return refsSoFar.add(evalRoot);
            } else if (!refsSoFar.contains(a)) {
                return refsSoFar.add(a);
            } else {
                // circular reference detected
                StringBuilder chain = new StringBuilder();
                for (Address ref : refsSoFar) {
                    chain.append(ref.toString());
                    chain.append(" ");
                }
                throw new IllegalStateException("Address " + a.toString() + " has a circular reference.\n" +
                        "Reference chain is [" + chain + "]");
            }
        }
    }

    public boolean checkCircularReference(Address current) { return addressReferenceResolver.addWithCheck(current); }
}
