package Optimisation;

import Optimisation.Fitter;

/**
 * 2D Gaussian Curve Fitter based on ImageJ's <code>CurveFitter</code>.
 *
 * TODO Compare with Schleich et al. background level for all fits
 *
 * @author David J Barry
 * @version 1.0, JAN 2011
 */
public class IsoGaussianFitter extends Fitter {

    //private native double evaluate(double x, double y, double p0, double p1,            double p2, double p3, double p4);
    // private native double sumResiduals(double x[], double xData[], double yData[], double zData[]);
    public static final int IterFactor = 500;
    private static final double alpha = -1.0;     // reflection coefficient
    private static final double beta = 0.5;   // contraction coefficient
    private static final double gamma = 2.0;      // expansion coefficient
    private static final double root2 = 1.414214; // square root of 2
    protected double[] xData, yData, zData;  // x,y,z data to fit
    protected int numPoints;          // number of data points
    protected int numParams;          // number of parametres
    protected int numVertices;        // numParams+1 (includes sumLocalResiduaalsSqrd)
    private int worst;          // worst current parametre estimates
    private int nextWorst;      // 2nd worst current parametre estimates
    protected int best;           // best current parametre estimates
    protected double[][] simp;        // the simplex (the last element of the array at each vertice is the sum of the square of the residuals)
    protected double[] next;      // new vertex to be tested
    private int numIter;        // number of iterations so far
    protected int maxIter;    // maximum number of iterations per restart
    protected int restarts;   // number of times to restart simplex after first soln.
    protected static int defaultRestarts = 2;  // default number of restarts
    protected int nRestarts;  // the number of restarts that occurred
    private static double maxError = 1e-10;    // maximum error tolerance
    private double x0, y0, mag, sigEst;
    private boolean floatingSigma;

//    public static void main(String args[]) {
//        ImagePlus imp = new ImagePlus("C:\\Users\\Dave\\Desktop\\lac.tif");
//        int width = imp.getWidth();
//        int height = imp.getHeight();
//        double xvals[] = new double[width];
//        double yvals[] = new double[height];
//        double zvals[][] = new double[width][height];
//        ImageProcessor ip = imp.getProcessor();
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                xvals[x] = x;
//                yvals[y] = y;
//                zvals[x][y] = ip.getPixelValue(x, y);
//            }
//        }
//        IsoGaussianFitter igf = new IsoGaussianFitter(xvals, yvals, zvals);
//        igf.doFit();
//        double params[] = igf.getParams();
//        /*
//         * System.out.println("MinZ: " + params[0]); System.out.println("MaxZ: "
//         * + params[1]); System.out.println("X: " + params[2]);
//         * System.out.println("Y: " + params[3]); System.out.println("Sigma: " +
//         * params[4]);
//         */
//        System.out.println("alpha: " + params[0]);
//        System.out.println("a: " + params[1]);
//        System.out.println("b: " + params[2]);
//        System.out.println("R^2: " + igf.getRSquared());
//
//        System.exit(0);
//    }
//    public static void main(String args[]) {
//        int iterations = 100000000;
//        int dim = (int) Math.sqrt(iterations);
//        double[] p = {0.0, 1.0, dim / 2.0, dim / 2.0, dim / 5.0};
//        double results[] = new double[iterations];
//        IsoGaussianFitter fitter = new IsoGaussianFitter();
//        double startTime = System.currentTimeMillis();
//        for (int x = 0; x < dim; x++) {
//            for (int y = 0; y < dim; y++) {
//                results[x + y * dim] = fitter.evaluate(p, x, y);
//            }
//        }
//        System.out.println("Analysis Time: " + (System.currentTimeMillis() - startTime) + " ms");
//    }
    public IsoGaussianFitter() {
    }

