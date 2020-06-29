package net.calm.iaclasslibrary.IAClasses;

import ij.measure.CurveFitter;
import java.util.ArrayList;

/**
 * A collection of static utility methods for the analysis of an object boundary
 * and the calculation of fractal dimension. A full description is provided in
 * the following:<br><br>
 *
 * D. J. Barry, O. C. Ifeyinwa, S. R. McGee, R. Ryan, G. A. Williams, and J. M.
 * Blackledge, “Relating fractal dimension to branching behaviour in filamentous
 * microorganisms,” <i>ISAST T Elec Sig Proc</i>, vol. 1, no. 4, pp. 71–76,
 * 2009.
 *
 * @author David J Barry <davejpbarry@gmail.com>
 * @version 01SEP2010
 */
public class DSPProcessor {

    private static final double NATURAL_LOG_OF_2 = Math.log(2);

    /**
     * Traces the boundary of a polygon (the vertices of which are represented
     * by <i>xPoints</i> and <i>yPoints</i>) and calculates the distance from
     * each point to the polygon's centroid, (<i>xCentre, yCentre</i>).
     *
     * @param n the number of vertices in the polygon
     * @param xCentre the x-coordinate of the polygon centroid
     * @param yCentre the y-coordinate of the centroid
     * @param xPoints the x-coordinates of the polygon vertices
     * @param yPoints the y-coordinates of the vertices
     * @param resolution the spatial resolution of the signal
     * @return an array consisting of the distances from each point on the
     * boundary to the centroid
     */
    public static Pixel[] getDistanceSignal(int n, double xCentre,
            double yCentre, int[] xPoints, int[] yPoints, double resolution) {
        int i, j, k, x1, y1, x2, y2, xdif, ydif, xInc, yInc;
        double xD, yD, current;
        ArrayList<Pixel> dist = new ArrayList<Pixel>();

        for (i = 0, j = 0; j < n; j++) {
            x1 = xPoints[j];
            y1 = yPoints[j];
            if (j < (n - 1)) {
                k = j + 1;
            } else {
                k = j + 1 - n;
            }
            x2 = xPoints[k];
            y2 = yPoints[k];
            xdif = x2 - x1;
            ydif = y2 - y1;

            if (xdif > 0) {
                xInc = 1;
            } else if (xdif < 0) {
                xInc = -1;
            } else {
                xInc = 0;
            }

            if (ydif > 0) {
                yInc = 1;
            } else if (ydif < 0) {
                yInc = -1;
            } else {
                yInc = 0;
            }

            do {
                xD = xCentre - x1;
                yD = yCentre - y1;
                current = resolution * Math.sqrt((xD * xD) + (yD * yD));
                dist.add(new Pixel(x1, y1, current));
                x1 += xInc;
                y1 += yInc;
                i++;
            } while ((x1 != x2) || (y1 != y2));
        }
        Pixel distArray[] = new Pixel[dist.size()];
//        double index[] = new double[dist.size()];
//        double d[] = new double[dist.size()];
        for (int l = 0; l < dist.size(); l++) {
            distArray[l] = ((Pixel) dist.get(l));
//            index[l] = l;
//            d[l] = distArray[l].getZ();
        }
//        Plot plot1 = new Plot("Original", "x", "y", index, d);
//        plot1.show();
        return distArray;
    }

    public static double[][] getInterpolatedDistanceSignal(int n, double xCentre,
            double yCentre, double[] xPoints, double[] yPoints, double resolution) {
        int j, k;
        double xD, yD, rad, arc;
        double dist[][] = new double[2][n];

        for (j = 0; j < n; j++) {
            double x1 = xPoints[j];
            double y1 = yPoints[j];
            if (j < (n - 1)) {
                k = j + 1;
            } else {
                k = j + 1 - n;
            }
            double x2 = xPoints[k];
            double y2 = yPoints[k];
            double xdiff = x2 - x1;
            double ydiff = y2 - y1;
            xD = xCentre - x1;
            yD = yCentre - y1;
            rad = resolution * Math.sqrt((xD * xD) + (yD * yD));
            arc = resolution * Math.sqrt((xdiff * xdiff) + (ydiff * ydiff));
            dist[0][j] = rad;
            dist[1][j] = arc;
        }
        return dist;
    }

