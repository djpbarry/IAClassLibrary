package net.calm.iaclasslibrary.Process.DistanceTransform;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class RiemannianDistanceTransformTest {

    @Test
    public void testDistanceTransformSampledMatchesNaive() {
        Random r = new Random(42);
        for (int trial = 0; trial < 50; trial++) {
            int n = 1 + r.nextInt(64);
            double[] f = new double[n];
            double[] g = new double[n];
            double gv = 0.0;
            for (int i = 0; i < n; i++) {
                f[i] = r.nextDouble() * 100.0;
                gv += 1.0 + r.nextDouble() * 2.0;
                g[i] = gv;
            }
            double w = 0.5 + r.nextDouble() * 3.0;
            double[] fast = RiemannianDistanceTransform.distanceTransformSampled(f, g, w);
            double[] naive = naiveDistanceTransformSampled(f, g, w);
            assertArrayEquals(naive, fast, 1e-6);
        }
    }

    @Test
    public void testNearestForegroundDistanceMatchesNaive() {
        Random r = new Random(7);
        for (int trial = 0; trial < 50; trial++) {
            int n = 1 + r.nextInt(64);
            double[] g = new double[n];
            short[] mask = new short[n];
            double gv = 0.0;
            for (int i = 0; i < n; i++) {
                gv += 1.0 + r.nextDouble() * 2.0;
                g[i] = gv;
                mask[i] = (short) (r.nextInt(3) == 0 ? 255 : 0);
            }
            double w = 0.5 + r.nextDouble() * 3.0;
            double[] fast = RiemannianDistanceTransform.nearestForegroundDistance(g, mask, w, (short) 0);
            double[] naive = naiveNearestForegroundDistance(g, mask, w, (short) 0);
            assertArrayEquals(naive, fast, 1e-6);
        }
    }

    private static double[] naiveDistanceTransformSampled(double[] f, double[] g, double w) {
        int n = f.length;
        double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            double min = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                double dx = g[j] - g[i];
                double test = f[j] + w * dx * dx;
                if (test < min) {
                    min = test;
                }
            }
            d[i] = min;
        }
        return d;
    }

    private static double[] naiveNearestForegroundDistance(double[] g, short[] mask, double w, short background) {
        int n = g.length;
        double[] d = new double[n];
        for (int i = 0; i < n; i++) {
            double min = Float.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (mask[j] != background) {
                    double dx = g[j] - g[i];
                    double test = w * dx * dx;
                    if (test < min) {
                        min = test;
                    }
                }
            }
            d[i] = min;
        }
        return d;
    }
}
