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
package Thresholding;

import ij.IJ;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import ij.process.AutoThresholder.Method;
import ij.process.ImageStatistics;
import java.io.File;
import java.util.ArrayList;

public class FuzzyThresholder {

    private Method threshMethod = Method.Intermodes;
    private double tolerance;
    private ImageProcessor ip;
    private final int FOREGROUND = 0, BACKGROUND=255, FUZZY=150;

//    public static void main(String args[]) {
//        ImageProcessor ip = IJ.openImage().getProcessor();
//        (new FuzzyThresholder(ip, Method.Otsu, 0.1)).threshold();
//        System.exit(0);
//    }

    public FuzzyThresholder() {

    }

    public FuzzyThresholder(ImageProcessor image, Method method, double tolerance) {
        this.ip = image;
        this.threshMethod = method;
        this.tolerance = tolerance;
    }

    public ImageProcessor threshold() {
        int width = ip.getWidth();
        int height = ip.getHeight();
        ImageStatistics stats = ip.getStatistics();
        double binSize = stats.binSize;
        int t = (int) Math.round((new AutoThresholder()).getThreshold(threshMethod, ip.getHistogram(256)) * binSize + stats.histMin);
        int t1 = (int) Math.round(t * (1.0 + tolerance));
        int t2 = (int) Math.round(t * (1.0 - tolerance));
        ImageProcessor binary = ip.duplicate();
        binary.threshold(t2);
        binary.setValue(FOREGROUND);
        ImageProcessor fuzzyBinary = binary.duplicate();
        fuzzyBinary.setValue(FOREGROUND);
        IJ.saveAs(new ImagePlus("", binary), "PNG", String.format("C:/Users/barryd/Debugging/fuzzy_threshold_debug%s%s", File.separator, "binary"));
        boolean finished = false;
        while (!finished) {
            finished = true;
            ArrayList<int[]> newPoints = new ArrayList();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (binary.getPixel(x, y) == BACKGROUND && ip.getPixelValue(x, y) <= t1 && searchNeighbourhood(binary, FOREGROUND, x, y)) {
                        fuzzyBinary.drawPixel(x, y);
                        finished = false;
                        newPoints.add(new int[]{x, y});
                    }
                }
            }
            if (!finished) {
                for (int[] p : newPoints) {
                    binary.drawPixel(p[0], p[1]);
                }
            }
        }
        IJ.saveAs(new ImagePlus("", fuzzyBinary), "PNG", String.format("C:/Users/barryd/Debugging/fuzzy_threshold_debug%s%s", File.separator, "fuzzyBinary"));
        return fuzzyBinary;
    }

    boolean searchNeighbourhood(ImageProcessor image, int value, int x, int y) {
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (!(x == i && y == j) && image.getPixel(i, j) == value) {
                    return true;
                }
            }
        }
        return false;
    }
}
