package com.junkumar.cassava;

import com.junkumar.cassava.expression.Address;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class SpreadsheetContextTest {
    private SpreadsheetContext nullContext, testContext;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        nullContext = new SpreadsheetContext();

        String csv = "";
        testContext = new SpreadsheetContext(new BufferedReader(new StringReader(csv)));
        testContext
                .put("A1", "1")
                .put("A2", "2.0")
                .put("B1", "1+2")
                .put("B2", "B1+1")
                .put("C1", "B2*A1")
                .put("C2", "B1 - A1 - A2")
                .put("D1", "-9")
                .put("D2", "A1-5")
        ;
    }

    @org.junit.jupiter.api.Test
    void parseTerminal() {
        Expression e = SpreadsheetContext.parseTerminal("1");
        assertEquals(e.getClass().toString(), "class com.junkumar.cassava.expression.IntegerLeaf");
        assertEquals("1.00", e.toString());
        assertEquals(1, e.eval(null));

        assertTrue("B".matches("^[A-Z]$"));

        e = SpreadsheetContext.parseTerminal("B2");
        assertEquals(e.getClass().toString(), "class com.junkumar.cassava.expression.Address");

        e = SpreadsheetContext.parseTerminal("1.10000000003204343");
        assertEquals(e.getClass().toString(), "class com.junkumar.cassava.expression.FloatLeaf");
        assertEquals("1.10", e.toString());
        assertEquals(1000000 * 1.1,
                Math.round(1000000 * e.eval(null)));

        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseTerminal("junk"));

        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseTerminal("1*3"));

        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseTerminal("b2"));

        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseTerminal("BB2"));

        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseTerminal("1. 9"));


        assertEquals("", SpreadsheetContext.parseTerminal("").toString());
        assertEquals("", SpreadsheetContext.parseTerminal("    ").toString());
        // constructing ok but not eval for ""
        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseTerminal("").eval(nullContext));

        //noinspection ConstantConditions,ConstantConditions
        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseTerminal(null));

        //test whitespace leading/trailing
        assertEquals(0, SpreadsheetContext.parseTerminal(" 0   ").eval(nullContext));
        assertEquals("B232", SpreadsheetContext.parseTerminal("   B232  ").toString());
        assertEquals(1000000 * 0.9, Math.round(1000000 * SpreadsheetContext.parseTerminal(" 0.9 ").eval(nullContext)));

    }

    @org.junit.jupiter.api.Test
    void parseBinary() {
        //noinspection ConstantConditions
        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseBinary(null, "+"));

        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseBinary("", ""));

        Expression e = SpreadsheetContext.parseBinary("1+2", "+");
        assertEquals(3, e.eval(null));

        e = SpreadsheetContext.parseBinary("10-2", "-");
        assertEquals(8, e.eval(null));
        e = SpreadsheetContext.parse("10-2");
        assertEquals(8, e.eval(null));


        assertTrue("a+c".matches(".+\\" + "+" + ".+"));
        assertTrue("a+c".matches(".*\\+.*"));

