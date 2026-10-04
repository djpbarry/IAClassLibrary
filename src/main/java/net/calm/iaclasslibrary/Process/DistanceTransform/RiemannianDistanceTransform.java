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
package net.calm.iaclasslibrary.Process.DistanceTransform;

import net.calm.iaclasslibrary.Process.Filtering.MultiThreadedSobelFilter;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageShort;
import mcib3d.image3d.distanceMap3d.EdtFloat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class RiemannianDistanceTransform extends EdtFloat {

    private double lambda;
    private final byte BACKGROUND = 0;

    public RiemannianDistanceTransform() {
        super();
    }

    public ImageFloat run(ImageFloat greyImp, ImageShort binImp, float thresh, float scaleXY, float scaleZ, float lambda) {
        this.lambda = lambda;
        int nbCPUs = Runtime.getRuntime().availableProcessors();
        int w = greyImp.sizeX;
        int h = greyImp.sizeY;
        int d = greyImp.sizeZ;
        float scale = scaleZ / scaleXY;
        float[][] greyData = greyImp.pixels;
        IJ.log("Generating gradient image.");
        float[][] gradData = getGradImage(greyImp.getImagePlus()).pixels;
        short[][] binData = binImp.pixels;
        //Create 32 bit floating point stack for output, s.  Will also use it for g in Transormation 1.
        ImageStack outStack = new ImageStack(w, h);
        float[][] s = new float[d][];
        for (int k = 0; k < d; k++) {
            ImageProcessor ipk = new FloatProcessor(w, h);
            outStack.addSlice(null, ipk);
            s[k] = (float[]) ipk.getPixels();
        }
        float[] sk;
        ExecutorService exec = Executors.newFixedThreadPool(nbCPUs);
        IJ.log("Commencing Stage 1...");
        //Transformation 1.  Use s to store g.
        Step1Thread[] s1t = new Step1Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s1t[thread] = new Step1Thread(thread, nbCPUs, w, h, d, s, scale, gradData, binData);
        }
        runWorkers(s1t, exec, "step 1");
        IJ.log("Commencing Stage 2...");
        //Transformation 2.  g (in s) -> h (in s)
        Step2Thread[] s2t = new Step2Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s2t[thread] = new Step2Thread(thread, nbCPUs, w, h, d, s, gradData);
        }
        runWorkers(s2t, exec, "step 2");
        IJ.log("Commencing Stage 3...");
        Step3Thread[] s3t = new Step3Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s3t[thread] = new Step3Thread(thread, nbCPUs, w, h, d, s, gradData);
        }
        runWorkers(s3t, exec, "step 3");
