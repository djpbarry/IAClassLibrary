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
package net.calm.iaclasslibrary.Process.Filtering;

import net.calm.iaclasslibrary.IO.BioFormats.LocationAgnosticBioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import net.calm.iaclasslibrary.Process.RunnableProcess;
import java.util.Properties;
import java.util.concurrent.Executors;
import mcib3d.image3d.ImageFloat;
import mcib3d.utils.ArrayUtil;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedSobelFilter extends MultiThreadedProcess {

    private final ImageFloat input;
    private ImageFloat result;
    private final double[] edgeX = {-1, 0, 1, -2, 0, 2, -1, 0, 1, -2, 0, 2, -4, 0, 4, -2, 0, 2, -1, 0, 1, -2, 0, 2, -1, 0, 1};
    private final double[] edgeY = {-1, -2, -1, 0, 0, 0, 1, 2, 1, -2, -4, -2, 0, 0, 0, 2, 4, 2, -1, -2, -1, 0, 0, 0, 1, 2, 1};
    private final double[] edgeZ = {-1, -2, -1, -2, -4, -2, -1, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 1, 2, 4, 2, 1, 2, 1};

    public MultiThreadedSobelFilter(MultiThreadedProcess[] inputs, ImageFloat input) {
        super(inputs);
        this.input = input;
    }

    public void setup(LocationAgnosticBioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
//        ImagePlus image = img.getLoadedImage();
//        (new StackConverter(img.getProcessedImage())).convertToGray32();
    }

    /**
     * Sobel-like filtering in 3D
     *
     * @return The 3D filtered image
     */
    public void run() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        this.exec = Executors.newFixedThreadPool(nThreads);
        result = input.duplicate();
        for (int t = 0; t < nThreads; t++) {
            exec.submit(new RunnableSobelFilter(t, nThreads, result, input));
        }
        terminate("Error generating sobel image.");
        this.output = result.getImagePlus();
    }

    public MultiThreadedSobelFilter duplicate() {
        MultiThreadedSobelFilter newProcess = new MultiThreadedSobelFilter(inputs, input);
        this.updateOutputDests(newProcess);
        return newProcess;
    }

    private class RunnableSobelFilter extends RunnableProcess {

        private final int thread, nThreads;
        private final ImageFloat result;
        private final ImageFloat input;

        public RunnableSobelFilter(int thread, int nThreads, ImageFloat result, ImageFloat input) {
            super(null);
            this.thread = thread;
            this.nThreads = nThreads;
            this.result = result;
            this.input = input;
        }

        public void run() {
            ArrayUtil nei;

            double ex;
            double ey;
            double ez;
            double edge;

            for (int k = thread; k < input.sizeZ; k += nThreads) {
                for (int j = 0; j < input.sizeY; j++) {
                    for (int i = 0; i < input.sizeX; i++) {
                        nei = getNeighborhood(input, i, j, k);
                        ex = nei.convolve(edgeX, 1.0f);
                        ey = nei.convolve(edgeY, 1.0f);
                        ez = nei.convolve(edgeZ, 1.0f);
                        edge = Math.sqrt(ex * ex + ey * ey + ez * ez);
                        result.setPixel(i, j, k, (float) edge);
                    }
                }
            }
        }

        public ArrayUtil getNeighborhood(ImageFloat input, int x, int y, int z) {
            ArrayUtil res = new ArrayUtil(27);
            int idx = 0;
            for (int k = z - 1; k <= z + 1; k++) {
                k = k < 0 ? 0 : k;
                k = k < input.sizeZ ? k : input.sizeZ - 1;
                for (int j = y - 1; j <= y + 1; j++) {
                    j = j < 0 ? 0 : j;
                    j = j < input.sizeY ? j : input.sizeY - 1;
                    for (int i = x - 1; i <= x + 1; i++) {
                        i = i < 0 ? 0 : i;
                        i = i < input.sizeX ? i : input.sizeX - 1;
                        res.putValue(idx, input.getPixel(i, j, k));
                        idx++;
                    }
                }
            }
            res.setSize(idx);
            return res;
        }
    }
}
