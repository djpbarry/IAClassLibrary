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
package Process.DistanceTransform;

import Process.Filtering.MultiThreadedSobelFilter;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageShort;
import mcib3d.image3d.distanceMap3d.EdtFloat;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class RiemannianDistanceTransform extends EdtFloat {

    private float lambda = 50.0f;
    private final byte BACKGROUND = 0;

    public RiemannianDistanceTransform() {
        super();
    }

    public ImageFloat run(ImageFloat greyImp, ImageShort binImp, float thresh, float scaleXY, float scaleZ) {
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
        IJ.log("Commencing Stage 1...");
        //Transformation 1.  Use s to store g.
        Step1Thread[] s1t = new Step1Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s1t[thread] = new Step1Thread(thread, nbCPUs, w, h, d, s, scale, binData, gradData);
            s1t[thread].start();
        }
        try {
            for (int thread = 0; thread < nbCPUs; thread++) {
                s1t[thread].join();
            }
        } catch (InterruptedException ie) {
            IJ.error("A thread was interrupted in step 1 .");
        }
        IJ.log("Commencing Stage 2...");
        //Transformation 2.  g (in s) -> h (in s)
        IJ.saveAs(new ImagePlus("", outStack), "TIF", "D:\\debugging\\giani_debug\\rdt1.tif");
        Step2Thread[] s2t = new Step2Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s2t[thread] = new Step2Thread(thread, nbCPUs, w, h, d, s, gradData);
            s2t[thread].start();
        }
        try {
            for (int thread = 0; thread < nbCPUs; thread++) {
                s2t[thread].join();
            }
        } catch (InterruptedException ie) {
            IJ.error("A thread was interrupted in step 2 .");
        }
        IJ.log("Commencing Stage 3...");
        IJ.saveAs(new ImagePlus("", outStack), "TIF", "D:\\debugging\\giani_debug\\rdt2.tif");
        Step4Thread[] s3t = new Step4Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s3t[thread] = new Step4Thread(thread, nbCPUs, w, h, d, s, gradData);
            s3t[thread].start();
        }
        try {
            for (int thread = 0; thread < nbCPUs; thread++) {
                s3t[thread].join();
            }
        } catch (InterruptedException ie) {
            IJ.error("A thread was interrupted in step 4 .");
        }
        //Transformation 3. h (in s) -> s
        IJ.log("Commencing Stage 4...");
        IJ.saveAs(new ImagePlus("", outStack), "TIF", "D:\\debugging\\giani_debug\\rdt3.tif");
        Step3Thread[] s4t = new Step3Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s4t[thread] = new Step3Thread(thread, nbCPUs, w, h, d, s, scale, gradData);
            s4t[thread].start();
        }
        try {
            for (int thread = 0; thread < nbCPUs; thread++) {
                s4t[thread].join();
            }
        } catch (InterruptedException ie) {
            IJ.error("A thread was interrupted in step 4 .");
        }
        //Find the largest distance for scaling
        //Also fill in the background values.
        IJ.saveAs(new ImagePlus("", outStack), "TIF", "D:\\debugging\\giani_debug\\rdt4.tif");
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

    float[] computeXDistances(float[][] gradPix, double lambda, int[] dims) {
        float[] sums = new float[dims[3]];
        float[] distances = new float[dims[1] * dims[3]];
        for (int k = dims[4]; k < dims[5]; k++) {
            for (int j = dims[2]; j < dims[3]; j++) {
                int jOffset = j * dims[1];
                for (int i = dims[0]; i < dims[1]; i++) {
                    sums[j] += (gradPix[k][i + jOffset] + 1.0f + lambda) / (1.0f + lambda);
                    distances[i + jOffset] = sums[j];
                }
            }
        }
        return distances;
    }

    float[] computeYDistances(float[][] gradPix, double lambda, int[] dims) {
        float[] sums = new float[dims[1]];
        float[] distances = new float[dims[1] * dims[3]];
        for (int k = dims[4]; k < dims[5]; k++) {
            for (int j = dims[2]; j < dims[3]; j++) {
                int jOffset = j * dims[1];
                for (int i = dims[0]; i < dims[1]; i++) {
                    sums[i] += (gradPix[k][i + jOffset] + 1.0f + lambda) / (1.0f + lambda);
                    distances[j + i * dims[3]] = sums[i];
                }
            }
        }
        return distances;
    }

    float[] computeZDistances(float[][] gradPix, double lambda, int[] dims) {
        float[] sums = new float[dims[1]];
        float[] distances = new float[dims[1] * dims[5]];
        for (int j = dims[2]; j < dims[3]; j++) {
            int jOffset = j * dims[1];
            for (int i = dims[0]; i < dims[1]; i++) {
                int iOffset = i * dims[5];
                for (int k = dims[4]; k < dims[5]; k++) {
                    sums[i] += (gradPix[k][i + jOffset] + 1.0f + lambda) / (1.0f + lambda);
                    distances[k + iOffset] = sums[i];
                }
            }
        }
        return distances;
    }

    class Step1Thread extends Thread {

        int thread, nThreads, w, h, d;
        float[][] s;
        float[][] gradData;
        short[][] binData;
        float scaleZ;

        public Step1Thread(int thread, int nThreads, int w, int h, int d, float[][] s, float scaleZ, short[][] binData, float[][] gradData) {
            this.thread = thread;
            this.nThreads = nThreads;
            this.w = w;
            this.h = h;
            this.d = d;
            this.binData = binData;
            this.gradData = gradData;
            this.s = s;
            this.scaleZ = scaleZ * scaleZ;
        }

        public void run() {
            float[] sk;
            short[] bk;
            int n = w;
            if (h > n) {
                n = h;
            }
            if (d > n) {
                n = d;
            }
            float test, min;
            for (int k = thread; k < d; k += nThreads) {
                float[] distances = computeXDistances(gradData, lambda, new int[]{0, w, 0, h, k, k + 1});
                sk = s[k];
                bk = binData[k];
                for (int j = 0; j < h; j++) {
                    int jOffset = w * j;
                    for (int i = 0; i < w; i++) {
                        min = Float.MAX_VALUE;
                        for (int x = i; x < w; x++) {
                            if (bk[x + jOffset] != BACKGROUND) {
                                test = (float) Math.pow(distances[x + jOffset] - distances[i + jOffset], 2.0);
                                if (test < min) {
                                    min = test;
                                }
                                break;
                            }
                        }
                        for (int x = i - 1; x >= 0; x--) {
                            if (bk[x + jOffset] != BACKGROUND) {
                                test = (float) Math.pow(distances[x + jOffset] - distances[i + jOffset], 2.0);
                                if (test < min) {
                                    min = test;
                                }
                                break;
                            }
                        }
                        sk[i + jOffset] = min;
                    }
                }
            }
        }
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
            int n = w;
            if (h > n) {
                n = h;
            }
            if (d > n) {
                n = d;
            }
            float[] tempInt = new float[n];
            float[] tempS = new float[n];
            boolean nonempty;
            float test, min;
            for (int k = thread; k < d; k += nThreads) {
                float[] distances = computeYDistances(gradData, lambda, new int[]{0, w, 0, h, k, k + 1});
                sk = s[k];
                for (int i = 0; i < w; i++) {
                    nonempty = false;
                    int iOffset = i * h;
                    for (int j = 0; j < h; j++) {
                        tempS[j] = sk[i + w * j];
                        if (tempS[j] > 0) {
                            nonempty = true;
                        }
                    }
                    if (nonempty) {
                        for (int j = 0; j < h; j++) {
                            min = Float.MAX_VALUE;
                            for (int y = j; y < h; y++) {
                                test = tempS[y] + (float) Math.pow(distances[y + iOffset] - distances[j + iOffset], 2.0);
                                if (test < min) {
                                    min = test;
                                }
                            }
                            for (int y = j - 1; y >= 0; y--) {
                                test = tempS[y] + (float) Math.pow(distances[y + iOffset] - distances[j + iOffset], 2.0);
                                if (test < min) {
                                    min = test;
                                }
                            }
                            tempInt[j] = min;
                        }
                        for (int j = 0; j < h; j++) {
                            sk[i + w * j] = tempInt[j];
                        }
                    }
                }
            }
        }
    }

    class Step3Thread extends Thread {

        int thread, nThreads, w, h, d;
        float[][] s;
        float[][] gradData;
        float scaleZ;

        public Step3Thread(int thread, int nThreads, int w, int h, int d, float[][] s, float scaleZ, float[][] gradData) {
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
            int n = w;
            if (h > n) {
                n = h;
            }
            if (d > n) {
                n = d;
            }
            float[] tempInt = new float[n];
            float[] tempS = new float[n];
            boolean nonempty;
            float test, min;
            for (int j = thread; j < h; j += nThreads) {
                float[] distances = computeZDistances(gradData, lambda, new int[]{0, w, j, j + 1, 0, d});
                for (int i = 0; i < w; i++) {
                    int iOffset = i * d;
                    nonempty = false;
                    for (int k = 0; k < d; k++) {
                        tempS[k] = s[k][i + w * j];
                        if (tempS[k] > 0) {
                            nonempty = true;
                        }
                    }
                    if (nonempty) {
                        for (int k = 0; k < d; k++) {
                            min = Float.MAX_VALUE;
                            for (int z = k; z < d; z++) {
                                test = tempS[z] + scaleZ * (float) Math.pow(distances[z + iOffset] - distances[k + iOffset], 2.0);
                                if (test < min) {
                                    min = test;
                                }
                            }
                            for (int z = k - 1; z >= 0; z--) {
                                test = tempS[z] + scaleZ * (float) Math.pow(distances[z + iOffset] - distances[k + iOffset], 2.0);
                                if (test < min) {
                                    min = test;
                                }
                            }
                            tempInt[k] = min;
                        }
                        for (int k = 0; k < d; k++) {
                            s[k][i + w * j] = tempInt[k];
                        }
                    }
                }
            }
        }
    }

    class Step4Thread extends Thread {

        int thread, nThreads, w, h, d;
        float[][] s;
        float[][] gradData;

        public Step4Thread(int thread, int nThreads, int w, int h, int d, float[][] s, float[][] gradData) {
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
            int n = w;
            if (h > n) {
                n = h;
            }
            if (d > n) {
                n = d;
            }
            float[] tempInt = new float[n];
            float[] tempS = new float[n];
            float test, min;
            for (int k = thread; k < d; k += nThreads) {
                float[] distances = computeXDistances(gradData, lambda, new int[]{0, w, 0, h, k, k + 1});
                sk = s[k];

                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        tempS[i] = sk[i + w * j];
                    }
                    int jOffset = w * j;
                    for (int i = 0; i < w; i++) {
                        min = Float.MAX_VALUE;
                        for (int x = i; x < w; x++) {
                            test = tempS[x] + (float) Math.pow(distances[x + jOffset] - distances[i + jOffset], 2.0);
                            if (test < min) {
                                min = test;
                            }
                        }
                        for (int x = i - 1; x >= 0; x--) {
                            test = tempS[x] + (float) Math.pow(distances[x + jOffset] - distances[i + jOffset], 2.0);
                            if (test < min) {
                                min = test;
                            }
                        }
                        tempInt[i] = min;
                    }
                    for (int i = 0; i < w; i++) {
                        sk[i + w * j] = tempInt[i];
                    }
                }
            }
        }
    }

}
