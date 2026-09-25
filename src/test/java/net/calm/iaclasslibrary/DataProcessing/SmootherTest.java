package net.calm.iaclasslibrary.DataProcessing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class SmootherTest {

    @Test
    public void testSmoothDataAppliesMovingAverage() {
        double[][][] input = {{{1}, {2}, {3}, {4}, {5}}};
        double[][][] result = Smoother.smoothData(input, 1, new boolean[]{true});
        assertArrayEquals(new double[][]{{1.5}, {2.0}, {3.0}, {4.0}, {5.0}}, result[0]);
    }
}
