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

import Process.RunnableProcess;
import java.util.ArrayList;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class RunnableMaximaFinder extends RunnableProcess {

    private final Object[] stackPix;
    private final float maxThresh;
    private final ArrayList<int[]> maxima;
    private final int[] dims;
    private final int[] radii;
    private int thread;
    private int nThreads;

    public RunnableMaximaFinder(Object[] stackPix, float maxThresh, ArrayList<int[]> maxima, int[] dims, int thread, int nThreads, int[] radii) {
        super(null);
        this.stackPix = stackPix;
        this.maxThresh = maxThresh;
        this.maxima = maxima;
        this.dims = dims;
        this.thread = thread;
        this.nThreads = nThreads;
        this.radii = radii;

    }

    public void run() {
        for (int k = thread; k < dims[2]; k += nThreads) {
            for (int j = 0; j < dims[1]; j++) {
                int jOffset = j * dims[0];
                for (int i = 0; i < dims[0]; i++) {
                    if (!(((float[]) stackPix[k])[i + jOffset] > 0.0)) {
                        continue;
                    }
                    int[] location = new int[]{i, j, k};
//                    System.out.println(String.format("%d %d %d %d %f", maxima.size(), location[0], location[1], location[2], ((float[]) stackPix[location[2]])[location[0] + jOffset]));
                    if (isMax(location, dims, radii) && ((float[]) stackPix[k])[i + jOffset] >= maxThresh) {
                        addMaxima(location, jOffset);
                    }
                }
            }
        }
    }

    synchronized void addMaxima(int[] location, int offset) {
        maxima.add(location);
    }

    boolean isMax(int[] location, int[] dims, int[] radii) {
        double max = -Double.MAX_VALUE;
        for (int k = location[2] - radii[2]; k <= location[2] + radii[2]; k++) {
            int z = k;
            if (k < 0) {
                z = 0;
            }
            if (k >= dims[2]) {
                z = dims[2] - 1;
            }
            for (int j = location[1] - radii[1]; j <= location[1] + radii[1]; j++) {
                int y = j;
                if (j < 0) {
                    y = 0;
                }
                if (j >= dims[1]) {
                    y = dims[1] - 1;
                }
                int yOffset = y * dims[0];
                for (int i = location[0] - radii[0]; i <= location[0] + radii[0]; i++) {
                    int x = i;
                    if (i < 0) {
                        x = 0;
                    }
                    if (i >= dims[0]) {
                        x = dims[0] - 1;
                    }
                    if (location[0] == x && location[1] == y && location[2] == z) {
                        continue;
                    }
                    float current = ((float[]) stackPix[z])[x + yOffset];
                    if (current > max) {
                        max = current;
                    }
                }
            }
        }
        return ((float[]) stackPix[location[2]])[location[0] + location[1] * dims[0]] >= max;
    }
}
