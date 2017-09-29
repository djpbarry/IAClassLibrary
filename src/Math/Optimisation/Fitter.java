/*
 * Copyright (C) 2015 David Barry <david.barry at cancer.org.uk>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package Math.Optimisation;

public abstract class Fitter {

    protected final double alpha; // reflection coefficient
    protected final double gamma; // expansion coefficient
    protected final double beta; // contraction coefficient
    protected final double maxError; // maximum error tolerance
    protected static final double root2 = Math.pow(2.0, 0.5); // square root of 2
    public static final int IterFactor = 500;
    protected static int defaultRestarts = 2; // default number of restarts
    protected double[] xData;
    protected double[] yData;
    protected double[] zData; // x,y,z data to fit
    protected int numIter; // number of iterations so far
    protected int numParams; // number of parametres
    protected int numVertices; // numParams+1 (includes sumLocalResiduaalsSqrd)
    protected double[] next; // new vertex to be tested
    protected double[][] simp; // the simplex (the last element of the array at each vertice is the sum of the square of the residuals)
    protected int worst; // worst current parametre estimates
    protected int best; // best current parametre estimates
    protected int maxIter; // maximum number of iterations per restart
    protected int nextWorst; // 2nd worst current parametre estimates
    protected int restarts; // number of times to restart simplex after first soln.
    protected int nRestarts; // the number of restarts that occurred
    protected int numPoints; // number of data points

    public Fitter() {
        this.alpha = -1.0; // reflection coefficient
        this.gamma = 2.0; // expansion coefficient
        this.beta = 0.5; // contraction coefficient
        this.maxError = 1.0E-10; // maximum error tolerance
    }

    public Fitter(double alpha, double gamma, double beta, double maxError) {
        this.alpha = alpha; // reflection coefficient
        this.gamma = gamma; // expansion coefficient
        this.beta = beta; // contraction coefficient
        this.maxError = maxError; // maximum error tolerance
    }

    public boolean doFit() {
        if (xData == null || yData == null || zData == null) {
            return false;
        }
        initialize();
        restart(0);
        numIter = 0;
        boolean done = false;
        double[] center = new double[numParams]; // mean of simplex vertices
        while (!done) {
            showProgress(numIter + maxIter * (defaultRestarts - restarts), maxIter * defaultRestarts);
//            for (double d : simp[best]) {
//                System.out.print(String.format("%f, ", d));
//            }
//            System.out.println();
//            System.out.println("x= " + simp[best][0] + "; y= " + simp[best][1]
//                    + "; r= " + simp[best][2]);
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
            double rtol = 2 * Math.abs(simp[best][numParams] - simp[worst][numParams]) / (Math.abs(simp[best][numParams]) + Math.abs(simp[worst][numParams]) + 1.0E-10);
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
        return true;
    }

    /**
     * Initialise the simplex
     */
    abstract boolean initialize();

    /**
     * Restart the simplex at the nth vertex
     */
    boolean restart(int n) {
        if (simp == null || n >= simp.length) {
            return false;
        }
        // Copy nth vertice of simplex to first vertice
        System.arraycopy(simp[n], 0, simp[0], 0, numParams);
        sumResiduals(simp[0]); // Get sum of residuals^2 for first vertex
        double[] step = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            step[i] = simp[0][i] / 2.0; // Step half the parametre value
            if (step[i] == 0.0) {
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
     * Adds sum of square of residuals to end of array of parameters
     */
    boolean sumResiduals(double[] x) {
        if (x == null) {
            return false;
        }
        /*
         * x[numParams] = sumResiduals(x, xData, yData, zData); return true;
         */
//        for (double d : x) {
//            System.out.print(String.format("%f, ", d));
//        }
//        System.out.println();
        double e;
        x[numParams] = 0.0;
        for (int i = 0; i < xData.length; i++) {
            for (int j = 0; j < yData.length; j++) {
                e = evaluate(x, xData[i], yData[j]) - zData[j * xData.length + i];
//                System.out.println(String.format("%f", evaluate(x, xData[i], yData[j])));
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

    public abstract double evaluate(double[] vector, double... params);

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

    /*
     * Last "parametre" at each vertex of simplex is sum of residuals for the
     * curve described by that vertex
     */
    public double getSumResidualsSqr() {
        double[] params = getParams();
        if (params != null) {
            return params[numParams];
        } else {
            return Double.NaN;
        }
    }

    protected void showProgress(int current, int max) {

    }
}
