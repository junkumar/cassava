package com.junkumar.cassava.expression;

import com.junkumar.cassava.Expression;
import com.junkumar.cassava.SpreadsheetContext;

public class IntegerLeaf implements Expression {
    private final int value;

    public IntegerLeaf(int i) {
        value = i;
    }

    @Override
    public float eval(SpreadsheetContext context) {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%.2f", 1.0*value);
    }
}
