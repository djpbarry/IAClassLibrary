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
    private final boolean varyBG, absolute;
    private final float maxThresh;
    private final ArrayList<int[]> maxima;
    private final int[] location;
    private final int[] dims;
    private final int[] radius;

    public RunnableMaximaFinder(Object[] stackPix, boolean varyBG, boolean absolute, float maxThresh, ArrayList<int[]> maxima, int[] location, int[] dims, int[] radius) {
        this.stackPix = stackPix;
        this.varyBG = varyBG;
        this.absolute = absolute;
        this.maxThresh = maxThresh;
        this.maxima = maxima;
        this.location = location;
        this.dims = dims;
        this.radius = radius;
    }

    public void run() {
        double min, max;
        int j;
        for (min = Double.MAX_VALUE, max = 0.0, j = location[1] - radius[0]; j <= location[1] + radius[0]; j++) {
            if (j < 0 || j >= dims[1]) {
                continue;
            }
            int jOffset = j * dims[0];
            for (int k = location[2] - radius[2]; k <= location[2] + radius[2]; k++) {
                if (k < 0 || k >= dims[2]) {
                    continue;
                }
                for (int i = location[0] - radius[0]; i <= location[0] + radius[0]; i++) {
                    if (i < 0 || i >= dims[0]) {
                        continue;
                    }
                    float current = ((float[]) stackPix[k])[i + jOffset];
                    if ((current > max) && !((location[0] == i) && (location[1] == j))) {
                        max = current;
                    }
                    if ((current < min) && !((location[0] == i) && (location[1] == j))) {
                        min = current;
                    }
                }
            }
        }
        double pix = ((float[]) stackPix[location[2]])[location[0] + location[1] * dims[0]];
        double diff = varyBG ? pix : pix - min;
        if ((absolute ? pix > max : pix >= max) && (diff > maxThresh)) {
            maxima.add(new int[]{location[0], location[1], location[2]});
        }
    }

}
