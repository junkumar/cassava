package com.junkumar.cassava.expression;

import com.junkumar.cassava.Expression;
import com.junkumar.cassava.SpreadsheetContext;

public class MultiplyOperation implements Expression {
    private final Expression lhs;
    private final Expression rhs;

    public MultiplyOperation(Expression l, Expression r) {
        lhs = l;
        rhs = r;
    }

    @Override
    public float eval(SpreadsheetContext context) {
        return lhs.eval(context) * rhs.eval(context);
    }

    @Override
    public String toString() {
        return lhs.toString() + " * " + rhs.toString();
    }
}
