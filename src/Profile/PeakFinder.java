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

    public static int[] findMaxAndSides(double[] data, double radius) {
        DescriptiveStatistics stats = new DescriptiveStatistics(data);
        double max = stats.getMax();
        int[] indices = new int[]{-1, -1, -1};
        double[] thresholds = new double[]{0.5 * max, max, 0.75 * max};
        boolean[] ops = new boolean[]{true, true, false};
        int j = 0;
        for (int i = 0; i < data.length && j < 3; i++) {
            if ((ops[j] && data[i] >= thresholds[j]) || (!ops[j] && data[i] <= thresholds[j])) {
                indices[j++] = i;
            }
        }
        return indices;
    }

}
