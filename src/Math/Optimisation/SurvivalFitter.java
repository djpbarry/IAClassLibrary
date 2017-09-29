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

import Math.Clustering.ClusterablePoint;
import Math.Clustering.ClusterablePointScore;
import IO.DataReader;
import IO.FileReader;
import UtilClasses.GenVariables;
import ij.gui.Plot;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.clustering.MultiKMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class SurvivalFitter extends Fitter {

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
            zVals[i] = new Double(data[1][i] * 100.0).floatValue();
        }
        ArrayList<Clusterable> clusterInput = new ArrayList();

        for (int i = 0; i < N; i++) {
            clusterInput.add(new ClusterablePoint(new double[]{xVals[i], zVals[i]}));
        }
        Random r = new Random();
        for (int p = 6; p < 7; p++) {
            KMeansPlusPlusClusterer<Clusterable> kmeans = new KMeansPlusPlusClusterer(p, -1, new YDist());
//            List<CentroidCluster<Clusterable>> clusters = kmeans.cluster(clusterInput);
//            showPlot(clusters, "Pre-Sort");
            MultiKMeansPlusPlusClusterer<Clusterable> multiCluster = new MultiKMeansPlusPlusClusterer(kmeans, 100, new ClusterablePointScore());
            List<CentroidCluster<Clusterable>> clusters = multiCluster.cluster(clusterInput);
//            ZeroSlopeClusterOptimiser<CentroidCluster> optim = new ZeroSlopeClusterOptimiser(clusters);
//            optim.optimise(10000);
            showPlot(clusters, "Post-Sort");
            System.out.println(String.format("Parameters: %d", p));
            int i = 0;
            for (CentroidCluster c : clusters) {
                double[] d = c.getCenter().getPoint();
                System.out.println(String.format("%d, %f, %f", i++, d[0], d[1]));
            }
        }

        for (int p = 3; p < 9; p++) {
            SurvivalFitter sf = new SurvivalFitter(xVals, yVals, zVals, p + 1);
            sf.doFit();
            double[] params = sf.getParams();
//            double[] xCoords = sf.getXCoords(params);
//            double[] yCoords = sf.getYCoords(params);
            for (int i = 0; i < params.length; i++) {
                System.out.print(String.format("%f, ", params[i]));
            }
            System.out.println();
        }
        System.exit(0);
    }

    public static void showPlot(List<CentroidCluster<Clusterable>> clusters, String title) {
        Plot plot = new Plot(title, "X", "Y");
        plot.setLineWidth(10);
        Random r = new Random();
        for (CentroidCluster c : clusters) {
            Color color = new Color(r.nextFloat(), r.nextFloat(), r.nextFloat());
            plot.setColor(color);
            List<ClusterablePoint> points = c.getPoints();
            double[] xpoints = new double[points.size()];
            double[] ypoints = new double[points.size()];
            for (int j = 0; j < points.size(); j++) {
                ClusterablePoint point = points.get(j);
                xpoints[j] = point.getPoint()[0];
                ypoints[j] = point.getPoint()[1];
            }
            plot.addPoints(xpoints, ypoints, Plot.DOT);
        }
        plot.setLimits(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        plot.show();
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
//        double[] xCoords = getXCoords(p);
//        double[] yCoords = getYCoords(p);
        if (x[0] <= p[0]) {
//            return (Math.exp(-p[numParams - 1] * p[0]) + Math.exp(-p[numParams - 1] * 0.0)) / 2.0;
            return 1.0;
        }
        if (x[0] >= p[numParams - 2]) {
//            return (Math.exp(-p[numParams - 1] * p[numParams - 2]) + Math.exp(-p[numParams - 1] * xData[xData.length - 1])) / 2.0;
            return Math.exp(-p[numParams - 1] * p[numParams - 2]);
        }
        int i = 0;
        while (p[i] < x[0]) {
            i++;
        }
//        return (Math.exp(-p[numParams - 1] * p[i]) + Math.exp(-p[numParams - 1] * p[i - 1])) / 2.0;
        return Math.exp(-p[numParams - 1] * p[i - 1]);
    }

    double errorCheck(double[] p) {
        return calcError(getXCoords(p), xData);
    }

    double calcError(double[] coords1, double[] coords2) {
        double err = 0.0;
        if (coords1[0] < 0.0) {
            err += -coords1[0];
        }
        if (coords1[coords1.length - 1] > coords2[coords2.length - 1]) {
            err += coords1[coords1.length - 1] - coords2[coords2.length - 1];
        }
        for (int i = 0; i < coords1.length - 1; i++) {
            if (coords1[i] > coords1[i + 1]) {
                err += coords1[i] - coords1[i + 1];
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
//        double[] xCoords = new double[numParams / 2];
//        double[] yCoords = new double[numParams / 2];
        for (int i = 0; i < numParams - 1; i++) {
            simp[0][i] = xData[(int) Math.round((i + 1) * xData.length / numParams)];
        }
        simp[0][numParams - 1] = 0.05;
//        EmpiricalDistribution dist = new EmpiricalDistribution(numParams / 2 + 1);
//        dist.load(zData);
//        List<SummaryStatistics> stats = dist.getBinStats();
//        for (int i = 0; i < yCoords.length; i++) {
//            yCoords[i] = stats.get(i).getMean();
//        }
//        simp[0] = setParams(xCoords, yCoords);
        return true;
    }

    private static class YDist implements DistanceMeasure {

        public double compute(double[] a, double[] b) {
            return Math.abs(a[1] - b[1]);
        }
    }

    double[] getXCoords(double[] p) {
        int xLength = numParams - 1;
        double[] xCoords = new double[xLength];
        System.arraycopy(p, 0, xCoords, 0, xLength);
        return xCoords;
    }
//
//    double[] getYCoords(double[] p) {
//        int yLength = numParams / 2;
//        double[] yCoords = new double[yLength];
//        System.arraycopy(p, numParams / 2, yCoords, 0, yLength);
//        return yCoords;
//    }
//
//    double[] setParams(double[] xCoords, double[] yCoords) {
//        double[] output = new double[xCoords.length + yCoords.length + 1];
//        System.arraycopy(xCoords, 0, output, 0, xCoords.length);
//        System.arraycopy(yCoords, 0, output, xCoords.length, yCoords.length);
//        return output;
//    }
}
