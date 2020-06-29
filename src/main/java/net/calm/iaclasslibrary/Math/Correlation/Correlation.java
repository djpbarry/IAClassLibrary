/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.calm.iaclasslibrary.Math.Correlation;

import net.calm.iaclasslibrary.Math.Rand;
import ij.process.ImageProcessor;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class Correlation implements Runnable {

    public static final int PEARSONS = 1;
    public static final int SPEARMANS = 2;
    private ImageProcessor image1;
    private ImageProcessor image2;
    private ImageProcessor[] output;
    private int[] coords;
    private int options;

    public Correlation(ImageProcessor image1, ImageProcessor image2, ImageProcessor[] output, int[] coords, int options) {
        this.image1 = image1;
        this.image2 = image2;
        this.output = output;
        this.coords = coords;
        this.options = options;
    }

    public static double spearman(double[] array1, double[] array2) {
        return (new SpearmansCorrelation()).correlation(array1, array2);
    }

    public static double pearsons(double[] array1, double[] array2) {
        return (new PearsonsCorrelation()).correlation(array1, array2);
    }

    public static double[] randSpearman(double[] array1, double[] array2, double lp, double up, int N) {
        SpearmansCorrelation spearman = new SpearmansCorrelation();
        int m = array1.length, n = array2.length;
        double[] array3 = new double[m];
        double[] array4 = new double[n];
        System.arraycopy(array1, 0, array3, 0, m);
        System.arraycopy(array2, 0, array4, 0, n);
        double[] randCoeffs = new double[N];
        for (int i = 0; i < N; i++) {
            Rand.bootstrap(array3, array4);
            randCoeffs[i] = spearman.correlation(array3, array4);
        }
        Percentile p = new Percentile();
        return new double[]{p.evaluate(randCoeffs, lp), p.evaluate(randCoeffs, up)};
    }

    public static double[] imageCorrelation(ImageProcessor image1, ImageProcessor image2, int options) {
        float[] pix1 = (float[]) image1.convertToFloatProcessor().getPixels();
        float[] pix2 = (float[]) image2.convertToFloatProcessor().getPixels();

        if (pix1.length != pix2.length) {
            return null;
        }

        double[] dpix1 = new double[pix1.length];
        double[] dpix2 = new double[pix2.length];

        for (int i = 0; i < pix1.length; i++) {
            dpix1[i] = pix1[i];
            dpix2[i] = pix2[i];
        }

        double[] result = new double[]{Double.NaN, Double.NaN};
        if ((options & PEARSONS) > 0) {
            result[0] = pearsons(dpix1, dpix2);
        }
        if ((options & SPEARMANS) > 0) {
            result[1] = pearsons(dpix1, dpix2);
        }
        return result;
    }

    public void run() {
        float[] pix1 = (float[]) image1.convertToFloatProcessor().getPixels();
        float[] pix2 = (float[]) image2.convertToFloatProcessor().getPixels();

        if (pix1.length != pix2.length) {
            return;
        }

        double[] dpix1 = new double[pix1.length];
        double[] dpix2 = new double[pix2.length];

        for (int i = 0; i < pix1.length; i++) {
            dpix1[i] = pix1[i];
            dpix2[i] = pix2[i];
        }

        if ((options & PEARSONS) > 0) {
            output[0].putPixelValue(coords[0], coords[1], pearsons(dpix1, dpix2));
        }
        if ((options & SPEARMANS) > 0) {
            output[1].putPixelValue(coords[0], coords[1], spearman(dpix1, dpix2));
        }
    }
}
