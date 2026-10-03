package net.calm.iaclasslibrary.IAClasses;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilsTest {

    @Test
    public void testCalcEuclidDistMatchesCommonsMath() {
        double[] a = {1.0, 2.0, 3.0};
        double[] b = {4.0, 6.0, 8.0};
        assertEquals(new EuclideanDistance().compute(a, b), Utils.calcEuclidDist(a, b), 1e-12);
    }

    @Test
    public void testCalcEuclidDistReturnsNaNOnNull() {
        assertTrue(Double.isNaN(Utils.calcEuclidDist(null, new double[]{1.0})));
    }

    @Test
    public void testGenerateGaussianMatchesNormalDensity() {
        double sigma = 2.0;
        int length = 9;
        NormalDistribution normal = new NormalDistribution(0.0, sigma);
        double[] actual = Utils.generateGaussian(sigma, length);
        int i0 = (length - 1) / 2;
        for (int i = 0; i < length; i++) {
            assertEquals(normal.density(i - i0), actual[i], 1e-12);
        }
    }

    @Test
    public void testArcTanCompassQuadrants() {
        assertEquals(0.0, Utils.arcTan(1.0, 0.0), 1e-12);
        assertEquals(90.0, Utils.arcTan(0.0, -1.0), 1e-12);
        assertEquals(180.0, Utils.arcTan(-1.0, 0.0), 1e-12);
        assertEquals(270.0, Utils.arcTan(0.0, 1.0), 1e-12);
        assertEquals(315.0, Utils.arcTan(1.0, 1.0), 1e-12);
        assertEquals(45.0, Utils.arcTan(1.0, -1.0), 1e-12);
        assertEquals(225.0, Utils.arcTan(-1.0, 1.0), 1e-12);
        assertEquals(135.0, Utils.arcTan(-1.0, -1.0), 1e-12);
    }
}
