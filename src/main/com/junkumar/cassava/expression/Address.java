package com.junkumar.cassava.expression;


import com.junkumar.cassava.Expression;
import com.junkumar.cassava.SpreadsheetContext;

/**
 * A reference to a different cell within spreadsheet
 * Uses a string representation for cell address in the spreadsheet
 * Is usable even without a valid context, but can only be evaluated within a valid spreadsheet context.
 * This behavior will allow working more easily with multiple spreadsheets with potential features.
 */
public class Address implements Expression {
    private final String row, column; // immutable once created

    public Address(String r, String c) {
        row = r;
        column = c;
        if (!isValid())
            throw new IllegalArgumentException("Cannot create address with these arguments - " + r + "," + c + "\n");
    }

    public Address(int r, int c) {
        row = String.format("%d", r);
        int asciiValue = 'A' - 1 + c;
        column = String.format("%c", (char) asciiValue); // should be between 'A' and 'Z'
        if (!isValid())
            throw new IllegalArgumentException("Cannot create address with these arguments - " + r + "," + c + "\n");
    }

    public Address(String cellAddressPair) {
        column = String.valueOf(cellAddressPair.charAt(0)); //single upper case column name matched
        row = cellAddressPair.substring(1); //any number of rows
        if (!isValid())
            throw new IllegalArgumentException("Cannot create address with these arguments - " + cellAddressPair + "\n");
    }

    @Override
    public float eval(SpreadsheetContext context) throws IllegalArgumentException, IllegalStateException {
        assertIsValid();
        // An address is only useful in context.
        if(context == null ||
                rowNumber() > context.rowCount() ||
                columnNumber() > context.columnCount()) {
            throw new IllegalArgumentException("Bad address with this spreadsheet in context. Either null sheet or address that is outside of spreadsheet");
        }
        // self-references don't make sense
        if(context.contents(this) == this) {
            throw new IllegalArgumentException("Self referring cell address - " + row+column + "\n");
        }
        //circular reference check will throw a IllegalStateException
        context.checkCircularReference(this);
        return context.contents(this).eval(context);
    }

    @Override
    public String toString() {
        assertIsValid();
        return column + row;
    }

    private void assertIsValid() {
        if (!isValid())
            throw new IllegalArgumentException("Bad address with these co-ordinates - " + row + "," + column);
    }

    private boolean isValid() {
        return (row != null && row.matches("^\\d+$") && //pure integer
                column != null && column.matches("^[A-Z]$")); //single upper case alpha
    }

    public boolean equals(Address address) {
        return address.toString().equals(this.toString());
    }

    // required due to equals override
    @Override
    public int hashCode() {
        return rowNumber() * columnNumber();
    }

    public int rowNumber() {
        return Integer.parseInt(row);
    }

    public int columnNumber() {
        return (int) column.charAt(0) - 'A' + 1;
    }
}
