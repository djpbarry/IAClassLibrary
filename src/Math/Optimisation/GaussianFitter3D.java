/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Math.Optimisation;

import Math.Optimisation.IsoGaussianFitter;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

/**
 *
 * @author barry05
 */
public class GaussianFitter3D extends IsoGaussianFitter {

    private double[][][] values;
    private double xysig, zsig, xysig2, zsig2;

    public GaussianFitter3D(double[] xVals, double[] yVals, double[] zVals, double[][][] values) {
        super();
        numParams = 5;
        this.xData = xVals;
        this.yData = yVals;
        this.zData = zVals;
        this.values = values;
        if (xData != null && yData != null) {
            numPoints = xVals.length * yVals.length * zVals.length;
            for (int i = xVals.length - 1; i >= 0; i--) {
                xData[i] -= xData[0];
                yData[i] -= yData[0];
                zData[i] -= zData[0];
            }
        } else {
            numPoints = 0;
        }
        ImageStack stack = new ImageStack(xData.length, yData.length);
        for (int z = 0; z < zData.length; z++) {
            ByteProcessor bp = new ByteProcessor(stack.getWidth(), stack.getHeight());
            for (int y = 0; y < yData.length; y++) {
                for (int x = 0; x < xData.length; x++) {
                    bp.putPixel(x, y, (int) values[x][y][z]);
                }
            }
            stack.addSlice(null, bp);
        }
        (new ImagePlus("", stack)).show();
    }

    boolean initialize() {
        if ( xData == null || yData == null || zData == null) {
            return false;
        }
        // Calculate some things that might be useful for predicting parametres
        numVertices = numParams + 1;      // need 1 more vertice than parametres,
        simp = new double[numVertices][numVertices];
        next = new double[numVertices];

        double firstx = xData[0];
        double firsty = yData[0];
        double firstz = zData[0];
        double lastx = xData[xData.length - 1];
        double lasty = yData[yData.length - 1];
        double lastz = zData[zData.length - 1];
        double xmean = (firstx + lastx) / 2.0;
        double ymean = (firsty + lasty) / 2.0;
        double zmean = (firstz + lastz) / 2.0;
        double minval = Double.MAX_VALUE, maxval = -Double.MAX_VALUE;
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                for (int z = 0; z < zData.length; z++) {
                    if (values[x][y][z] > maxval) {
                        maxval = values[x][y][z];
                    }
                    if (values[x][y][z] < minval) {
                        minval = values[x][y][z];
                    }
                }
            }
        }
        maxIter = IterFactor * numParams * numParams;  // Where does this estimate come from?
        restarts = defaultRestarts;
        nRestarts = 0;
        simp[0][0] = minval;
        simp[0][1] = maxval;
        simp[0][2] = xmean;
        simp[0][3] = ymean;
        simp[0][4] = zmean;
        xysig2 = xysig * xysig;
        zsig2 = zsig * zsig;

        return true;
    }

    /** Returns 'fit' formula value for parameters "p" at "x" */
    public double evaluate(double[] p, double x, double y, double z) {
        if (p == null) {
            return Double.NaN;
        }
        /*return p[0] + p[4] * Math.exp(-(Math.pow(x - p[5], 2.0) / (2.0 * p[1] * p[1])
        + Math.pow(y - p[6], 2.0) / (2.0 * p[2] * p[2])
        + Math.pow(x - p[7], 2.0) / (2.0 * p[3] * p[3])));*/
        return p[0] + p[1] * Math.exp(-(Math.pow(x - p[2], 2.0) / (2.0 * xysig2)
                + Math.pow(y - p[3], 2.0) / (2.0 * xysig2)
                + Math.pow(z - p[4], 2.0) / (2.0 * zsig2)));
    }

    public double[] getResiduals() {
        if (!(numPoints > 0)) {
            return null;
        }
        double[] params = getParams();
        double[] residuals = new double[numPoints];
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                for (int z = 0; z < zData.length; z++) {
                    residuals[x * y * z] = values[x][y][z] - evaluate(params, xData[x], yData[y], zData[z]);
                }
            }
        }
        return residuals;
    }

    boolean sumResiduals(double[] x) {
        if (x == null) {
            return false;
        }
        /*x[numParams] = sumResiduals(x, xData, yData, zData);
        return true;*/
        double e;
        x[numParams] = 0.0;
        for (int i = 0; i < xData.length; i++) {
            for (int j = 0; j < yData.length; j++) {
                for (int k = 0; k < zData.length; k++) {
                    e = evaluate(x, xData[i], yData[j], zData[k]) - values[i][j][k];
                    x[numParams] = x[numParams] + (e * e);
                }
            }
        }
        return true;
    }

    public double getRSquared() {
        if (numPoints < 1) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                for (int z = 0; z < zData.length; z++) {
                    sum += values[x][y][z];
                }
            }
        }
        double mean = sum / numPoints;
        double sumMeanDiffSqr = 0.0;
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                for (int z = 0; z < yData.length; z++) {
                    sumMeanDiffSqr += Math.pow(values[x][y][z] - mean, 2.0);
                }
            }
        }
        double rSquared = 0.0;
        if (sumMeanDiffSqr > 0.0) {
            double srs = getSumResidualsSqr();
            rSquared = 1.0 - srs / sumMeanDiffSqr;
        }
        return rSquared;
    }
}
