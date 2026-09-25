package net.calm.iaclasslibrary.DataProcessing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class InterpolatorTest {

    @Test
    public void testInterpolateLinearly() {
        double[][][] data = {{{0, 0}, {2, 20}}};
        boolean[] keys = {false, true};
        double[][][] result = Interpolator.interpolateLinearly(data, 0, keys);
        assertArrayEquals(new double[][]{{0, 0}, {0, 10}, {2, 20}}, result[0]);
    }
}
