package net.calm.iaclasslibrary.IAClasses;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Characterisation tests for {@link DSPProcessor#FFT(double[])} /
 * {@link DSPProcessor#IFFT(double[][])}, pinning the current unscaled,
 * natural-order complex DFT behaviour before it is reimplemented on top of
 * Commons Math3.
 */
public class DSPProcessorFFTTest {

    private static final double TOL = 1e-9;

    @Test
    public void testFFTMatchesCommonsMathForward() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[][] actual = DSPProcessor.FFT(signal);
        Complex[] expected = new FastFourierTransformer(DftNormalization.STANDARD)
                .transform(signal, TransformType.FORWARD);
        assertComplexEquals(expected, actual, TOL);
    }

    @Test
    public void testIFFTMatchesCommonsMathInverseUnscaled() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[][] spectrum = DSPProcessor.FFT(signal);
        double[][] actual = DSPProcessor.IFFT(spectrum);
        Complex[] expected = new FastFourierTransformer(DftNormalization.STANDARD)
                .transform(toComplex(spectrum), TransformType.INVERSE);
        // DSPProcessor.IFFT is unscaled, while Commons Math's STANDARD inverse
        // applies a 1/N factor, so multiply the Math3 result back by N.
        int n = signal.length;
        for (int i = 0; i < n; i++) {
            assertEquals(expected[i].getReal() * n, actual[i][0], TOL * n);
            assertEquals(expected[i].getImaginary() * n, actual[i][1], TOL * n);
        }
    }

    @Test
    public void testFFTRoundTripIsUnscaled() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[][] spectrum = DSPProcessor.FFT(signal);
        double[][] recovered = DSPProcessor.IFFT(spectrum);
        int n = signal.length;
        for (int i = 0; i < n; i++) {
            assertEquals(signal[i] * n, recovered[i][0], TOL * n);
            assertEquals(0.0, recovered[i][1], TOL * n);
        }
    }

    private static Complex[] toComplex(double[][] pairs) {
        Complex[] out = new Complex[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            out[i] = new Complex(pairs[i][0], pairs[i][1]);
        }
        return out;
    }

    private static void assertComplexEquals(Complex[] expected, double[][] actual, double tol) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].getReal(), actual[i][0], tol);
            assertEquals(expected[i].getImaginary(), actual[i][1], tol);
        }
    }
}
