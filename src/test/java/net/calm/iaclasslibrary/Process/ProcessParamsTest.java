package net.calm.iaclasslibrary.Process;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessParamsTest {

    private static final String[] LABELS = {"series", "channel", "radius", "enabled", "name"};

    private static ProcessParams params(String... keyValues) {
        Properties props = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            props.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return new ProcessParams(props, LABELS);
    }

    @Test
    public void testGetInt() {
        ProcessParams p = params("series", "5", "channel", "2");
        assertEquals(5, p.getInt(0));
        assertEquals(2, p.getInt(1));
    }

    @Test
    public void testGetIntDefaultOnMissing() {
        ProcessParams p = params();
        assertEquals(7, p.getInt(0, 7));
    }

    @Test
    public void testGetIntDefaultOnEmpty() {
        ProcessParams p = params("series", "");
        assertEquals(7, p.getInt(0, 7));
    }

    @Test
    public void testGetIntThrowsOnMissingWithoutDefault() {
        ProcessParams p = params();
        assertThrows(NumberFormatException.class, () -> p.getInt(0));
    }

    @Test
    public void testGetDouble() {
        ProcessParams p = params("radius", "1.5");
        assertEquals(1.5, p.getDouble(2), 1e-9);
    }

    @Test
    public void testGetDoubleDefault() {
        ProcessParams p = params();
        assertEquals(2.5, p.getDouble(2, 2.5), 1e-9);
    }

    @Test
    public void testGetFloat() {
        ProcessParams p = params("radius", "0.5");
        assertEquals(0.5f, p.getFloat(2), 1e-6);
    }

    @Test
    public void testGetBoolean() {
        ProcessParams p = params("enabled", "true");
        assertTrue(p.getBoolean(3));
    }

    @Test
    public void testGetBooleanFalseOnMissing() {
        ProcessParams p = params();
        assertFalse(p.getBoolean(3));
    }

    @Test
    public void testGetBooleanDefaultOnMissing() {
        ProcessParams p = params();
        assertTrue(p.getBoolean(3, true));
    }

    @Test
    public void testGetString() {
        ProcessParams p = params("name", "abc");
        assertEquals("abc", p.getString(4));
    }

    @Test
    public void testGetStringNullOnMissing() {
        ProcessParams p = params();
        assertNull(p.getString(4));
    }

    @Test
    public void testGetStringDefault() {
        ProcessParams p = params();
        assertEquals("dflt", p.getString(4, "dflt"));
    }

    @Test
    public void testHas() {
        String[] labels = {"a", null, "c"};
        ProcessParams p = new ProcessParams(new Properties(), labels);
        assertTrue(p.has(0));
        assertFalse(p.has(1));
        assertTrue(p.has(2));
        assertFalse(p.has(3));
    }
}
