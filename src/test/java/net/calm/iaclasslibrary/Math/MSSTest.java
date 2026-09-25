package net.calm.iaclasslibrary.Math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MSSTest {

    @Test
    public void testCalcMSSReturnsNaNForNullInput() {
        assertTrue(Double.isNaN(MSS.calcMSS(null, 1.0, 1.0, 1.0)));
    }
}