    /**
     * Construct a new CurveFitter.
     */
    public IsoGaussianFitter(double[] xVals, double[] yVals, double[][] zVals, boolean floatingSigma) {
        this.xData = xVals;
        this.yData = yVals;
        this.zData = new double[xData.length * yData.length];
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                double z = zVals[x][y];
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

    public boolean doFit(double xySigEst) {
        if (xData == null || yData == null || zData == null) {
            return false;
        }
        initialize(xySigEst);
        restart(0);

        numIter = 0;
        boolean done = false;
        double[] center = new double[numParams];  // mean of simplex vertices
        while (!done) {
//            System.out.println("x1= " + simp[best][0] + "; s1= " + simp[best][1]
//                    + "; x2= " + simp[best][2] + "; s2= " + simp[best][3] + "; y1= "
//                    + simp[best][4] + "; s3= " + simp[best][5] + "; A= " + simp[best][6]
//                    + "; a= " + simp[best][7] + ";");
            numIter++;
            for (int i = 0; i < numParams; i++) {
                center[i] = 0.0;
            }
            // get mean "center" of vertices, excluding worst
            for (int i = 0; i < numVertices; i++) {
                if (i != worst) {
                    for (int j = 0; j < numParams; j++) {
                        center[j] += simp[i][j];
                    }
                }
            }
            // Reflect worst vertex through centre
            for (int i = 0; i < numParams; i++) {
                center[i] /= numParams;
                next[i] = center[i] + alpha * (simp[worst][i] - center[i]);
            }
            sumResiduals(next);
            // if it's better than the best...
            if (next[numParams] <= simp[best][numParams]) {
                newVertex();
                // try expanding it
                for (int i = 0; i < numParams; i++) {
                    next[i] = center[i] + gamma * (simp[worst][i] - center[i]);
                }
                sumResiduals(next);
                // if this is even better, keep it
                if (next[numParams] <= simp[worst][numParams]) {
                    newVertex();
                }
            } // else if better than the 2nd worst keep it...
            else if (next[numParams] <= simp[nextWorst][numParams]) {
                newVertex();
            } // else try to make positive contraction of the worst
            else {
                for (int i = 0; i < numParams; i++) {
                    next[i] = center[i] + beta * (simp[worst][i] - center[i]);
                }
                sumResiduals(next);
                // if this is better than the second worst, keep it.
                if (next[numParams] <= simp[nextWorst][numParams]) {
                    newVertex();
                } // if all else fails, contract simplex in on best
                else {
                    for (int i = 0; i < numVertices; i++) {
                        if (i != best) {
                            for (int j = 0; j < numVertices; j++) {
                                simp[i][j] = beta * (simp[i][j] + simp[best][j]);
                            }
                            sumResiduals(simp[i]);
                        }
                    }
                }
            }
            order();

            double rtol = 2 * Math.abs(simp[best][numParams] - simp[worst][numParams])
                    / (Math.abs(simp[best][numParams]) + Math.abs(simp[worst][numParams]) + 0.0000000001);

            if (numIter >= maxIter) {
                done = true;
            } else if (rtol < maxError) {
                restarts--;
                if (restarts < 0) {
                    done = true;
                } else {
                    restart(best);
                }
            }
        }
        mag = simp[best][1];
        x0 = simp[best][2];
        y0 = simp[best][3];

        return true;
    }

    /**
     * Initialise the simplex
     */
    boolean initialize(double xySigEst) {
        if (xData == null || yData == null || zData == null) {
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
        simp[0][0] = minz; // a
        simp[0][1] = maxz; // b
        simp[0][2] = xmean;          // c
        simp[0][3] = ymean; // d
        if (floatingSigma) {
            simp[0][4] = xySigEst;
        } else {
            sigEst = xySigEst;
        }
        return true;
    }

    /**
     * Restart the simplex at the nth vertex
     */
    boolean restart(int n) {
        if (simp == null || n >= simp.length) {
            return false;
        }
        // Copy nth vertice of simplex to first vertice
        System.arraycopy(simp[n], 0, simp[0], 0, numParams);
        sumResiduals(simp[0]);          // Get sum of residuals^2 for first vertex
        double[] step = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            step[i] = simp[0][i] / 2.0;     // Step half the parametre value
            if (step[i] == 0.0) // We can't have them all the same or we're going nowhere
            {
                step[i] = 0.01;
            }
        }
        // Some kind of factor for generating new vertices
        double[] p = new double[numParams];
        double[] q = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            p[i] = step[i] * (Math.sqrt(numVertices) + numParams - 1.0) / (numParams * root2);
            q[i] = step[i] * (Math.sqrt(numVertices) - 1.0) / (numParams * root2);
        }
        // Create the other simplex vertices by modifing previous one.
        for (int i = 1; i < numVertices; i++) {
            for (int j = 0; j < numParams; j++) {
                simp[i][j] = simp[i - 1][j] + q[j];
            }
            simp[i][i - 1] = simp[i][i - 1] + p[i - 1];
            sumResiduals(simp[i]);
        }
        // Initialise current lowest/highest parametre estimates to simplex 1
        best = 0;
        worst = 0;
        nextWorst = 0;
        order();
        nRestarts++;
        return true;
    }

