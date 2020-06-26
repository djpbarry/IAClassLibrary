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
package ImageProcessing;

import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class ImageBlurrer {

    public static void blurStack(ImageStack stack, double radius) {
        int s = stack.size();
        for (int i = 1; i <= s; i++) {
            ImageProcessor slice = stack.getProcessor(i);
            (new GaussianBlur()).blurGaussian(slice, radius);
        }
    }
}
