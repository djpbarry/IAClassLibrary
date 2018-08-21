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
package Extrema;

import Process.MultiThreadedProcess;
import UtilClasses.GenUtils;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.StackConverter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedMaximaFinder extends MultiThreadedProcess {

    public ImagePlus makeLocalMaximaImage(int xyRadius, ImagePlus imp, float maxThresh, boolean varyBG, boolean absolute, int zRadius, byte background) {
        (new StackConverter(imp)).convertToGray32();
        ArrayList<int[]> localMaxima = new ArrayList();
        try {
            localMaxima = findLocalMaxima(xyRadius, imp.getImageStack(), maxThresh, varyBG, absolute, zRadius);
        } catch (InterruptedException e) {
            GenUtils.logError(e, "Error detecting local maxima.");
        }
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImageStack output = new ImageStack(width, height);
        byte foreground = (byte) ~background;
        for (int n = 0; n < imp.getNSlices(); n++) {
            ByteProcessor bp = new ByteProcessor(width, height);
            bp.setValue(background);
            bp.fill();
            output.addSlice(bp);
        }
        Object[] stackPix = output.getImageArray();
        for (int[] pix : localMaxima) {
            ((byte[]) stackPix[pix[2]])[pix[0] + pix[1] * width] = foreground;
        }
        return new ImagePlus(String.format("%s - Local Maxima", imp.getTitle()), output);
    }

    public ArrayList<int[]> findLocalMaxima(int xyRadius, ImageStack stack, float maxThresh, boolean varyBG, boolean absolute, int zRadius) throws InterruptedException {
        if (stack == null) {
            return null;
        }
        int width = stack.getWidth();
        int height = stack.getHeight();
        int depth = stack.getSize();
        ArrayList<int[]> maxima = new ArrayList();
        Object[] stackPix = stack.getImageArray();
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    exec.submit(new RunnableMaximaFinder(stackPix, varyBG, absolute,
                            maxThresh, maxima, new int[]{x, y, z}, new int[]{width, height, depth},
                            new int[]{xyRadius, xyRadius, zRadius}));
                }
            }
        }
        exec.shutdown();
        exec.awaitTermination(12, TimeUnit.HOURS);
        return maxima;
    }

}
