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
package ImgLib2.SimpleConverters;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class SimpleConverter {

    public static RandomAccessibleInterval<BitType> convertUnsignedByteToBit(final RandomAccessibleInterval<UnsignedByteType> image) {

        return Converters.convert(image, new Converter<UnsignedByteType, BitType>() {
            public void convert(UnsignedByteType arg0, BitType arg1) {
                arg1.set(arg0.get() > 0);
            }
        }, new BitType());
    }

    public static RandomAccessibleInterval<UnsignedByteType> convertBitToUnsignedByte(final RandomAccessibleInterval<BitType> image) {

        return Converters.convert(image, new Converter<BitType, UnsignedByteType>() {
            public void convert(BitType arg0, UnsignedByteType arg1) {
                arg1.set(arg0.get() ? new UnsignedByteType((byte) 255) : new UnsignedByteType((byte) 0));
            }
        }, new UnsignedByteType());
    }

    public static <T extends UnsignedByteType, U extends BitType> Img<T> convertBitToUnsignedByte(final Img<U> image) {
        Img< T> output = (new ArrayImgFactory(new UnsignedByteType())).create(image);

        // create a cursor for both images
        Cursor< U> cursorInput = image.cursor();
        Cursor< T> cursorOutput = output.cursor();

        // iterate over the input
        while (cursorInput.hasNext()) {
            // move both cursors forward by one pixel
            cursorInput.fwd();
            cursorOutput.fwd();

            // set the value of this pixel of the output image to the same as the input,
            // every Type supports T.set( T type )
            cursorOutput.get().set(cursorInput.get().get() ? new UnsignedByteType((byte) 255) : new UnsignedByteType((byte) 0));
        }

        // return the copy
        return output;
    }
}
