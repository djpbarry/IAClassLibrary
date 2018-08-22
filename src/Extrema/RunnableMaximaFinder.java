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

    private final float[][] stackPix;
    private final boolean varyBG, absolute;
    private final float maxThresh;
    private final ArrayList<int[]> maxima;
    private final int[] location;
    private final int[] dims;
    private final int[] radius;

    public RunnableMaximaFinder(float[][] stackPix, boolean varyBG, boolean absolute, float maxThresh, ArrayList<int[]> maxima, int[] location, int[] dims, int[] radius, String name) {
        super(name);
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
            int jOffset = j * dims[0];
            for (int k = location[2] - radius[2]; k <= location[2] + radius[2]; k++) {
                for (int i = location[0] - radius[0]; i <= location[0] + radius[0]; i++) {
                    if (location[0] == i && location[1] == j && location[2] == k) {
                        continue;
                    }
                    float current = stackPix[k][i + jOffset];
                    if (current > max) {
                        max = current;
                    }
                    if (current < min) {
                        min = current;
                    }
                }
            }
        }
        double pix = stackPix[location[2]][location[0] + location[1] * dims[0]];
        double diff = varyBG ? pix : pix - min;
        if ((absolute ? pix > max : pix >= max) && (diff > maxThresh)) {
            maxima.add(new int[]{location[0], location[1], location[2]});
        }
    }

}
