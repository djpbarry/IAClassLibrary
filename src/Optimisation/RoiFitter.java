/*
 * Copyright (C) 2017 Dave Barry <david.barry at crick.ac.uk>
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
package Optimisation;

import IAClasses.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.awt.Rectangle;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class RoiFitter extends Fitter {

    protected final double alpha = -1.0; // reflection coefficient
    protected final double gamma = 2.0; // expansion coefficient
    protected final double beta = 0.5; // contraction coefficient
    private Roi initRoi;

    public static void main(String args[]) {
        ImagePlus imp = IJ.openImage();
        ImageProcessor ip = imp.getProcessor();
        int width = ip.getWidth();
        int height = ip.getHeight();
        float[] pix = (float[]) ip.getPixels();
        double[] xVals = new double[width];
        double[] yVals = new double[height];
        for (int x = 0; x < width; x++) {
            xVals[x] = x;
        }
        for (int y = 0; y < height; y++) {
            yVals[y] = y;
        }
        OvalRoi roi = new OvalRoi(0.375 * width, 0.375 * height, 0.25 * width, 0.25 * height);
        RoiFitter instance = new RoiFitter(xVals, yVals, pix, roi);
        instance.doFit();
        double[] p = instance.getParams();
        double rw = p[3] * 2.0;
        double x0 = p[0] - p[3];
        double y0 = p[0] - p[3];
        roi = new OvalRoi(x0, y0, rw, rw);
        imp.setRoi(roi);
        imp.show();
        System.exit(0);
    }

    public RoiFitter(double[] xVals, double[] yVals, float[] zVals, Roi initRoi) {
        this.xData = xVals;
        this.yData = yVals;
        this.initRoi = initRoi;
        if (xData != null && yData != null) {
            numPoints = xVals.length * yVals.length;
            for (int i = xVals.length - 1; i >= 0; i--) {
                xData[i] -= xData[0];
                if (i < yData.length) {
                    yData[i] -= yData[0];
                }
            }
        } else {
            numPoints = 0;
        }
        this.zData = new double[zVals.length];
        for (int i = 0; i < zVals.length; i++) {
            zData[i] = zVals[i];
        }
        numParams = 3;
    }

    public double evaluate(double[] params, double... x) {
//        double dist = Utils.calcDistance(params[0], params[1], x[0], x[1]);
//        if (dist < params[2]) {
//            return 0.0;
//        } else {
//            return 1.0;
//        }
        double dist = Math.abs(Utils.calcDistance(params[0], params[1], x[0], x[1]) - params[2]);
        return 1.0 / (1.0 + dist);
    }

    boolean initialize() {
        if (xData == null || yData == null || zData == null) {
            return false;
        }
        // Calculate some things that might be useful for predicting parametres
        numVertices = numParams + 1; // need 1 more vertice than parametres,
        simp = new double[numVertices][numVertices];
        next = new double[numVertices];
        Rectangle bounds = initRoi.getBounds();
        simp[0][2] = bounds.width; // c
        simp[0][0] = bounds.x + bounds.width / 2; // a
        simp[0][1] = bounds.y + bounds.width / 2; // b
        maxIter = IterFactor * numParams * numParams; // Where does this estimate come from?
        restarts = defaultRestarts;
        nRestarts = 0;
        return true;
    }

    boolean sumResiduals(double[] x) {
        if (x == null) {
            return false;
        }

        double e;
        x[numParams] = 0.0;
        for (int i = 0; i < xData.length; i++) {
            for (int j = 0; j < yData.length; j++) {
                e = evaluate(x, xData[i], yData[j]) - zData[j * xData.length + i];
                x[numParams] = x[numParams] + (e * e);
            }
        }
        return true;
    }
}
