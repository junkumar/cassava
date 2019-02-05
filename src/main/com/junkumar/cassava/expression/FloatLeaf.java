package com.junkumar.cassava.expression;

import com.junkumar.cassava.Expression;
import com.junkumar.cassava.SpreadsheetContext;

public class FloatLeaf implements Expression {
    private final float value;

    public FloatLeaf(float f) {
        value = f;
    }

    @Override
    public float eval(SpreadsheetContext context) {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%.2f", value);
    }
}
