package ch.bubendorf.spam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SymbolTest {

    @Test
    public void ctor() {
        final Symbol s1 = new Symbol("");
        assertEquals("", s1.getName());
        assertTrue(Double.isNaN(s1.getScore()));
        assertNull(s1.getDesc());
        assertEquals("", s1.toString());
        assertEquals("", s1.getShortForm());

        final Symbol s2 = new Symbol("MANY_INVISIBLE_PARTS(0.10)[2]");
        assertEquals("MANY_INVISIBLE_PARTS", s2.getName());
        assertEquals(0.1, s2.getScore());
        assertEquals("2", s2.getDesc());
        assertEquals("MANY_INVISIBLE_PARTS (0.1)[2]", s2.toString());
        assertEquals("MANY_INVISIBLE_PARTS (0.1)", s2.getShortForm());

        final Symbol s2a = new Symbol("MANY_INVISIBLE_PARTS (0.10)[2, Foo]");
        assertEquals("MANY_INVISIBLE_PARTS", s2a.getName());
        assertEquals(0.1, s2a.getScore());
        assertEquals("2, Foo", s2a.getDesc());
        assertEquals("MANY_INVISIBLE_PARTS (0.1)[2, Foo]", s2a.toString());
        assertEquals("MANY_INVISIBLE_PARTS (0.1)", s2a.getShortForm());

        final Symbol s3 = new Symbol("TO_DN_NONE (-1.00)");
        assertEquals("TO_DN_NONE", s3.getName());
        assertEquals(-1, s3.getScore());
        assertNull(s3.getDesc());
        assertEquals("TO_DN_NONE (-1.0)", s3.toString());
        assertEquals("TO_DN_NONE (-1.0)", s3.getShortForm());

        final Symbol s4 = new Symbol("XM_UA_NO_VERSION");
        assertEquals("XM_UA_NO_VERSION", s4.getName());
        assertTrue(Double.isNaN(s4.getScore()));
        assertNull(s4.getDesc());
        assertEquals("XM_UA_NO_VERSION", s4.toString());
        assertEquals("XM_UA_NO_VERSION", s4.getShortForm());
    }

    /*@Test
    public void getFormatted() {
        final Symbol s2 = new Symbol("R_SPF_ALLOW (-0.20)[+ip4:45.86.116.0/22:c]");
        assertEquals("R_SPF_ALLOW (-0.2)[+ip4:45.86.116.0/22]", s2.getFormatted());
    }*/
}
