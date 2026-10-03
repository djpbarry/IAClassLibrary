package net.calm.iaclasslibrary.Math.Optimisation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterisation test for the Nelder-Mead fitting path
 * ({@link Fitter#doFit()} via {@link IsoGaussianFitter}), pinning that a clean
 * synthetic 2-D Gaussian is recovered before the optimiser is reimplemented on
 * top of Commons Math3's {@code SimplexOptimizer}.
 */
public class IsoGaussianFitterTest {

    @Test
    public void testDoFitRecoversSyntheticGaussian() {
        int n = 16;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i;
            y[i] = i;
        }

        double background = 10.0;
        double magnitude = 100.0;
        double x0 = 7.0;
        double y0 = 8.0;
        double sigma = 2.0;

        double[][] z = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double dx = (x[i] - x0) / sigma;
                double dy = (y[j] - y0) / sigma;
                z[i][j] = background + magnitude * Math.exp(-0.5 * (dx * dx + dy * dy));
            }
        }

        IsoGaussianFitter fitter = new IsoGaussianFitter(x, y, z, true, sigma);
        assertTrue(fitter.doFit());
        assertEquals(5, fitter.getNumParams());
        assertEquals(magnitude, fitter.getMag(), magnitude * 0.1);
        assertEquals(x0, fitter.getX0(), 0.5);
        assertEquals(y0, fitter.getY0(), 0.5);
        assertEquals(sigma, fitter.getXsig(), sigma * 0.3);
    }
}
