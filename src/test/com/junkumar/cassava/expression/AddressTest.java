package com.junkumar.cassava.expression;

import static org.junit.jupiter.api.Assertions.*;

class AddressTest {

    @org.junit.jupiter.api.Test
    void Address() {
        assertEquals("B2", new Address("2", "B").toString());
        assertEquals("B2", new Address(2,2).toString());

        // reverse row and col
        assertThrows(IllegalArgumentException.class,
                () -> new Address("B", "2"));

        assertEquals(new Address(2,2).hashCode(), new Address("2", "B").hashCode());
        assertTrue(new Address(2,1).equals(new Address("2", "A")));
        assertTrue(new Address(2,1).equals(new Address("A2")));

    }
}