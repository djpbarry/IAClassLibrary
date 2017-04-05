/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Math;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Histogram {

    public static int[] calcHistogram(double[] data, double min, double max, int numBins) {
        final int[] result = new int[numBins];
        final double binSize = (max - min) / numBins;
        for (double d : data) {
            int bin = (int) ((d - min) / binSize);
            if (bin < 0) {
                bin = 0;
            } else if (bin >= numBins) {
                bin = numBins - 1;
            }
            result[bin] += 1;
        }
        return result;
    }
}
