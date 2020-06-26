/*
 * Copyright (C) 2018 David Barry <david.barry at crick dot ac dot uk>
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
package Extrema;

import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MaximaFinder {

    public static int[] findImageMaxima(ImageProcessor image) {
        if (image == null) {
            return null;
        }
        int[] thismax = {-1, -1};
        //        double max = image.getStatistics().max;
        double max = 0.0;
        int width = image.getWidth();
        int height = image.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double pix = image.getPixelValue(x, y);
                if (pix > max) {
                    thismax[0] = x;
                    thismax[1] = y;
                    max = pix;
                }
            }
        }
        return thismax;
    }

    public static ByteProcessor findLocalMaxima(int kWidth, int kHeight, int drawValue, ImageProcessor image, double maxThresh, boolean varyBG, int buffer) {
        if (image == null) {
            return null;
        }
        int i;
        int j;
        int x;
        int y;
        int width = image.getWidth();
        int height = image.getHeight();
        double max;
        double current;
        double min;
        ByteProcessor bProc = new ByteProcessor(width, height);
        bProc.setValue(drawValue);
        int x0 = kWidth + buffer;
        int x1 = width - kWidth - buffer;
        int y0 = kHeight + buffer;
        int y1 = height - kHeight - buffer;
        for (x = x0; x < x1; x++) {
            for (y = y0; y < y1; y++) {
                for (min = Double.MAX_VALUE, max = 0.0, i = x - kWidth; i <= x + kWidth; i++) {
                    for (j = y - kHeight; j <= y + kHeight; j++) {
                        current = image.getPixelValue(i, j);
                        if ((current > max) && !((x == i) && (y == j))) {
                            max = current;
                        }
                        if ((current < min) && !((x == i) && (y == j))) {
                            min = current;
                        }
                    }
                }
                double pix = image.getPixelValue(x, y);
                double diff;
                if (varyBG) {
                    diff = pix - min;
                } else {
                    diff = pix;
                }
                if ((pix > max) && (diff > maxThresh)) {
                    bProc.drawPixel(x, y);
                }
            }
        }
        if (drawValue > 0.0) {
            Prefs.blackBackground = false;
        }
        EDM edm = new EDM();
        ByteProcessor bProc2 = (ByteProcessor) bProc.duplicate();
        bProc2.invert();
        edm.setup("points", new ImagePlus("", bProc2));
        edm.run(bProc2);
        bProc2.multiply(drawValue);
        return bProc2;
    }

    public static ArrayList<int[]> findLocalMaxima(int kWidth, ImageProcessor image, float maxThresh, boolean varyBG, boolean absolute) {
        ImageStack stack = new ImageStack(image.getWidth(), image.getHeight());
        stack.addSlice(image);
        return findLocalMaxima(kWidth, stack, maxThresh, varyBG, absolute, kWidth);
    }

    @Deprecated
    public static ArrayList<int[]> findLocalMaxima(int xyRadius, ImageStack stack, float maxThresh, boolean varyBG, boolean absolute, int zRadius) {
        MultiThreadedMaximaFinder mf = new MultiThreadedMaximaFinder(new double[]{xyRadius, xyRadius, zRadius}, maxThresh);
        mf.start();
        return mf.getMaxima();
    }

    @Deprecated
    public static ImagePlus makeLocalMaximaImage(int xyRadius, ImagePlus imp, float maxThresh, boolean varyBG, boolean absolute, int zRadius, byte background) {
        MultiThreadedMaximaFinder mf = new MultiThreadedMaximaFinder(new double[]{xyRadius, xyRadius, zRadius}, maxThresh);
        mf.start();
        return mf.makeLocalMaximaImage(background, xyRadius);
    }

    /**
     * Checks whether the current position is a local maximum.
     *
     * @param x X-Coordinate
     * @param y Y-Coordinate
     * @param kWidth Search radius in X-dimension
     * @param kHeight Search radius in Y-dimension
     * @param pix Image pixels
     * @param varyBG Take into consideration local background variation?
     * @param maxThresh The minimum intensity permissable for a maxima
     * @param width The width of the image
     * @return true if (x, y) is a local maximum, false otherwise
     */
    public static boolean isLocalMax(int x, int y, int kWidth, int kHeight, short[] pix, boolean varyBG, double maxThresh, int width) {
        double min = Double.MAX_VALUE, max = 0.0;
        for (int j = y - kHeight; j <= y + kHeight; j++) {
            int jOffset = j * width;
            for (int i = x - kWidth; i <= x + kWidth; i++) {
                double current = pix[jOffset + i];
                if ((current > max) && !((x == i) && (y == j))) {
                    max = current;
                }
                if ((current < min) && !((x == i) && (y == j))) {
                    min = current;
                }
            }
        }
        double p = pix[y * width + x];
        double diff;
        if (varyBG) {
            diff = p - min;
        } else {
            diff = p;
        }
        if ((p >= max) && (diff > maxThresh)) {
            return true;
        }
        return false;
    }

}
