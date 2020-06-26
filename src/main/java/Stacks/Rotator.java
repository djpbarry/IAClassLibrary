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
package Stacks;

import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Rotator {

    public static ImageStack rotateRight(ImageStack input) {
        ImageStack output = new ImageStack(input.getHeight(), input.getWidth());
        int n = input.getSize();
        for (int i = 0; i < n; i++) {
            ImageProcessor slice = input.getProcessor(i + 1).duplicate();
            output.addSlice(rotateSliceRight(slice));
        }
        return output;
    }

    public static FloatProcessor rotateSliceRight(ImageProcessor input) {
        int width = input.getWidth();
        int height = input.getHeight();
        FloatProcessor output = new FloatProcessor(height, width);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output.putPixelValue(height - 1 - y, x, input.getPixelValue(x, y));
            }
        }
        return output;
    }
    
    public static FloatProcessor rotateSliceLeft(ImageProcessor input) {
        int width = input.getWidth();
        int height = input.getHeight();
        FloatProcessor output = new FloatProcessor(height, width);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output.putPixelValue(y, x, input.getPixelValue(x, y));
            }
        }
        return output;
    }
}
