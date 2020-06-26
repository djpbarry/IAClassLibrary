/*
 * Copyright (C) 2019 David Barry <david.barry at crick dot ac dot uk>
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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class StackMath {

    public static void mutiply(ImagePlus imp, double val) {
        ImageStack inputStack = imp.getImageStack();
        int nSlices = inputStack.getSize();
        ImageStack outputStack = new ImageStack(inputStack.getWidth(), inputStack.getHeight());
        for (int i = 1; i <= nSlices; i++) {
            ImageProcessor slice = inputStack.getProcessor(i);
            slice.multiply(val);
            outputStack.addSlice(slice);
        }
        imp.setStack(outputStack);
    }
}