//        // Figuring out java regex oddities.
//        Pattern binaryOp = Pattern.compile("\\" + "+");
//        String[] pair = binaryOp.split("B1+3", 2);
//        System.out.println(pair[0] + "," + pair[1]);

        float f = testContext.get(new Address("C1"));
        assertEquals(4, f);


        e = SpreadsheetContext.parseBinary("1+2+3", "+");
        assertEquals(6, e.eval(null));

        e = SpreadsheetContext.parseBinary("1+2*3", "+");
        assertEquals(7, e.eval(null));

        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseBinary("1@2", ""));

        assertThrows(IllegalArgumentException.class,
                () -> SpreadsheetContext.parseBinary("1 ", ""));

    }

    @org.junit.jupiter.api.Test
    void printStream() {
        try {
            StringWriter sw = new StringWriter();
            testContext.printStream(sw, testContext::getStrict);

            testContext.put("E1", "");
            testContext.put("E2", "A2");
            testContext.put("F1", "A1*A2*D1");
            testContext.put("F2", "A2");

            StringWriter sw2 = new StringWriter();
            assertThrows(RuntimeException.class,
                    () -> testContext.printStream(sw2, testContext::getStrict));

            StringWriter sw3 = new StringWriter();
            testContext.printStream(sw3, testContext::getFormatted);
            // System.out.print(sw3.toString());

            // try reading a bad file in
            final String bad = sw3.toString() + "sdfdssdf ";

            assertThrows(IllegalArgumentException.class,
                    () -> new SpreadsheetContext(new BufferedReader(new StringReader(bad))));

            final String badWithExtraCommas = sw3.toString().replaceAll("9.00", "3.00,4.00");
            // System.out.print(badWithExtraCommas);
            assertThrows(IllegalArgumentException.class,
                    () -> new SpreadsheetContext(new BufferedReader(new StringReader(badWithExtraCommas))));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @org.junit.jupiter.api.Test
    void orderingAndLeftAssociativity() {
//        // Figuring out java regex oddities.
//.
//        Pattern binaryOp = Pattern.compile("\\" + "-");
//        String[] pair = binaryOp.split("10-4-2", 2);
//        System.out.println(pair[0] + "," + pair[1]);
//
//        binaryOp = Pattern.compile("\\" + "-");
//        String[] set = binaryOp.split("10-4-2");
//
//        String last = set[set.length-1];
//        String[] rest = Arrays.copyOfRange(set, 0, set.length-1);
//        String restString = String.join("-", rest);
//        System.out.println(restString + "," + last);

        Expression e = SpreadsheetContext.parse("10-2-1");
        assertEquals(7, e.eval(null));

        e = SpreadsheetContext.parse("10-2-1+10-3");
        assertEquals(14, e.eval(null));

        e = SpreadsheetContext.parse("10-2-1+10*8-3");
        assertEquals(84, e.eval(null));

        e = SpreadsheetContext.parse("40/4/2");
        assertEquals(5, e.eval(null));

        e = SpreadsheetContext.parse("64*3/2*8");
        assertEquals(768, e.eval(null));

        e = SpreadsheetContext.parse("10-2-1+10*8-3/2*9/3");
        assertEquals(82.5, e.eval(null));

        float f = testContext.get(new Address("C2"));
        assertEquals(0, f);

        e = SpreadsheetContext.parse("10-2-20");
        assertEquals(-12, e.eval(null));
    }

    @org.junit.jupiter.api.Test
    void negatives() {
        assertEquals("-9.00", testContext.getFormatted(new Address("D1")));
        assertEquals("-4.00", testContext.getFormatted(new Address("D2")));
    }

    @org.junit.jupiter.api.Test
    void nullTerminals() {
        testContext.put("E1", "");
        testContext.put("E2", "E1");
        assertEquals("", testContext.getFormatted(new Address("E1")));
        assertThrows(IllegalArgumentException.class,
                () -> testContext.getFormatted(new Address("E2")));

        // make non null and try reference resolution
        testContext.put("E1", "1*A1");
        assertEquals(1, testContext.get(new Address("E2")));
    }

    @org.junit.jupiter.api.Test
    void badRefs() {
        testContext.put("E1", "E2");
        testContext.put("E2", "E1");

        assertThrows(IllegalStateException.class,
                () -> testContext.getFormatted(new Address("E2")));

        // multi-way circular
        testContext.put("E1", "F2*3");
        testContext.put("E2", "F1");
        testContext.put("F1", "E1*3");
        testContext.put("F2", "F1");

        assertThrows(IllegalStateException.class,
                () -> testContext.getFormatted(new Address("E1")));

        assertThrows(IllegalStateException.class,
                () -> testContext.getFormatted(new Address("E2")));
    }
}
