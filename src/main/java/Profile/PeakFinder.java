/*
 * Copyright (C) 2017 Dave Barry <david.barry at crick.ac.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package Profile;

import IAClasses.Utils;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.util.Arrays;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class PeakFinder {

    public static ImageProcessor alignProfileToRefImage(FloatProcessor inputImage, double radius, FloatProcessor refImage) {
        ImageProcessor blurredRef = refImage.duplicate();
        (new GaussianBlur()).blurGaussian(blurredRef, radius);
        int[] max = Utils.findImageMaxima(blurredRef);
        int xc = blurredRef.getWidth() / 2;
        inputImage.translate(xc - max[0], 0);
        return inputImage;
    }

    public static int[] findMaxAndSides(double[] data) {
        DescriptiveStatistics stats = new DescriptiveStatistics(data);
        double max = stats.getMax();
        double min = stats.getMin();
        double[] thresholds = new double[]{0.5 * (max - min) + min, max, 0.8 * (max - min) + min};
        boolean[] ops = new boolean[]{true, true, false};
        return findIndices(data, ops, thresholds);
    }

    public static int[] findRegionWidth(double[] data, double thresh, double min, double max) {
        double[] thresholds = new double[]{thresh * (max - min) + min, thresh * (max - min) + min};
        boolean[] ops = new boolean[]{true, false};
        return findIndices(data, ops, thresholds);
    }

    private static int[] findIndices(double[] data, boolean[] ops, double[] thresholds) {
        int[] indices = new int[ops.length];
        Arrays.fill(indices, -1);
        int j = 0;
        for (int i = 0; i < data.length && j < ops.length; i++) {
            if ((ops[j] && data[i] >= thresholds[j]) || (!ops[j] && data[i] < thresholds[j])) {
                indices[j++] = i;
            }
        }
        return indices;
    }

    public static double[][] smoothData(double[][] data, double radius) {
        double[][] smoothedData = new double[data.length][];
        for (int i = 0; i < data.length; i++) {
            smoothedData[i] = smoothData(data[i], radius);
        }
        return smoothedData;
    }

    public static double[][] smoothData2D(double[][] data, double radius) {
        int mapWidth = 0;
        for (double[] d : data) {
            if (d.length > mapWidth) {
                mapWidth = d.length;
            }
        }
        FloatProcessor fp = new FloatProcessor(mapWidth, data.length);
        float[] floatPix = new float[data.length * mapWidth];
        for (int y = 0; y < data.length; y++) {
            int offset = y * mapWidth;
            for (int x = 0; x < mapWidth; x++) {
                floatPix[x + offset] = (float) data[y][x];
            }
        }
        fp.setPixels(floatPix);
        (new GaussianBlur()).blurGaussian(fp, radius);
        floatPix = (float[]) fp.getPixels();
        double[][] doublePix = new double[data.length][mapWidth];
        for (int y = 0; y < data.length; y++) {
            int offset = y * mapWidth;
            for (int x = 0; x < mapWidth; x++) {
                doublePix[y][x] = floatPix[x + offset];
            }
        }
        return doublePix;
    }

    public static double[] smoothData(double[] data, double radius) {
        FloatProcessor fp = new FloatProcessor(data.length, 1);
        float[] floatPix = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            floatPix[i] = (float) data[i];
        }
        fp.setPixels(floatPix);
        (new GaussianBlur()).blurGaussian(fp, radius);
        floatPix = (float[]) fp.getPixels();
        double[] doublePix = new double[floatPix.length];
        for (int i = 0; i < data.length; i++) {
            doublePix[i] = floatPix[i];
        }
        return doublePix;
    }

    public static double[] findMinAndMax(double[][] data) {
        double[] extrema = {Double.MAX_VALUE, Double.MIN_VALUE};
        for (double[] d : data) {
            DescriptiveStatistics stats = new DescriptiveStatistics(d);
            double max = stats.getMax();
            double min = stats.getMin();
            if (max > extrema[1]) {
                extrema[1] = max;
            }
            if (min < extrema[0]) {
                extrema[0] = min;
            }
        }
        return extrema;
    }

}
