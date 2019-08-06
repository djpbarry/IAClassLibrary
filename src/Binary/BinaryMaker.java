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
package Binary;

import Process.Segmentation.MultiThreadedWatershed;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import ij.process.StackStatistics;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BinaryMaker {

    /**
     * Generates a binary stack from an input stack
     *
     * @param stackImp The input stack
     * @param method The thresholding method to employ
     * @param manualThresh The manual threshold to use, if required
     * @param holeSize The approximate size of holes to be filled
     * post-thresholding
     * @return
     */
    public static ImageStack makeBinaryStack(ImagePlus stackImp, String method, int manualThresh, int holeSize) {
        int threshold = manualThresh;
        if (method != null) {
            StackStatistics stats = new StackStatistics(stackImp);
            int[] histogram = stats.histogram;
            int tIndex = new AutoThresholder().getThreshold(method, histogram);
            threshold = (int) Math.round(stats.min + tIndex * stats.binSize);
        }
        ImageStack stack = stackImp.getImageStack();
        int s = stack.size();
        ImageStack output = new ImageStack(stack.getWidth(), stack.getHeight());
        for (int i = 1; i <= s; i++) {
            output.addSlice(makeBinaryImage(stack.getProcessor(i), null, threshold, holeSize));
        }
        return output;
    }

    /**
     * Generates a binary image from an input pixels
     *
     * @param pix Input image pixels
     * @param width Input image width
     * @param height Input image height
     * @param method Thresholding method
     * @param manualThresh A manual threshold value
     * @param holeSize Approximate size of holes to be filled in mask object
     * @return A binary, thresholded image
     */
    public static ByteProcessor makeBinaryImage(short[] pix, int width, int height, String method, int manualThresh, int holeSize) {
        ShortProcessor ip = new ShortProcessor(width, height);
        ip.setPixels(pix);
        return makeBinaryImage(ip.duplicate(), method, manualThresh, holeSize);
    }

    /**
     * Generates a binary image from an input image
     *
     * @param ip Input image
     * @param method Thresholding method to employ
     * @param manualThresh A manual threshold value
     * @param holeSize Approximate size of holes to be filled in mask object
     * @return A binary, thresholded image
     */
    public static ByteProcessor makeBinaryImage(ImageProcessor ip, String method, int manualThresh, int holeSize) {
        int threshold = manualThresh;
        if (method != null) {
            threshold = getThreshold(ip.getStatistics(), method);
        }
        ip.threshold(threshold);
        ByteProcessor output = ip.convertToByteProcessor(false);
        fill(output, 255, 0);
        output.invert();
        for (int j = 0; j < holeSize; j++) {
            output.dilate();
        }
        for (int j = 0; j < 2 * holeSize; j++) {
            output.erode();
        }
        for (int j = 0; j < holeSize; j++) {
            output.dilate();
        }
        output.invert();
        return output;
    }

    /*
        Copied from ij.plugin.filter.Binary
     */
    static void fill(ImageProcessor ip, int foreground, int background) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloodFiller ff = new FloodFiller(ip);
        ip.setColor(127);
        for (int y = 0; y < height; y++) {
            if (ip.getPixel(0, y) == background) {
                ff.fill(0, y);
            }
            if (ip.getPixel(width - 1, y) == background) {
                ff.fill(width - 1, y);
            }
        }
        for (int x = 0; x < width; x++) {
            if (ip.getPixel(x, 0) == background) {
                ff.fill(x, 0);
            }
            if (ip.getPixel(x, height - 1) == background) {
                ff.fill(x, height - 1);
            }
        }
        byte[] pixels = (byte[]) ip.getPixels();
        int n = width * height;
        for (int i = 0; i < n; i++) {
            if (pixels[i] == 127) {
                pixels[i] = (byte) background;
            } else {
                pixels[i] = (byte) foreground;
            }
        }
    }

    /**
     * Calculates a grey level threshold based on the specified method
     *
     * @param stats Image statistics for the image of interest
     * @param method The tresholding method to employ
     * @return A grey level threshold
     */
    static int getThreshold(ImageStatistics stats, String method) {
        int tIndex = new AutoThresholder().getThreshold(method, stats.histogram);
        return (int) Math.round(stats.min + tIndex * stats.binSize);
    }

    public static boolean checkIfBinary(ImagePlus imp) {
        ImageProcessor ip = imp.getProcessor();
        ImageStatistics stats = ip.getStatistics();
        if (!(ip instanceof ByteProcessor) || (stats.histogram[0] + stats.histogram[255] != stats.pixelCount)) {
            return false;
        } else {
            return true;
        }
    }

    public static int getThreshold(ImagePlus imp, AutoThresholder.Method method) {
        StackStatistics stats = new StackStatistics(imp);
        int tIndex = (new AutoThresholder()).getThreshold(method, stats.histogram);
        return (int) Math.round(stats.histMin + stats.binSize * tIndex);
    }
}