//        //Transformation 3. h (in s) -> s
        IJ.log("Commencing Stage 4...");
        Step4Thread[] s4t = new Step4Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s4t[thread] = new Step4Thread(thread, nbCPUs, w, h, d, s, scale, gradData);
        }
        runWorkers(s4t, exec, "step 4");
        exec.shutdown();
        //Find the largest distance for scaling
        //Also fill in the background values.
        float distMax = 0;
        int wh = w * h;
        float dist;
        for (int k = 0; k < d; k++) {
            sk = s[k];
            for (int ind = 0; ind < wh; ind++) {
                if ((greyData[k][ind] <= thresh)) {
                    sk[ind] = 0;
                } else {
                    dist = (float) Math.sqrt(sk[ind]) * scaleXY;
                    sk[ind] = dist;
                    distMax = (dist > distMax) ? dist : distMax;
                }
            }
        }

        ImageFloat res = (ImageFloat) ImageFloat.wrap(outStack);
        res.setScale(greyImp);
        res.setOffset(greyImp);
        res.setMinAndMax(0, distMax);
        return res;
    }

    private void runWorkers(Runnable[] workers, ExecutorService exec, String stepName) {
        List<Future<?>> futures = new ArrayList<>(workers.length);
        for (Runnable worker : workers) {
            futures.add(exec.submit(worker));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                GenUtils.logError(e, "A thread was interrupted in " + stepName + ".");
            } catch (ExecutionException e) {
                GenUtils.logError(e, "A worker failed in " + stepName + ".");
            }
        }
    }

    ImageFloat getGradImage(ImagePlus greyImp) {
        MultiThreadedSobelFilter sobel = new MultiThreadedSobelFilter(null, new ImageFloat(greyImp));
        sobel.start();
        try {
            sobel.join();
        } catch (InterruptedException e) {
            GenUtils.logError(e, "Failed to generate sobel-filtered input.");
            return null;
        }
        return new ImageFloat(sobel.getOutput());
    }

    double[] computeXDistances(float[][] gradPix, double lambda, int[] dims) {
        double[] sums = new double[dims[3]];
        double[] distances = new double[dims[1] * dims[3]];
        for (int k = dims[4]; k < dims[5]; k++) {
            for (int j = dims[2]; j < dims[3]; j++) {
                int jOffset = j * dims[1];
                for (int i = dims[0]; i < dims[1]; i++) {
                    sums[j] += (gradPix[k][i + jOffset] + 1.0 + lambda) / (1.0 + lambda);
                    distances[i + jOffset] = sums[j];
                }
            }
        }
        return distances;
    }

    double[] computeYDistances(float[][] gradPix, double lambda, int[] dims) {
        double[] sums = new double[dims[1]];
        double[] distances = new double[dims[1] * dims[3]];
        for (int k = dims[4]; k < dims[5]; k++) {
            for (int j = dims[2]; j < dims[3]; j++) {
                int jOffset = j * dims[1];
                for (int i = dims[0]; i < dims[1]; i++) {
                    sums[i] += (gradPix[k][i + jOffset] + 1.0 + lambda) / (1.0 + lambda);
                    distances[j + i * dims[3]] = sums[i];
                }
            }
        }
        return distances;
    }

    double[] computeZDistances(float[][] gradPix, double lambda, int[] dims) {
        double[] sums = new double[dims[1]];
        double[] distances = new double[dims[1] * dims[5]];
        for (int j = dims[2]; j < dims[3]; j++) {
            int jOffset = j * dims[1];
            for (int i = dims[0]; i < dims[1]; i++) {
                int iOffset = i * dims[5];
                for (int k = dims[4]; k < dims[5]; k++) {
                    sums[i] += (gradPix[k][i + jOffset] + 1.0 + lambda) / (1.0 + lambda);
                    distances[k + iOffset] = sums[i];
                }
            }
        }
        return distances;
    }

    static double[] distanceTransformSampled(double[] f, double[] g, double w) {
        int n = f.length;
        int[] v = new int[n];
        double[] z = new double[n + 1];
        double[] d = new double[n];
        int k = 0;
        v[0] = 0;
        z[0] = Double.NEGATIVE_INFINITY;
        z[1] = Double.POSITIVE_INFINITY;
        for (int q = 1; q < n; q++) {
            double s = ((f[q] + w * g[q] * g[q]) - (f[v[k]] + w * g[v[k]] * g[v[k]])) / (2.0 * w * (g[q] - g[v[k]]));
            while (s <= z[k]) {
                k--;
                s = ((f[q] + w * g[q] * g[q]) - (f[v[k]] + w * g[v[k]] * g[v[k]])) / (2.0 * w * (g[q] - g[v[k]]));
            }
            k++;
            v[k] = q;
            z[k] = s;
            z[k + 1] = Double.POSITIVE_INFINITY;
        }
        k = 0;
        for (int q = 0; q < n; q++) {
            while (z[k + 1] < g[q]) {
                k++;
            }
            double dx = g[q] - g[v[k]];
            d[q] = w * dx * dx + f[v[k]];
        }
        return d;
    }

    static double[] nearestForegroundDistance(double[] g, short[] mask, double w, short background) {
        int n = g.length;
        double[] d = new double[n];
        int lastFg = -1;
        for (int k = 0; k < n; k++) {
            if (mask[k] != background) {
                lastFg = k;
            }
            if (lastFg >= 0) {
                double dx = g[k] - g[lastFg];
                d[k] = w * dx * dx;
            } else {
                d[k] = Float.MAX_VALUE;
            }
        }
        int nextFg = -1;
        for (int k = n - 1; k >= 0; k--) {
            if (mask[k] != background) {
                nextFg = k;
            }
            if (nextFg >= 0) {
                double dx = g[nextFg] - g[k];
                double candidate = w * dx * dx;
                if (candidate < d[k]) {
                    d[k] = candidate;
                }
            }
        }
        return d;
    }

    class Step2Thread extends Thread {

        int thread, nThreads, w, h, d;
        float[][] s;
        float[][] gradData;

        public Step2Thread(int thread, int nThreads, int w, int h, int d, float[][] s, float[][] gradData) {
            this.gradData = gradData;
            this.thread = thread;
            this.nThreads = nThreads;
            this.w = w;
            this.h = h;
            this.d = d;
            this.s = s;
        }

        public void run() {
            float[] sk;
            double[] tempS = new double[h];
            double[] g = new double[h];
            boolean nonempty;
            for (int k = thread; k < d; k += nThreads) {
                double[] distances = computeYDistances(gradData, lambda, new int[]{0, w, 0, h, k, k + 1});
                sk = s[k];
                for (int i = 0; i < w; i++) {
                    nonempty = false;
                    int iOffset = i * h;
                    for (int j = 0; j < h; j++) {
                        tempS[j] = sk[i + w * j];
                        g[j] = distances[j + iOffset];
                        if (tempS[j] > 0) {
                            nonempty = true;
                        }
                    }
                    if (nonempty) {
                        double[] tempInt = distanceTransformSampled(tempS, g, 1.0);
                        for (int j = 0; j < h; j++) {
                            sk[i + w * j] = (float) tempInt[j];
                        }
                    }
                }
            }
        }
    }

    class Step1Thread extends Thread {

        int thread, nThreads, w, h, d;
        float[][] s;
        float[][] gradData;
        float scaleZ;
        short[][] binData;

        public Step1Thread(int thread, int nThreads, int w, int h, int d, float[][] s, float scaleZ, float[][] gradData, short[][] binData) {
            this.thread = thread;
            this.nThreads = nThreads;
            this.w = w;
            this.h = h;
            this.d = d;
            this.s = s;
            this.gradData = gradData;
            this.binData = binData;
            this.scaleZ = scaleZ * scaleZ;
        }

        public void run() {
            double[] g = new double[d];
            short[] mask = new short[d];
            for (int j = thread; j < h; j += nThreads) {
                int jOffset = j * w;
                double[] distances = computeZDistances(gradData, lambda, new int[]{0, w, j, j + 1, 0, d});
                for (int i = 0; i < w; i++) {
                    int iOffset = i * d;
                    for (int k = 0; k < d; k++) {
                        g[k] = distances[k + iOffset];
                        mask[k] = binData[k][i + jOffset];
                    }
                    double[] tempInt = nearestForegroundDistance(g, mask, scaleZ, BACKGROUND);
                    for (int k = 0; k < d; k++) {
                        s[k][i + w * j] = (float) tempInt[k];
                    }
                }
            }
        }
    }

    class Step3Thread extends Thread {

        int thread, nThreads, w, h, d;
        float[][] s;
        float[][] gradData;

        public Step3Thread(int thread, int nThreads, int w, int h, int d, float[][] s, float[][] gradData) {
            this.thread = thread;
            this.nThreads = nThreads;
            this.w = w;
            this.h = h;
            this.d = d;
            this.gradData = gradData;
            this.s = s;
        }

        public void run() {
            float[] sk;
            double[] tempS = new double[w];
            double[] g = new double[w];
            for (int k = thread; k < d; k += nThreads) {
                double[] distances = computeXDistances(gradData, lambda, new int[]{0, w, 0, h, k, k + 1});
                sk = s[k];
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        tempS[i] = sk[i + w * j];
                    }
                    int jOffset = w * j;
                    for (int i = 0; i < w; i++) {
                        g[i] = distances[i + jOffset];
                    }
                    double[] tempInt = distanceTransformSampled(tempS, g, 1.0);
                    for (int i = 0; i < w; i++) {
                        sk[i + w * j] = (float) tempInt[i];
                    }
                }
            }
        }

    }

    class Step4Thread extends Thread {

        int thread, nThreads, w, h, d;
        float[][] s;
        float[][] gradData;
        float scaleZ;

        public Step4Thread(int thread, int nThreads, int w, int h, int d, float[][] s, float scaleZ, float[][] gradData) {
            this.thread = thread;
            this.nThreads = nThreads;
            this.w = w;
            this.h = h;
            this.d = d;
            this.s = s;
            this.gradData = gradData;
            this.scaleZ = scaleZ * scaleZ;
        }

        public void run() {
            double[] tempS = new double[d];
            double[] g = new double[d];
            for (int j = thread; j < h; j += nThreads) {
                double[] distances = computeZDistances(gradData, lambda, new int[]{0, w, j, j + 1, 0, d});
                for (int i = 0; i < w; i++) {
                    int iOffset = i * d;
                    for (int k = 0; k < d; k++) {
                        tempS[k] = s[k][i + w * j];
                        g[k] = distances[k + iOffset];
                    }
                    double[] tempInt = distanceTransformSampled(tempS, g, scaleZ);
                    for (int k = 0; k < d; k++) {
                        s[k][i + w * j] = (float) tempInt[k];
                    }
                }
            }
        }
    }

}