    /**
     * Calculates the fractal dimension (<i>D</i>) of an input signal by
     * generating a log-log Fourier domain representation of the signal and
     * estimating <i>D</i> based on a linear regression analysis. For background
     * on this method, consult the following:<br> <br> J. M. Blackledge,
     * "Fractal Images and net.calm.iaclasslibrary.Image Processing," in <i>Digital net.calm.iaclasslibrary.Image
     * Processing</i>, 1st ed. Chichester: Horwood Publishing, 2005.
     *
     * @param inputSignal the signal to be analysed
     * @return the fractal dimension
     */
    public static double[] calcFourierSpec(double[] inputSignal, double sampleRate) {
        int n = inputSignal.length;
        if (n < 10) {
            return null;
        }
        /*
         * Upscale signal to ensure length is a power of 2
         */
        int cutOffIndex = (int) Math.ceil(0.5d * n);
        double fourierTransform[][] = FFT(inputSignal);

        return getPowerSpectrum(fourierTransform, cutOffIndex);
    }

    public static double[] calcFourierDim(double[] inputSignal, double sampleRate, double cutoff) {
        int k; // Input index
        double w; // Frequency component index
        if (inputSignal == null) {
            return null;
        }
        int n = inputSignal.length;
        if (n < 10) {
            return null;
        }
        /*
         * Upscale signal to ensure length is a power of 2
         */
        ArrayList<Double> lof = new ArrayList<Double>();
        ArrayList<Double> lop = new ArrayList<Double>();
        for (k = 1; (k < n) && (w = k * sampleRate / n) < cutoff; k++) {
            lof.add(new Double(Math.log(w)));
            lop.add(new Double(Math.log(inputSignal[k])));
        }
        double logOfFrequency[] = new double[lof.size()];
        double logOfPower[] = new double[lop.size()];
        for (int i = 0; i < logOfFrequency.length; i++) {
            logOfFrequency[i] = ((Double) lof.get(i)).doubleValue();
            logOfPower[i] = ((Double) lop.get(i)).doubleValue();
        }
        CurveFitter fitter = new CurveFitter(logOfFrequency, logOfPower);
        fitter.doFit(CurveFitter.STRAIGHT_LINE); // The fractal dimension is estimated from a linear regression of a log-log Fourier domain plot
        double eqCoeffs[] = fitter.getParams(); // Coefficents of the equation of the straight line fit (ax + by = c).
        double output[] = new double[3];
        output[0] = eqCoeffs[0];
        output[1] = eqCoeffs[1];
        output[2] = fitter.getRSquared();
//        Plot plot3 = new Plot("PS", "log[w]", "log[PS]", logOfFrequency, logOfPower);
//        plot3.show();

        return output;
    }

    public static double[] upScale(double[] input) {
        int n = (int) Math.ceil(Math.log(input.length) / NATURAL_LOG_OF_2);
        return upScale(input, n, true);
    }

    /**
     * Increase the number of samples in the <i>input</i> signal using linear
     * interpolation. The number of samples in the output signal will be a power
     * of 2.
     *
     * @param input signal with <i>N</i> samples
     * @return output signal with <i>2<sup>n</sup></i> samples, where <i>n</i>
     * is the smallest integer value that satisfies <i>2<sup>n</sup> > N</i>
     */
    public static double[] upScale(double[] input, int n, boolean powerOf2) {
        int i, j, k, inputLength = input.length;
        int outputLength = n;

        if (powerOf2) {
            outputLength = (int) Math.pow(2, n);
        }

        double output[] = new double[outputLength];
        double increment = (double) (inputLength - 1.0) / (outputLength - 1.0);
        double interindex = 0.0;

        /*
         * The input signal is linearly interpolated to produce the output
         */
        for (i = 0; i < outputLength; i++) {
            j = (int) Math.floor(interindex);
            if (j < inputLength - 1) {
                k = j + 1;
            } else {
                k = j;
            }
            output[i] = input[j] + (interindex - j) * (input[k] - input[j]);
            interindex += increment;
        }
        return output;
    }

