# Mini CSV (comma separated values) Spreadsheet interpreter
Reads in a csv spreadsheet (format described below), evaluates the value of each cell, and outputs values to STDOUT.

A spreadsheet is defined as a two-dimensional array of cells. Columns are identified using letters and rows by
numbers (C2 references a cell in column 3, row 2). Each cell contains either an integer or an expression.
Expressions contain integers, cell references, and operators ('+', '-', '*', '/') and are evaluated with the
usual rules of evaluation.

**Input Format:**
- A csv file with m rows and n columns
- The input file will have no headers
- Cells will not be surrounded in double quotes

**Output Format:**
- A csv file (to stdout) with the same dimensions as the input file
- Each cell is output as a floating point value. Round output values to two decimal places.

**Example:**

_input.csv_
```
B2+2,A1+A2
B2-3,7+5
```

_output.csv_
```
14.00,23.00
9.00,12.00
```


## More specifications
- The spreadsheet should be able to evaluate expressions containing cell references, integers, floating point
  numbers, and the addition and subtraction operators. 
- Support for up to 26 columns (A-Z)
- The spreadsheet should detect circular references and exit appropriately
- Main method in a class named `Spreadsheet.java`
- Empty cell contents will be treated as null values and be output as empty strings.
- Negative numbers supported.



# Design
- The overall design is based on the Gang-Of-Four Interpreter design pattern.
- Factory methods are used to create and manage Expressions and are in the context class.
- The implementation emphasizes simplicity and hence does not implement a full lexer and parser.
- They are jointly implemented via regexes which are sufficient for the 4 binary operators desired.
- The primary SpreadSheetContext class is the "Context" class in the Interpreter pattern.
- The Expression class hierarchy is designed per the Composite design pattern to support easy extensibility



# Solution
- Expressions are the fundamental units stored in cells in the spreadsheet.
- Some Expressions are simple terminal ones that contain ints/floats/references to other cells/Expressions.
- Other Expressions are complex and are currently limited to Binary operations.

- SpreadsheetContext objects will hold all the actual data slurped from the csv.\

- Spreadsheet cells are stored in a HashMap. The unevaluated text representation is stored as is.
- The cells (Expressions) are evaluated on demand when required for activities such as printing.
- This postponed/lazy evaluation will allow for the construction a more general spreadsheet editor over time.
