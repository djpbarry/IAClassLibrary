package net.calm.iaclasslibrary.Segmentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegionGrowerTest {

    @Test
    public void testCalcDistance() {
        int width = 4;
        float[] gradPix = new float[width * width];
        gradPix[0] = 1.0f;
        gradPix[5] = 4.0f; // pixel (1, 1)
        double lambda = 2.0;
        short[] p1 = {0, 0};
        short[] p2 = {1, 1};
        float expected = (float) ((Math.pow(4.0 - 1.0, 2.0) + lambda) / (1.0 + lambda));
        assertEquals(expected, RegionGrower.calcDistance(p1, p2, gradPix, lambda, width), 1e-6f);
    }

    @Test
    public void testGetMinCellAreaNullReturnsZero() {
        assertEquals(0.0, RegionGrower.getMinCellArea(null), 1e-12);
    }
}