    /**
     * Implementation of the Fast Fourier Transform based on the Radix-2
     * Cooley-Tukey Algorithm. The method is called recursively to divide the
     * input into a series of FFT's with just one term in each. The input signal
     * must be <i>2<sup>n</sup></i> in length, where <i>n</i>
     * is an integer.
     *
     * @param input time/space domain signal
     * @return Fourier-domain signal
     */
    public static double[][] FFT(double[][] input) {
        int k, m, inputLength = input.length;
        double exponent, cosine, sine;
        double fft[][] = new double[inputLength][2];

        if (inputLength > 1) {
            int halfInput = inputLength / 2;
            double evenIndices[][] = new double[halfInput][2];
            double oddIndices[][] = new double[halfInput][2];
            for (m = 0; m < halfInput; m++) {
                evenIndices[m][0] = input[2 * m][0];
                evenIndices[m][1] = input[2 * m][1];
                oddIndices[m][0] = input[(2 * m) + 1][0];
                oddIndices[m][1] = input[(2 * m) + 1][1];
            }
            double evenFFT[][] = FFT(evenIndices);
            double oddFFT[][] = FFT(oddIndices);

            for (k = 0; k < halfInput; k++) {
                exponent = -(2 * Math.PI * k) / inputLength;
                cosine = Math.cos(exponent);
                sine = Math.sin(exponent);
                fft[k][0] = evenFFT[k][0] + cosine * oddFFT[k][0] - sine * oddFFT[k][1];
                fft[k][1] = evenFFT[k][1] + cosine * oddFFT[k][1] + sine * oddFFT[k][0];
            }
            for (k = halfInput; k < inputLength; k++) {
                exponent = -(2 * Math.PI * (k - halfInput)) / inputLength;
                cosine = Math.cos(exponent);
                sine = Math.sin(exponent);
                fft[k][0] = evenFFT[k - halfInput][0] - cosine * oddFFT[k - halfInput][0] + sine * oddFFT[k - halfInput][1];
                fft[k][1] = evenFFT[k - halfInput][1] - cosine * oddFFT[k - halfInput][1] - sine * oddFFT[k - halfInput][0];
            }
        } else {
            fft[0][0] = input[0][0];
            fft[0][1] = input[0][1];
        }
        return fft;
    }

    public static double[][] FFT(double[] input) {
        double complex[][] = new double[input.length][2];
        for (int i = 0; i < input.length; i++) {
            complex[i][0] = input[i];
            complex[i][1] = 0.0;
        }
        return FFT(complex);
    }

    public static double[][] FFT(double[] real, double[] imag) {
        double complex[][] = new double[real.length][2];
        for (int i = 0; i < real.length; i++) {
            complex[i][0] = real[i];
            complex[i][1] = imag[i];
        }
        return FFT(complex);
    }

    public static double[][] IFFT(double[][] input) {
        int k, m, inputLength = input.length;
        double exponent, cosine, sine;
        double ifft[][] = new double[inputLength][2];

        if (inputLength > 1) {
            int halfInput = inputLength / 2;
            double evenIndices[][] = new double[halfInput][2];
            double oddIndices[][] = new double[halfInput][2];
            for (m = 0; m < halfInput; m++) {
                evenIndices[m][0] = input[2 * m][0];
                oddIndices[m][0] = input[(2 * m) + 1][0];
                evenIndices[m][1] = input[2 * m][1];
                oddIndices[m][1] = input[(2 * m) + 1][1];
            }
            double evenFFT[][] = IFFT(evenIndices);
            double oddFFT[][] = IFFT(oddIndices);

            for (k = 0; k < halfInput; k++) {
                exponent = (2 * Math.PI * k) / inputLength;
                cosine = Math.cos(exponent);
                sine = Math.sin(exponent);
                ifft[k][0] = evenFFT[k][0] + cosine * oddFFT[k][0] - sine * oddFFT[k][1];
                ifft[k][1] = evenFFT[k][1] + cosine * oddFFT[k][1] + sine * oddFFT[k][0];
            }
            for (k = halfInput; k < inputLength; k++) {
                exponent = (2 * Math.PI * (k - halfInput)) / inputLength;
                cosine = Math.cos(exponent);
                sine = Math.sin(exponent);
                ifft[k][0] = evenFFT[k - halfInput][0] - cosine * oddFFT[k - halfInput][0] + sine * oddFFT[k - halfInput][1];
                ifft[k][1] = evenFFT[k - halfInput][1] - cosine * oddFFT[k - halfInput][1] - sine * oddFFT[k - halfInput][0];
            }
        } else {
            ifft[0][0] = input[0][0];
            ifft[0][1] = input[0][1];
        }
        return ifft;
    }

