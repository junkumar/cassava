package com.junkumar.cassava;

/**
 * Every cell in the spreadsheet contains an expression.
 * There various classes implementing this interface all represent a different
 * type of expression.
 *
 * The implementation here is a Composite Design Pattern with the creation of
 * Expressions being handled via Factory Methods in the SpreadsheetContext class.
 */
public interface Expression {
    /**
     * @param context will be used to dereference cell addresses
     * @return final numeric value of Expression after recursive evaluation
     */
    float eval(SpreadsheetContext context);

    /**
     * @return infix notation (useful for debugging)
     */
    String toString();

    /**
     * @param context rest of spreadsheet
     * @param format output format for string returned. eg- %.2f
     * @return A string representation for the result of evaluation.
     *
     * This is provided as a separate function beyond eval() because there are cases
     * where different behavior is desirable than calling eval() recursively followed by
     * stringification. Expressions desiring that should override this function.
     *
     * As an example, NullLeaf Expressions can be printed as blanks but
     * cannot be evaluated when part of a more complex expression.
     * The need is to get different behavior based on the Expression type
     * which is achieved through this method.
     */
    default String evaluatedString(SpreadsheetContext context, String format) {
        float f = eval(context);
        return String.format(format, f);
    }
}
