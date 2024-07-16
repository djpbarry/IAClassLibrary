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
package net.calm.iaclasslibrary.Curvature;

import net.calm.iaclasslibrary.IAClasses.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class CurveAnalyser {

    public static double[] calcCurvature(int[][] pix, int step, boolean loop, ArrayList<ArrayList<Double>> cumulativeCurveStats) {

//        int index = 0;
        int n = pix.length;
        double[] curvature = new double[n];
        if (n < step) {
            Arrays.fill(curvature, 0.0);
        } else {
            for (int j = 0; j < n; j++) {
//                ByteProcessor bp = new ByteProcessor(1010, 1010);
//                bp.setColor(255);
//                bp.fill();
//                bp.setColor(0);
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
//                bp.drawLine(pix[j][0], pix[j][1], pix[k][0], pix[k][1]);
//                bp.drawLine(pix[j][0], pix[j][1], pix[i][0], pix[i][1]);
//                IJ.saveAs(new ImagePlus("", bp), "PNG", "C:\\Users\\barryd\\debugging\\anamorf_debug\\curve_" + index++);
                curvature[j] = calculateMengerCurvature(new Point(pix[j][0], pix[j][1]),
                new Point(pix[i][0], pix[i][1]), new Point(pix[k][0], pix[j][k]));
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
                if (cumulativeCurveStats != null) {
                    cumulativeCurveStats.get(0).add(new Double(pix[j][0]));
                    cumulativeCurveStats.get(1).add(new Double(pix[j][1]));
                    cumulativeCurveStats.get(2).add(theta1);
                    cumulativeCurveStats.get(3).add(theta2);
                }
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
        return calcCurvature(intPix, step, true, null);
    }

    public static double calculateMengerCurvature(Point P1, Point P2, Point P3) {
        // Calculate side lengths
        double a = P1.distance(P2);
        double b = P1.distance(P3);
        double c = P2.distance(P3);

        // Calculate semi-perimeter
        double s = (a + b + c) / 2.0;

        // Calculate the area using Heron's formula
        double area = Math.sqrt(s * (s - a) * (s - b) * (s - c));

        // Circumcircle radius
        double R = (a * b * c) / (4.0 * area);

        // Menger curvature
        double curvature = 1.0 / R;

        return curvature;
    }

}
