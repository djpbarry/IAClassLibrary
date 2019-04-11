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
package Process.IO;

import Process.RunnableProcess;
import UtilClasses.GenUtils;
import ij.ImageStack;
import java.nio.ByteBuffer;
import loci.formats.ImageReader;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class RunnablePixelLoader extends RunnableProcess {

    private final ImageReader reader;
    private final ImageStack stack;
    private final int byteIndex;
    private final int outSliceIndex;

    public RunnablePixelLoader(ImageReader reader, ImageStack stack, int byteIndex, int outSliceIndex) {
        super(null);
        this.reader = reader;
        this.stack = stack;
        this.byteIndex = byteIndex;
        this.outSliceIndex = outSliceIndex;
    }

    public void run() {
        try {
            int bitDepth = reader.getBitsPerPixel();
            boolean littleEndian = reader.isLittleEndian();
            int width = reader.getSizeX();
            int height = reader.getSizeY();
            int area = width * height;
            byte[] pix = reader.openBytes(byteIndex);
            if (bitDepth == 16) {
                short[] shortPix = new short[area];
                for (int index = 0; index < shortPix.length; index++) {
                    int index2 = 2 * index;
                    byte[] bytePixel;
                    if (!littleEndian) {
                        bytePixel = new byte[]{pix[index2], pix[index2 + 1]};
                    } else {
                        bytePixel = new byte[]{pix[index2 + 1], pix[index2]};
                    }
                    shortPix[index] = ByteBuffer.wrap(bytePixel).getShort();
                }
                stack.setPixels(shortPix, outSliceIndex);
            } else {
                stack.setPixels(pix, outSliceIndex);
            }
        } catch (Exception e) {
            GenUtils.logError(e, "Failed to load image plane.");
        }
    }
}
