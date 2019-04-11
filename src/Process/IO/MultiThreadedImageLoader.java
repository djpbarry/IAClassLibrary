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

import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import ij.ImageStack;
import java.util.Properties;
import java.util.concurrent.Executors;
import loci.formats.ImageReader;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedImageLoader extends MultiThreadedProcess {

    private final int[] limits;
    private final ImageReader reader;
    private final ImageStack stack;

    public MultiThreadedImageLoader(int[] limits, ImageReader reader, ImageStack stack) {
        super(null);
        this.limits = limits;
        this.reader = reader;
        this.stack = stack;
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {

    }

    @Override
    public void run() {
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int outSliceIndex = 1;
        for (int k = limits[6]; k < limits[7]; k++) {
            int kOffset = k * limits[5] * limits[2];
            for (int j = limits[3]; j < limits[4]; j++) {
                int jOffset = j * limits[2];
                for (int i = limits[0]; i < limits[1]; i++) {
                    exec.submit(new RunnablePixelLoader(reader, stack, kOffset + jOffset + i, outSliceIndex++));
                }
            }
        }
        terminate("Error loading image.");
    }

    public MultiThreadedImageLoader duplicate() {
        MultiThreadedImageLoader newProcess = new MultiThreadedImageLoader(null, null, null);
        this.updateOutputDests(newProcess);
        return newProcess;
    }
}
