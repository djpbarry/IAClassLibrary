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
package net.calm.iaclasslibrary.ImgLib2.Filters;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class ImageInverter {

    public static < T extends BitType> Img< T> invertBinaryImage(final Img< T> input) {
        // create a new net.calm.iaclasslibrary.Image with the same properties
        // note that the input provides the size for the new image as it implements
        // the Interval interface
        Img< T> output = input.factory().create(input);

        // create a cursor for both images
        Cursor< T> cursorInput = input.cursor();
        Cursor< T> cursorOutput = output.cursor();

        // iterate over the input
        while (cursorInput.hasNext()) {
            // move both cursors forward by one pixel
            cursorInput.fwd();
            cursorOutput.fwd();

            // set the value of this pixel of the output image to the same as the input,
            // every Type supports T.set( T type )
            cursorOutput.get().set(invertPixel(cursorInput.get()));
        }

        // return the copy
        return output;
    }

    private static BitType invertPixel(BitType b) {
        return new BitType(!b.get());
    }
}
