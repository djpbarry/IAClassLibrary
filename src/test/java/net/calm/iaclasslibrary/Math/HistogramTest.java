package net.calm.iaclasslibrary.Math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class HistogramTest {

    @Test
    public void testCalcHistogramBasic() {
        int[] result = Histogram.calcHistogram(new double[]{1, 2, 3}, 0, 4, 4);
        assertArrayEquals(new int[]{0, 1, 1, 1}, result);
    }

    @Test
    public void testCalcHistogramClampsOutOfRangeValues() {
        int[] result = Histogram.calcHistogram(new double[]{-5, 10}, 0, 4, 4);
        assertArrayEquals(new int[]{1, 0, 0, 1}, result);
    }
}
