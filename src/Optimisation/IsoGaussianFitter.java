package Optimisation;

/**
 * 2D Gaussian Curve Fitter based on ImageJ's <code>CurveFitter</code>.
 *
 * TODO Compare with Schleich et al. background level for all fits
 *
 * @author David J Barry
 * @version 1.0, JAN 2011
 */
public class IsoGaussianFitter extends Fitter {

    protected double x0, y0, mag, sigEst;
    private boolean floatingSigma;

    public IsoGaussianFitter() {
    }

    /**
     * Construct a new CurveFitter.
     */
    public IsoGaussianFitter(double[] xVals, double[] yVals, double[][] zVals, boolean floatingSigma, double sigEst) {
        this.xData = xVals;
        this.yData = yVals;
        this.zData = new double[xData.length * yData.length];
        this.sigEst = sigEst;
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                this.zData[y * xData.length + x] = zVals[x][y];
            }
        }
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
        this.floatingSigma = floatingSigma;
        if (floatingSigma) {
            numParams = 5;
        } else {
            numParams = 4;
        }
    }

    boolean initialize() {
        if (xData == null || yData == null || zData == null) {
            return false;
        }
        // Calculate some things that might be useful for predicting parametres
        numVertices = numParams + 1; // need 1 more vertice than parametres,
        simp = new double[numVertices][numVertices];
        next = new double[numVertices];
        double firstx = xData[0];
        double firsty = yData[0];
        double firstz = zData[0];
        double lastx = xData[xData.length - 1];
        double lasty = yData[yData.length - 1];
        double xmean = (firstx + lastx) / 2.0;
        double ymean = (firsty + lasty) / 2.0;
        double minz = firstz;
        double maxz = firstz;
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
        maxIter = IterFactor * numParams * numParams; // Where does this estimate come from?
        restarts = defaultRestarts;
        nRestarts = 0;
        simp[0][0] = minz; // a
        simp[0][1] = maxz; // b
        simp[0][2] = xmean; // c
        simp[0][3] = ymean; // d
        if (floatingSigma) {
            simp[0][4] = sigEst;
        }
        return true;
    }

    /**
     * Returns 'fit' formula value for parameters "p" at "x"
     */
    public double evaluate(double[] p, double... x) {
        if (p == null) {
            return Double.NaN;
        }
        double sig = sigEst;
        if (floatingSigma) {
            sig = p[4];
        }
        return p[0] + p[1] * Math.exp(-(((x[0] - p[2]) * (x[0] - p[2])) / (2 * sig * sig) + ((x[1] - p[3])
                * (x[1] - p[3])) / (2 * sig * sig)));
    }

    public int getNumParams() {
        return numParams;
    }

    public double getMag() {
        return mag;
    }

    public double getX0() {
        return x0;
    }

    public double getXsig() {
        if (!floatingSigma) {
            return sigEst;
        } else {
            return simp[best][4];
        }
    }

    public double getY0() {
        return y0;
    }

    public double getYsig() {
        if (!floatingSigma) {
            return sigEst;
        } else {
            return simp[best][4];
        }
    }

    /*
     * static { System.loadLibrary("libVirusTracker_GaussianFitter"); }
     */
}
