package net.calm.iaclasslibrary.IO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataWriterTest {

    @Test
    public void testTransposeValues() {
        assertArrayEquals(new double[][]{{1, 4}, {2, 5}, {3, 6}},
                DataWriter.transposeValues(new double[][]{{1, 2, 3}, {4, 5, 6}}));
    }

    @Test
    public void testConvertArrayToString() {
        assertEquals("seed1-2-3-", DataWriter.convertArrayToString("seed", new Object[]{1, 2, 3}, "-"));
    }

    @Test
    public void testGetAverageValues() {
        assertArrayEquals(new Double[]{2.0, 2.5, 3.5, 4.5},
                DataWriter.getAverageValues(new double[][]{{1, 2, 3}, {4, 5, 6}}, 3));
    }
}
