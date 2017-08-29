/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Optimisation;

import Optimisation.IsoGaussianFitter;

/**
 *
 * @author barry05
 */
public class NonIsoGaussianFitter extends IsoGaussianFitter {

    public NonIsoGaussianFitter(double[] xVals, double[] yVals, double[][] zVals) {
        super();
        numParams = 7;
        this.xData = xVals;
        this.yData = yVals;
        this.zData = new double[xData.length * yData.length];
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                this.zData[y * xData.length + x] = zVals[x][y];
            }
        }
        if (xData != null && yData != null) {
            numPoints = xVals.length * yVals.length;
            for (int i = xVals.length - 1; i >= 0; i--) {
                xData[i] -= xData[0];
                yData[i] -= yData[0];
            }
        } else {
            numPoints = 0;
        }
    }

    boolean initialize(double sigmaEst) {
        if (sigmaEst <= 0.0 || xData == null || yData == null || zData == null) {
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
        double xmean = (firstx + lastx) / 2.0;
        double ymean = (firsty + lasty) / 2.0;
        double minz = firstz, maxz = firstz;
        for (int x = 1; x < xData.length; x++) {
            for (int y = 1; y < yData.length; y++) {
                if (zData[x + xData.length * y] > maxz) {
                    maxz = zData[x + xData.length * y];
                }
                if (zData[x + xData.length * y] < minz) {
                    minz = zData[x + xData.length * y];
                }
            }
        }
        maxIter = IterFactor * numParams * numParams;  // Where does this estimate come from?
        restarts = defaultRestarts;
        nRestarts = 0;
        simp[0][0] = 0.0;
        simp[0][1] = sigmaEst;
        simp[0][2] = sigmaEst;
        simp[0][3] = maxz;
        simp[0][4] = xmean;
        simp[0][5] = ymean;
        simp[0][6] = minz;

        return true;
    }

    /** Returns 'fit' formula value for parameters "p" at "x" */
    public double evaluate(double[] p, double x, double y) {
        if (p == null) {
            return Double.NaN;
        }
        double d = Math.pow(Math.cos(p[0]), 2.0);
        double e = 1.0 / (2.0 * Math.pow(p[1], 2.0));
        double f = Math.pow(Math.sin(p[0]), 2.0);
        double g = 1.0 / (2.0 * Math.pow(p[2], 2.0));
        double h = Math.sin(2.0 * p[0]) / 2.0;
        // p[0]=theta, p[1]=xSigma, p[2]=ySigma, p[3]=A, p[4]=x0, p[5]=y0, p[6]=offset
        double a = (d * e) + (f * g);
        double b = h * (g - e);
        double c = (d * g) + (f * e);
        return p[6] + p[3] * Math.exp(-(a * Math.pow(x - p[4], 2.0)
                + 2.0 * b * (x - p[4]) * (y - p[5]) + c * Math.pow(y - p[5], 2.0)));
    }
}
