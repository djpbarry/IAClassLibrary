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
package Curvature;

import IAClasses.Utils;
import java.util.Arrays;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class CurveAnalyser {

    public static double[] calcCurvature(int[][] pix, int step, boolean loop) {
        int n = pix.length;
        double[] curvature = new double[n];
        if (n < step) {
            Arrays.fill(curvature, 0.0);
        } else {
            for (int j = 0; j < n; j++) {
                int i = j - step;
                int k = j + step;
                if (!loop && (i < 0 || k >= n)) {
                    continue;
                }
                if (i < 0) {
                    i += n;
                }
                if (k >= n) {
                    k -= n;
                }
                double theta1 = Utils.arcTan(pix[j][0] - pix[i][0], pix[j][1] - pix[i][1]);
                double theta2 = Utils.arcTan(pix[k][0] - pix[j][0], pix[k][1] - pix[j][1]);
                if (Math.abs(theta1 - theta2) >= 180.0) {
                    if (theta2 > theta1) {
                        theta2 -= 360.0;
                    } else {
                        theta1 -= 360.0;
                    }
                }
                curvature[j] = theta1 - theta2;
            }
        }
        if (!loop && n > 2 * step) {
            double[] curvature2 = new double[n - 2 * step];
            System.arraycopy(curvature, step, curvature2, 0, n - 2 * step);
            curvature = curvature2;
        }
        return curvature;
    }

    public static double[] calcCurvature(short[][] pix, int step) {
        int[][] intPix = new int[pix.length][pix[0].length];
        for (int i = 0; i < pix.length; i++) {
            short[] p = pix[i];
            for (int j = 0; j < p.length; j++) {
                intPix[i][j] = p[j];
            }
        }
        return calcCurvature(intPix, step, true);
    }
}
