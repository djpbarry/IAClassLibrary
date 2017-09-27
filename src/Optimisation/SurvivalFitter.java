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

import IO.DataReader;
import IO.FileReader;
import UtilClasses.GenVariables;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class SurvivalFitter extends Fitter {

    private double[] zDivs;

    public static void main(String[] args) {
        FileReader fr = new FileReader(1, 1, GenVariables.UTF8);
        File directory = null;
        double[][] data = null;
        try {
            data = DataReader.readFile(new File("C:\\Users\\barryd\\OneDrive - The Francis Crick Institute\\Working Data\\Yardimici\\Dominika\\test.csv"), CSVFormat.EXCEL);
        } catch (Exception e) {

        }
        int N = data[0].length;
        double[] xVals = data[0];
        double[] yVals = {0};
        float[] zVals = new float[N];
        for (int i = 0; i < N; i++) {
            zVals[i] = new Double(data[1][i]).floatValue();
        }
//        ArrayList<ClusterablePoint> clusterInput = new ArrayList();
//
//        for (int i = 0; i < N; i++) {
//            clusterInput.add(new ClusterablePoint(new double[]{xVals[i], zVals[i]}));
//        }
//
//        for (int p = 3; p < 8; p++) {
//            KMeansPlusPlusClusterer<ClusterablePoint> kmeans = new KMeansPlusPlusClusterer(
//                    p, 1000, new DistanceMeasure() {
//                public double compute(double[] a, double[] b) {
//                    return Math.abs(a[1] - b[1]);
//                }
//            }
//            );
//            List<CentroidCluster<ClusterablePoint>> clusters = kmeans.cluster(clusterInput);
//            System.out.println(String.format("Parameters: %d", p));
//            int i = 0;
//            for (CentroidCluster c : clusters) {
//                double[] d = c.getCenter().getPoint();
//                System.out.println(String.format("%d, %f, %f", i++, d[0], d[1]));
//            }
//        }

        for (int p = 7; p < 8; p++) {
            SurvivalFitter sf = new SurvivalFitter(xVals, yVals, zVals, p);
            sf.doFit();
            double[] params = sf.getParams();
            int i = 0;
            System.out.println(String.format("Parameters: %d", p));
            for (double d : params) {
                System.out.println(String.format("%d, %f", i++, d));
            }
        }
        System.exit(0);
    }

    public SurvivalFitter() {

    }

    public SurvivalFitter(double[] xVals, double[] yVals, float[] zVals, int numParams) {
        super(-1.0, 2.0, 0.5, 1.0E-3);
        this.xData = xVals;
        this.yData = yVals;
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
        this.numParams = numParams;
    }

    public double evaluate(double[] p, double... x) {
        double e = errorCheck(p);
        if (e > 0.0) {
            return 1.0 + e;
        }
        if (x[0] > p[numParams - 1]) {
            return zDivs[0];
        } else {
            int i = 0;
            while (i < numParams && p[i] < x[0]) {
                i++;
            }
            return zDivs[numParams - i];
        }
    }

    double errorCheck(double[] p) {
        double err = 0.0;
        if (p[0] < 0.0) {
            err += -p[0];
        }
        if (p[numParams - 1] > xData[xData.length - 1]) {
            err += p[numParams - 1] - xData[xData.length - 1];
        }
        for (int i = 0; i < numParams - 1; i++) {
            if (p[i] > p[i + 1]) {
                err += p[i] - p[i + 1];
            }
        }
        return err;
    }

    boolean initialize() {
        if (xData == null || yData == null || zData == null) {
            return false;
        }
        // Calculate some things that might be useful for predicting parametres
        numVertices = numParams + 1; // need 1 more vertice than parametres,
        simp = new double[numVertices][numVertices];
        next = new double[numVertices];
        maxIter = IterFactor * numParams * numParams; // Where does this estimate come from?
        restarts = defaultRestarts;
        nRestarts = 0;
        for (int i = 0; i < numParams; i++) {
            simp[0][i] = xData[(int) Math.round((i + 1) * xData.length / (numParams + 1))];
        }
        EmpiricalDistribution dist = new EmpiricalDistribution(numParams + 1);
        dist.load(zData);
        List<SummaryStatistics> stats = dist.getBinStats();
        zDivs = new double[numParams + 1];
        for (int i = 1; i < numParams; i++) {
            zDivs[i] = stats.get(i).getMean();
        }
        zDivs[0] = 0.0;
        zDivs[numParams] = 1.0;
        return true;
    }

    private static class ClusterablePoint implements Clusterable {

        private double[] point;

        public ClusterablePoint(double[] point) {
            this.point = point;
        }

        public double[] getPoint() {
            return point;
        }

    }
}