    /**
     * Returns 'fit' formula value for parameters "p" at "x"
     */
    public double evaluate(double[] p, double x, double y) {
        if (p == null) {
            return Double.NaN;
        }
        double sig = sigEst;
        if (floatingSigma) {
            sig = p[4];
        }
        return p[0] + p[1] * Math.exp(-(((x - p[2]) * (x - p[2])) / (2 * sig * sig) + ((y - p[3])
                * (y - p[3])) / (2 * sig * sig)));
    }

    /**
     * Returns residuals array ie. differences between data and curve.
     */
    public double[] getResiduals() {
        if (!(numPoints > 0)) {
            return null;
        }
        double[] params = getParams();
        double[] residuals = new double[numPoints];
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                residuals[x * y] = zData[x + xData.length * y] - evaluate(params, xData[x], yData[y]);
            }
        }
        return residuals;
    }

    /**
     * Returns R<sup>2</sup>, where 1.0 is best.<br> <br> R<sup>2</sup> = 1.0 -
     * SSE/SSD<br> <br> where SSE is the sum of the squares of the errors and
     * SSD is the sum of the squares of the deviations about the mean.
     */
    public double getRSquared() {
        if (numPoints < 1) {
            return Double.NaN;
        }
        double sumZ = 0.0;
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                sumZ += zData[x + xData.length * y];
            }
        }
        double mean = sumZ / numPoints;
        double sumMeanDiffSqr = 0.0;
        for (int x = 0; x < xData.length; x++) {
            for (int y = 0; y < yData.length; y++) {
                sumMeanDiffSqr += Math.pow(zData[x + xData.length * y] - mean, 2);
            }
        }
        double rSquared = 0.0;
        if (sumMeanDiffSqr > 0.0) {
            double srs = getSumResidualsSqr();
            rSquared = 1.0 - srs / sumMeanDiffSqr;
        }
        return rSquared;
    }

    /**
     * Adds sum of square of residuals to end of array of parameters
     */
    boolean sumResiduals(double[] x) {
        if (x == null) {
            return false;
        }
        /*
         * x[numParams] = sumResiduals(x, xData, yData, zData); return true;
         */
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

    /**
     * Keep the "next" vertex
     */
    boolean newVertex() {
        if (next == null) {
            return false;
        }
        System.arraycopy(next, 0, simp[worst], 0, numVertices);
        return true;
    }

    /**
     * Find the worst, nextWorst and best current set of parameter estimates
     */
    void order() {
        for (int i = 0; i < numVertices; i++) {
            if (simp[i][numParams] < simp[best][numParams]) {
                best = i;
            }
            if (simp[i][numParams] > simp[worst][numParams]) {
                worst = i;
            }
        }
        nextWorst = best;
        for (int i = 0; i < numVertices; i++) {
            if (i != worst) {
                if (simp[i][numParams] > simp[nextWorst][numParams]) {
                    nextWorst = i;
                }
            }
        }
    }

    /**
     * Get the set of parameter values from the best corner of the simplex
     */
    public double[] getParams() {
        order();
        if (simp != null) {
            return simp[best];
        } else {
            return null;
        }
    }

    /*
     * Last "parametre" at each vertex of simplex is sum of residuals for the
     * curve described by that vertex
     */
    public double getSumResidualsSqr() {
        double[] params = getParams();
        if (params != null) {
            return params[getNumParams()];
        } else {
            return Double.NaN;
        }
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
