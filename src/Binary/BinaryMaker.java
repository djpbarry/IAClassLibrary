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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.AutoThresholder;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BinaryMaker {

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
            ImageProcessor slice = stack.getProcessor(i);
            slice.threshold(threshold);
            slice = slice.convertToByteProcessor(false);
            fill(slice, 255, 0);
            slice.invert();
            for (int j = 0; j < holeSize; j++) {
                slice.dilate();
            }
            for (int j = 0; j < 2 * holeSize; j++) {
                slice.erode();
            }
            for (int j = 0; j < holeSize; j++) {
                slice.dilate();
            }
            slice.invert();
            output.addSlice(slice);
        }
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
}
