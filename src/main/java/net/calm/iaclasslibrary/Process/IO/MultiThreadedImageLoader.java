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
package net.calm.iaclasslibrary.Process.IO;

import net.calm.iaclasslibrary.IO.BioFormats.LocationAgnosticBioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import ij.IJ;
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

    public void setup(LocationAgnosticBioFormatsImg img, Properties props, String[] propLabels) {

    }

    @Override
    public void run() {
        int nThreads = 1;
        this.exec = Executors.newFixedThreadPool(nThreads);
        try {
            int[] incs = new int[3];
            int kdiff = limits[7] - limits[6];
            int jdiff = limits[4] - limits[3];
            int idiff = limits[1] - limits[0];
            incs[0] = (kdiff > jdiff) && (kdiff > idiff) ? nThreads : 1;
            incs[1] = (jdiff > kdiff) && (jdiff > idiff) ? nThreads : 1;
            incs[2] = (idiff > jdiff) && (idiff > kdiff) ? nThreads : 1;
            RunnablePixelLoader[] loaders = new RunnablePixelLoader[nThreads];
            for (int t = 0; t < nThreads; t++) {
                loaders[t] = new RunnablePixelLoader(reader, stack, limits, t, nThreads, incs);
                loaders[t].start();
                //exec.submit(new RunnablePixelLoader(readers[t], stack, limits, t, nThreads, incs));
            }
            for (int thread = 0; thread < nThreads; thread++) {
                loaders[thread].join();
            }
        } catch (InterruptedException ie) {
            IJ.error("Error loading image.");
        }
        //terminate("Error loading image.");
    }

    public MultiThreadedImageLoader duplicate() {
        MultiThreadedImageLoader newProcess = new MultiThreadedImageLoader(null, null, null);
        this.updateOutputDests(newProcess);
        return newProcess;
    }
}
