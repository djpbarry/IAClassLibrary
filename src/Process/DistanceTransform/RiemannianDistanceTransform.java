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

import ij.IJ;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.util.Arrays;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.distanceMap3d.EdtFloat;
import mcib3d.image3d.processing.FastFilters3D;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class RiemannianDistanceTransform extends EdtFloat {

    private float lambda = 1.0f;
    private final byte BACKGROUND = 0, FOREGROUND = 1;

    public RiemannianDistanceTransform() {
        super();
    }

    public ImageFloat run(ImageFloat greyImp, ImageByte binImp, float thresh, float scaleXY, float scaleZ, int nbCPUs) {
        int w = greyImp.sizeX;
        int h = greyImp.sizeY;
        int d = greyImp.sizeZ;
        float scale = scaleZ / scaleXY;
        ImageStack gradStack = FastFilters3D.filterImageStack(greyImp.getImageStack(), FastFilters3D.SOBEL, 1, 1, (int) Math.round(scale), nbCPUs, false);
        float[][] greyData = greyImp.pixels;
        float[][] gradData = (new ImageFloat(gradStack)).pixels;
        byte[][] binData = binImp.pixels;
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
            s1t[thread] = new Step1Thread(thread, nbCPUs, w, h, d, thresh, s, greyData, scale, binData, gradData);
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
        Step2Thread[] s2t = new Step2Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s2t[thread] = new Step2Thread(thread, nbCPUs, w, h, d, s, gradData, greyData, thresh);
            s2t[thread].start();
        }
        try {
            for (int thread = 0; thread < nbCPUs; thread++) {
                s2t[thread].join();
            }
        } catch (InterruptedException ie) {
            IJ.error("A thread was interrupted in step 2 .");
        }
        //Transformation 3. h (in s) -> s
        IJ.log("Commencing Stage 3...");
        Step3Thread[] s3t = new Step3Thread[nbCPUs];
        for (int thread = 0; thread < nbCPUs; thread++) {
            s3t[thread] = new Step3Thread(thread, nbCPUs, w, h, d, s, greyData, thresh, scale, gradData);
            s3t[thread].start();
        }
        try {
            for (int thread = 0; thread < nbCPUs; thread++) {
                s3t[thread].join();
            }
        } catch (InterruptedException ie) {
            IJ.error("A thread was interrupted in step 3 .");
        }
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

    float calcDistance(int[] limits, float[][] gradPix, double lambda, int width) {
        float sum = 0.0f;
        for (int k = limits[4]; k < limits[5]; k++) {
            for (int j = limits[2]; j < limits[3]; j++) {
                int jOffset = j * width;
                for (int i = limits[0]; i < limits[1]; i++) {
                    sum += (gradPix[k][i + jOffset] + 1.0f + lambda) / (1.0f + lambda);
                }
            }
        }
        return sum * sum;
    }

    float[] computeDistances(int[] limits, float[][] gradPix, double lambda, int width, int dimLength) {
        float sum = 0.0f;
        float[] distances = new float[dimLength];
        int distIndex = 0;
        for (int k = limits[4]; k < limits[5]; k++) {
            for (int j = limits[2]; j < limits[3]; j++) {
                int jOffset = j * width;
                for (int i = limits[0]; i < limits[1]; i++) {
                    sum += (gradPix[k][i + jOffset] + 1.0f + lambda) / (1.0f + lambda);
                    distances[distIndex++] = sum * sum;
                }
            }
        }
        for (int d = 0; d < dimLength; d++) {
            distances[d] -= distances[0];
        }
        return distances;
    }

    class Step1Thread extends Thread {

        int thread, nThreads, w, h, d;
        float thresh;
        float[][] s;
        float[][] greyData;
        float[][] gradData;
        byte[][] binData;
        float scaleZ;

        public Step1Thread(int thread, int nThreads, int w, int h, int d, float thresh, float[][] s, float[][] greyData, float scaleZ, byte[][] binData, float[][] gradData) {
            this.thread = thread;
            this.nThreads = nThreads;
            this.w = w;
            this.h = h;
            this.d = d;
            this.thresh = thresh;
            this.greyData = greyData;
            this.binData = binData;
            this.gradData = gradData;
            this.s = s;
            this.scaleZ = scaleZ * scaleZ;
        }

        public void run() {
            float[] sk;
            float[] dk;
            byte[] bk;
            int n = w;
            if (h > n) {
                n = h;
            }
            if (d > n) {
                n = d;
            }
            float test, min;
            for (int k = thread; k < d; k += nThreads) {
                sk = s[k];
                dk = greyData[k];
                bk = binData[k];
                for (int j = 0; j < h; j++) {
                    int jOffset = w * j;
//                    float[] distances = computeDistances(new int[]{0, w, j, j + 1, k, k + 1}, gradData, lambda, w, w);
                    for (int i = 0; i < w; i++) {
//                        if ((dk[i + jOffset] <= thresh)) {
//                            continue;
//                        }
//                        min = Math.min(calcDistance(new int[]{0, i, j, j + 1, k, k + 1}, gradData, lambda, w),
//                                calcDistance(new int[]{i, w - 1, j, j + 1, k, k + 1}, gradData, lambda, w));
                        min = Float.MAX_VALUE;
                        for (int x = i; x < w; x++) {
                            if (bk[x + jOffset] == BACKGROUND) {
                                test = calcDistance(new int[]{i, x, j, j + 1, k, k + 1}, gradData, lambda, w);
//                                tempBinData[k][i + jOffset] = BACKGROUND;
                                if (test < min) {
                                    min = test;
                                }
                                break;
                            }
                        }
                        for (int x = i - 1; x >= 0; x--) {
                            if (bk[x + jOffset] == BACKGROUND) {
                                test = calcDistance(new int[]{x, i, j, j + 1, k, k + 1}, gradData, lambda, w);
//                                tempBinData[k][i + jOffset] = BACKGROUND;
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
        float thresh;
        float[][] greyData;

        public Step2Thread(int thread, int nThreads, int w, int h, int d, float[][] s, float[][] gradData, float[][] greyData, float thresh) {
            this.gradData = gradData;
            this.greyData = greyData;
            this.thread = thread;
            this.nThreads = nThreads;
            this.thresh = thresh;
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
                sk = s[k];
                for (int i = 0; i < w; i++) {
                    nonempty = false;
                    for (int j = 0; j < h; j++) {
                        tempS[j] = sk[i + w * j];
                        if (tempS[j] > 0) {
                            nonempty = true;
                        }
                    }
                    if (nonempty) {
//                        float[] distances = computeDistances(new int[]{i, i + 1, 0, h, k, k + 1}, gradData, lambda, w, h);
                        for (int j = 0; j < h; j++) {
//                            if (i == 12 && j == 30 && k == 19) {
//                                IJ.wait(0);
//                            }
//                            if ((greyData[k][i + w * j] <= thresh)) {
//                                tempInt[j] = 0.0f;
//                                continue;
//                            }
//                            min = Math.min(calcDistance(new int[]{i, i + 1, 0, j, k, k + 1}, gradData, lambda, w),
//                                    calcDistance(new int[]{i, i + 1, j, h - 1, k, k + 1}, gradData, lambda, w));
                            min = Float.MAX_VALUE;
                            for (int y = j; y < h; y++) {
//                                if (binData[k][i + y * w] == BACKGROUND) {
                                    test = tempS[y] + calcDistance(new int[]{i, i + 1, j, y, k, k + 1}, gradData, lambda, w);
//                                    tempBin[j] = BACKGROUND;
                                    if (test < min) {
                                        min = test;
                                    }
//                                }
                            }
                            for (int y = j - 1; y >= 0; y--) {
//                                if (binData[k][i + y * w] == BACKGROUND) {
                                    test = tempS[y] + calcDistance(new int[]{i, i + 1, y, j, k, k + 1}, gradData, lambda, w);
//                                    tempBin[j] = BACKGROUND;
                                    if (test < min) {
                                        min = test;
                                    }
//                                }
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
        float thresh;
        float[][] s;
        float[][] greyData;
        float[][] gradData;
        float scaleZ;

        public Step3Thread(int thread, int nThreads, int w, int h, int d, float[][] s, float[][] greyData, float thresh, float scaleZ, float[][] gradData) {
            this.thresh = thresh;
            this.thread = thread;
            this.nThreads = nThreads;
            this.w = w;
            this.h = h;
            this.d = d;
            this.s = s;
            this.greyData = greyData;
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
                for (int i = 0; i < w; i++) {
                    nonempty = false;
                    for (int k = 0; k < d; k++) {
                        tempS[k] = s[k][i + w * j];
                        if (tempS[k] > 0) {
                            nonempty = true;
                        }
                    }
                    if (nonempty) {
//                        float[] distances = computeDistances(new int[]{i, i + 1, j, j + 1, zStart, zStop + 1}, gradData, lambda, w, d);
                        for (int k = 0; k < d; k++) {
//                            if ((greyData[k][i + w * j] > thresh)) {
//                                min = scaleZ * Math.min(calcDistance(new int[]{i, i + 1, j, j + 1, 0, k}, gradData, lambda, w),
//                                        calcDistance(new int[]{i, i + 1, j, j + 1, k, d - 1}, gradData, lambda, w));// bug fixed
                                min = Float.MAX_VALUE;
                                for (int z = k; z < d; z++) {
//                                    if (binData[z][i + j * w] == BACKGROUND) {
                                        test = tempS[z] + scaleZ * calcDistance(new int[]{i, i + 1, j, j + 1, k, z}, gradData, lambda, w);
                                        if (test < min) {
                                            min = test;
                                        }
//                                    }
                                }
                                for (int z = k - 1; z >= 0; z--) {
//                                    if (binData[z][i + j * w] == BACKGROUND) {
                                        test = tempS[z] + scaleZ * calcDistance(new int[]{i, i + 1, j, j + 1, z, k}, gradData, lambda, w);
                                        if (test < min) {
                                            min = test;
                                        }
//                                    }
                                }
                                tempInt[k] = min;
//                            } else {
//                                tempInt[k] = 0.0f;
//                            }
                        }
                        for (int k = 0; k < d; k++) {
                            s[k][i + w * j] = tempInt[k];
                        }
                    }
                }
            }
        }
    }
}