    /**
     * Calculates the power spectrum of a given Fourier input sequence.
     *
     * @param fourier complex Fourier-domain signal (<i>F(jk)</i>)
     * @param inputLength length of the Fourier signal
     * @return power spectrum (magnitude; <i>|F(jk)|</i>) of the Fourier signal
     */
    public static double[] getPowerSpectrum(double[][] fourier, int inputLength) {
        int k;
        double[] powerSpectrum = new double[inputLength];
//        double index[] = new double[inputLength];

        for (k = 0; k < inputLength; k++) {
            powerSpectrum[k] = Math.pow(fourier[k][0], 2) + Math.pow(fourier[k][1], 2);
//            index[k] = k;
        }

//        Plot plot = new Plot("PowerSpectrum", "i", "K", index, powerSpectrum);
//        plot.show();
        return powerSpectrum;
    }

    public static short[][] interpolatePoints(int n, int[] xPoints, int[] yPoints) {
        int k, x1, y1, x2, y2, xdif, ydif, xInc, yInc;
        ArrayList<short[]> interList = new ArrayList<short[]>();

        for (int j = 0; j < n; j++) {
            x1 = xPoints[j];
            y1 = yPoints[j];
            if (j < (n - 1)) {
                k = j + 1;
            } else {
                k = j + 1 - n;
            }
            x2 = xPoints[k];
            y2 = yPoints[k];
            xdif = x2 - x1;
            ydif = y2 - y1;

            if (xdif > 0) {
                xInc = 1;
            } else if (xdif < 0) {
                xInc = -1;
            } else {
                xInc = 0;
            }

            if (ydif > 0) {
                yInc = 1;
            } else if (ydif < 0) {
                yInc = -1;
            } else {
                yInc = 0;
            }

            do {
                interList.add(new short[]{(short)x1, (short)y1});
                x1 += xInc;
                y1 += yInc;
            } while ((x1 != x2) || (y1 != y2));
        }
        short interArray[][] = new short[interList.size()][2];
        for (int l = 0; l < interList.size(); l++) {
            interArray[l] = interList.get(l);
        }
        return interArray;
    }

    public static double[][] calcKappa(double[] xinput, double[] yinput, double resolution) {
        double sigma = 2.5 * resolution;
        int gLength = (int) Math.round(12.5 * resolution);
        if (xinput == null || yinput == null || xinput.length < gLength) {
            return null;
        }
//        for (int i = 0; i < xinput.length; i++) {
//            System.out.println(xinput[i] + "\t" + yinput[i]);
//        }
        double gaussian[] = Utils.generateGaussian(sigma, gLength);
        double dgaussian[] = Utils.generateGaussianFirstDeriv(sigma, gLength);
        double ddgaussian[] = Utils.generateGaussianSecondDeriv(sigma, gLength);
        double X[] = Utils.convolveCurve(gaussian, xinput);
        double Y[] = Utils.convolveCurve(gaussian, yinput);
        double dX[] = Utils.convolveCurve(dgaussian, xinput);
        double dY[] = Utils.convolveCurve(dgaussian, yinput);
        double ddX[] = Utils.convolveCurve(ddgaussian, xinput);
        double ddY[] = Utils.convolveCurve(ddgaussian, yinput);
        double shrinkX = (1.0 / ddX[0])
                * (1.0 - Math.exp(-sigma * sigma * ddX[0] * ddX[0] / 2.0));
        double shrinkY = (1.0 / ddY[0])
                * (1.0 - Math.exp(-sigma * sigma * ddY[0] * ddY[0] / 2.0));
        double[][] output = new double[3][X.length];
        for (int i = 0; i < X.length; i++) {
            output[0][i] = X[i] - shrinkX;
            output[1][i] = Y[i] - shrinkY;
            output[2][i] = (dX[i] * ddY[i] - dY[i] * ddX[i])
                    / Math.pow(dX[i] * dX[i] + dY[i] * dY[i], 1.5);
        }
        return output;
    }

    public static double[] smoothSignal(double[] input, double timeRes) {
        double sigma = 0.5 * 2.5 * timeRes;
        int gLength = (int) Math.round(0.5 * 12.5 * timeRes);
        if (input == null || input.length < gLength) {
            return null;
        }
        double gaussian[] = Utils.generateGaussian(sigma, gLength);
        //double ddgaussian[] = Utils.generateGaussianSecondDeriv(sigma, gLength);
        double X[] = Utils.convolveCurve(gaussian, input);
        //double ddX[] = Utils.convolveCurve(ddgaussian, input);
        double[] output = new double[X.length];
        System.arraycopy(X, 0, output, 0, X.length);
//        for (int i = 0; i < X.length; i++) {
//            output[i] = X[i];// + (ddX[i] - X[i]);
//        }
        return output;
    }
}
