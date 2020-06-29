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
package net.calm.iaclasslibrary.Image;

import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class ImageResizer {

    public static FloatProcessor resizeNoScaling(ImageProcessor image, int width, int height) {
        if (image.getWidth() > width || image.getHeight() > height) {
            int x = (image.getWidth() - width) / 2;
            int y = (image.getHeight() - height) / 2;
            image.setRoi(new Rectangle(x, y, width, height));
            image = image.crop();
        }
        FloatProcessor output = new FloatProcessor(width, height);
        output.setValue(0.0);
        output.fill();
        FloatBlitter blitter = new FloatBlitter(output);
        int x = (width - image.getWidth()) / 2;
        int y = (height - image.getHeight()) / 2;
        blitter.copyBits(image, x, y, Blitter.COPY);
        return output;
    }
    
}
