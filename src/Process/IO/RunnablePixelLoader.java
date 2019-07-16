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
import loci.formats.IFormatReader;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class RunnablePixelLoader extends RunnableProcess {

    private final IFormatReader reader;
    private final ImageStack stack;
    private final int[] limits;
    private final int thread;
    private final int nThreads;
    private final int[] incs;

    public RunnablePixelLoader(IFormatReader reader, ImageStack stack, int[] limits, int thread, int nThreads, int[] incs) {
        super(null);
        this.reader = reader;
        this.stack = stack;
        this.limits = limits;
        this.thread = thread;
        this.nThreads = nThreads;
        this.incs = incs;
    }

    public void run() {
        int bitDepth = reader.getBitsPerPixel();
        boolean littleEndian = reader.isLittleEndian();
        int width = reader.getSizeX();
        int height = reader.getSizeY();
        int area = width * height;
        int k0 = incs[0] > 1 ? limits[6] + thread : limits[6];
        int j0 = incs[1] > 1 ? limits[3] + thread : limits[3];
        int i0 = incs[2] > 1 ? limits[0] + thread : limits[0];
        try {
            for (int k = k0; k < limits[7]; k += incs[0]) {
                int kOffset = k * limits[5] * limits[2];
                for (int j = j0; j < limits[4]; j += incs[1]) {
                    int jOffset = j * limits[2];
                    for (int i = i0; i < limits[1]; i += incs[2]) {
                        int outSliceIndex = 1 + (k - limits[6]) * (limits[1] - limits[0]) * (limits[4] - limits[3])
                                + (j - limits[3]) * (limits[1] - limits[0])
                                + (i - limits[0]);
                        byte[] pix = new byte[area * bitDepth / 8];
                        reader.openBytes(kOffset + jOffset + i, pix);
                        if (bitDepth == 32) {
                            float[] floatPix = new float[area];
                            for (int index = 0; index < floatPix.length; index++) {
                                int index4 = 4 * index;
                                byte[] bytePixel;
                                if (!littleEndian) {
                                    bytePixel = new byte[]{pix[index4], pix[index4 + 1], pix[index4 + 2], pix[index4 + 3]};
                                } else {
                                    bytePixel = new byte[]{pix[index4 + 3], pix[index4 + 2], pix[index4 + 1], pix[index4]};
                                }
                                floatPix[index] = ByteBuffer.wrap(bytePixel).getFloat();
                            }
                            stack.setPixels(floatPix, outSliceIndex);
                        } else if (bitDepth == 16) {
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
                    }
                }
            }
        } catch (Exception e) {
            GenUtils.logError(e, "Failed to load image plane.");
        }
    }
}
