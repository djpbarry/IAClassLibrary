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
package Math.Optimisation;

import ij.IJ;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.ImageProcessor;
import java.awt.Rectangle;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class RoiFitter extends Fitter {

    private Roi initRoi;
//    private double 
//    private final boolean ring;

    public RoiFitter(double[] xVals, double[] yVals, float[] zVals, Roi initRoi) {
        super(-1.0, 2.0, 0.5, 1.0E-3);
//        this.ring = ring;
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
        FloatProcessor fproc = new FloatProcessor(xData.length, yData.length, zData);
        double x0 = params[0] - params[2];
        double y0 = params[1] - params[2];
        double w = 2.0 * params[2];
        if (w <= 0.0) {
            return -Double.MAX_VALUE;
        }
        OvalRoi roi = new OvalRoi(x0, y0, w, w);
        if (x0 < 0.0
                || x0 + w >= xData.length
                || y0 < 0.0
                || y0 + w >= yData.length) {
            return Math.PI * params[2] * params[2] - areaInsideImage(roi, fproc);
        }
        FloatPolygon fpoly = roi.getFloatPolygon();
        int n = fpoly.npoints;
        float[] xpoints = fpoly.xpoints;
        float[] ypoints = fpoly.ypoints;
        double perimSum = 0.0;
        fproc.setInterpolationMethod(ImageProcessor.BILINEAR);
        for (int i = 0; i < n; i++) {
            perimSum += fproc.getInterpolatedPixel(xpoints[i], ypoints[i]);
        }
        fproc.setRoi(roi);
        FloatStatistics stats = new FloatStatistics(fproc, Measurements.MEAN + Measurements.AREA, null);
//        System.out.println(String.format("%f %f %f", perimSum / n, stats.mean, perimSum / n - stats.mean));
        double result = perimSum / n - stats.mean;
        if (Double.isNaN(result)) {
            return -1.0;
        } else {
            return result;
        }
    }

    int areaInsideImage(Roi roi, ImageProcessor image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int sum = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (roi.contains(x, y)) {
                    sum++;
                }
            }
        }
        return sum;
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
        simp[0][2] = bounds.width / 2.0; // c
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
        double e = evaluate(x, null) - 1.0;
        x[numParams] = e * e;
        return true;
    }
    
    protected void showProgress(int current, int max){
        IJ.showStatus("Fitting...");
        IJ.showProgress(current, max);
    }
}
