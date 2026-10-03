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
package net.calm.iaclasslibrary.Math.Optimisation;

import java.util.Arrays;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

public abstract class Fitter {

    protected final double alpha; // reflection coefficient
    protected final double gamma; // expansion coefficient
    protected final double beta; // contraction coefficient
    protected final double maxError; // maximum error tolerance
    protected static final double root2 = Math.sqrt(2.0); // square root of 2
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
        initialize();
        if (simp == null || simp.length == 0) {
            return false;
        }
        double[] start = Arrays.copyOf(simp[0], numParams);

        MultivariateFunction objective = point -> {
            double srs = 0.0;
            for (int i = 0; i < xData.length; i++) {
                for (int j = 0; j < yData.length; j++) {
                    double e = evaluate(point, xData[i], yData[j]) - zData[j * xData.length + i];
                    srs += e * e;
                }
            }
            return srs;
        };

        double[] steps = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            steps[i] = simp[0][i] / 2.0;
            if (steps[i] == 0.0) {
                steps[i] = 0.01;
            }
        }

        SimplexOptimizer optimizer = new SimplexOptimizer(new SimpleValueChecker(maxError, maxError));
        PointValuePair result = optimizer.optimize(
                new ObjectiveFunction(objective),
                new InitialGuess(start),
                new MaxEval(maxIter),
                new NelderMeadSimplex(steps),
                GoalType.MINIMIZE);

        double[] bestPoint = result.getPoint();
        simp = new double[numVertices][numVertices];
        System.arraycopy(bestPoint, 0, simp[0], 0, numParams);
        simp[0][numParams] = result.getValue();
        for (int i = 1; i < numVertices; i++) {
            System.arraycopy(simp[0], 0, simp[i], 0, numVertices);
        }
        best = 0;
        worst = 0;
        nextWorst = 0;
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

    public abstract double evaluate(double[] vector, double... params);

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
