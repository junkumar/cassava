package com.junkumar.cassava.expression;

import com.junkumar.cassava.Expression;
import com.junkumar.cassava.SpreadsheetContext;

/**
 * Null cells exist so they can be placeholders. They cannot be evaluated.
 * When printed they show up as blank strings.
 */
public class NullLeaf implements Expression {

    public NullLeaf() {}

    @Override
    public float eval(SpreadsheetContext context) {
        throw new IllegalArgumentException("Null cells cannot be evaluated. Bad reference?");
    }

    @Override
    public String toString() {
        return "";
    }

    /**
     * Allows special case for null terminal expressions where blanks are printed out.
     * For non-terminal cases where NullLeaf is a part of an expression the evaluation will fail.
     */
    @Override
    public String evaluatedString(SpreadsheetContext context, String format) {
        return "";
    }
}