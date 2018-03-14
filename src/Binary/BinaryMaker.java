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
import ij.process.ImageProcessor;
import ij.process.StackStatistics;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BinaryMaker {

    public static ImageStack makeBinaryStack(ImagePlus stackImp, AutoThresholder.Method method) {
        StackStatistics stats = new StackStatistics(stackImp);
        int[] histogram = stats.histogram;
        int tIndex = new AutoThresholder().getThreshold(method, histogram);
        int threshold = (int) Math.round(stats.min + tIndex * stats.binSize);
        ImageStack stack = stackImp.getImageStack();
        int s = stack.size();
        ImageStack output = new ImageStack(stack.getWidth(), stack.getHeight());
        for (int i = 1; i <= s; i++) {
            ImageProcessor slice = stack.getProcessor(i);
            slice.threshold(threshold);
            output.addSlice(slice.convertToByteProcessor(false));
        }
        return output;
    }
}
