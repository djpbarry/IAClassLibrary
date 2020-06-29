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

import net.calm.iaclasslibrary.Extrema.MultiThreadedMaximaFinder;
import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import ij.ImagePlus;
import ij.ImageStack;
import imagescience.feature.Hessian;
import imagescience.image.Image;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedHessian extends MultiThreadedProcess {

    private final Image input;
    private Image[][] result;
    private double startScale;
    private double stopScale;
    private double scaleStep;
    private boolean abs;
//    private int series;

    public MultiThreadedHessian(MultiThreadedProcess[] inputs, Image input) {
        super(inputs);
        this.input = input;
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
        this.startScale = Double.parseDouble(props.getProperty(propLabels[MultiThreadedMaximaFinder.HESSIAN_START_SCALE]));
        this.stopScale = Double.parseDouble(props.getProperty(propLabels[MultiThreadedMaximaFinder.HESSIAN_STOP_SCALE]));
        this.scaleStep = Double.parseDouble(props.getProperty(propLabels[MultiThreadedMaximaFinder.HESSIAN_SCALE_STEP]));
        this.abs = Boolean.parseBoolean(props.getProperty(propLabels[MultiThreadedMaximaFinder.HESSIAN_ABS]));
//        this.series = Integer.parseInt(props.getProperty(propLabels[MultiThreadedMaximaFinder.SERIES_SELECT]));
    }

    public void run() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        int nScales = (int) Math.ceil(1.0 + (stopScale - startScale) / scaleStep);
        double[] scales = new double[nScales];
        result = new Image[nScales][3];
        for (int s = 0; s < nScales; s++) {
            scales[s] = startScale + s * scaleStep;
        }

        int nProcesses = Math.min(nThreads, nScales);

        this.exec = Executors.newFixedThreadPool(nProcesses);

        final ArrayList< Future< Void>> futures = new ArrayList<>();

        for (int t = 0; t < nProcesses; t++) {
            futures.add(exec.submit(new RunnableHessianFilter(t, nProcesses, scales, abs, result, input.duplicate()), null));
        }

        for (final Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                GenUtils.logError(e, "Failed to generate Hessian images");
            }
        }

        exec.shutdown();

//        terminate("Error generating hessian images.");
        ImageStack outStack = new ImageStack(input.imageplus().getWidth(), input.imageplus().getHeight());
//        int count = 0;
        for (Image[] images : result) {
            for (Image i : images) {
                if (i == null) {
                    continue;
                }
//                IJ.saveAs(i.imageplus(), "TIF", "D:\\debugging\\giani_debug\\hessian_output_" + count++ + ".tif");
                ImageStack stack = i.imageplus().getImageStack();
                for (int s = 1; s <= stack.size(); s++) {
                    outStack.addSlice(stack.getProcessor(s));
                }
            }
        }

        this.output = new ImagePlus("Result of Hessian Filtering", outStack);
    }

    public MultiThreadedHessian duplicate() {
        MultiThreadedHessian newProcess = new MultiThreadedHessian(inputs, input);
        this.updateOutputDests(newProcess);
        return newProcess;
    }

    private class RunnableHessianFilter extends Thread implements Callable<Void> {

        private final Image[][] results;
        private final Image input;
        private final double[] scales;
        private final boolean absolute;
        private final int thread;
        private final int nThreads;

        public RunnableHessianFilter(int thread, int nThreads, double[] scales, boolean absolute, Image[][] results, Image input) {
            this.scales = scales;
            this.absolute = absolute;
            this.results = results;
            this.input = input;
            this.thread = thread;
            this.nThreads = nThreads;
        }

        public void run() {
            for (int t = thread; t < scales.length; t += nThreads) {
                final Vector<Image> eigenimages = (new Hessian()).run(input, scales[t], absolute);
                for (int e = 0; e < eigenimages.size(); e++) {
                    results[t][e] = eigenimages.get(e);
                }
            }
        }

        public Void call() {
            return null;
        }
    }
}
