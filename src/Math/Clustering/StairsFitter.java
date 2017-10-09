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
package Math.Clustering;

import IO.DataReader;
import ij.ImagePlus;
import ij.gui.Plot;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
public class StairsFitter {

    private ImagePlus plot;
    private List<CentroidCluster<Clusterable>> bestClusters;

    public StairsFitter() {

    }

//    public static void main(String[] args) {
//        double[][] data = null;
//        try {
//            data = DataReader.readFile(new File("C:\\Users\\barryd\\OneDrive - The Francis Crick Institute\\Working Data\\Yardimici\\Dominika\\test.csv"), CSVFormat.EXCEL);
//        } catch (Exception e) {
//
//        }
//        int N = data[0].length;
//        double[] xVals = data[0];
//        double[] yVals = new double[N];
//        for (int i = 0; i < N; i++) {
//            yVals[i] = data[1][i] * 100.0;
//        }
//        (new StairsFitter()).doFit(xVals, yVals, new int[]{2, 9}, 5.0);
//        System.exit(0);
//    }

    public boolean doFit(double[] xVals, double[] yVals, int[] range, double threshold) {
        ArrayList<Clusterable> clusterInput = new ArrayList();
        int N = xVals.length;
        boolean fitted = false;
        if (yVals.length != N) {
            return false;
        }
        for (int i = 0; i < N; i++) {
            clusterInput.add(new ClusterablePoint(new double[]{xVals[i], yVals[i]}));
        }
        Random r = new Random();
        double minSD = Double.MAX_VALUE;
        int bestIndex = -1;
//        List<ImagePlus> imps = new ArrayList();
        for (int p = range[0]; p < range[1]; p++) {
            KMeansPlusPlusClusterer<Clusterable> kmeans = new KMeansPlusPlusClusterer(p, -1, new YDist());
            MultiKMeansPlusPlusClusterer<Clusterable> multiCluster = new MultiKMeansPlusPlusClusterer(kmeans, 100, new ClusterablePointScore());
            List<CentroidCluster<Clusterable>> clusters = multiCluster.cluster(clusterInput);
//            imps.add(plot);
//            System.out.println(String.format("Parameters: %d", p));
            double sd = calcInterClusterSpread(clusters, threshold);
            if (!Double.isNaN(sd) && sd < minSD) {
                minSD = sd;
                bestIndex = p;
                bestClusters = clusters;
                plot = showPlot(clusters, String.format("%d Clusters", p));
                fitted = true;
            }
        }
//        System.out.println(String.format("Best: %d", bestIndex));
//        imps.forEach((imp) -> {
//            imp.close();
//        });
        return fitted;
    }

    public static ImagePlus showPlot(List<CentroidCluster<Clusterable>> clusters, String title) {
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
        ImagePlus output = plot.makeHighResolution(null, 4.0f, true, false);
        return output;
    }

    double calcInterClusterSpread(List<CentroidCluster<Clusterable>> clusters, double threshold) {
        int N = clusters.size();
        double[] yCoords = new double[N];
        double[] xCoords = new double[N];
        for (int i = 0; i < N; i++) {
            CentroidCluster c = clusters.get(i);
            xCoords[i] = c.getCenter().getPoint()[0];
            yCoords[i] = c.getCenter().getPoint()[1];

        }
        Arrays.sort(yCoords);
        Arrays.sort(xCoords);
        for (int j = 0; j < N - 1; j++) {
//            System.out.println("x1:" + xCoords[j] + " y1:" + yCoords[j] + "x2:" + xCoords[j + 1] + " y2:" + yCoords[j + 1]);
            if (yCoords[j + 1] - yCoords[j] < threshold
                    || xCoords[j + 1] - xCoords[j] < threshold) {
                return Double.NaN;
            }
        }
        return (new ClusterablePointScore()).score(clusters);
    }

    private static class YDist implements DistanceMeasure {

        public double compute(double[] a, double[] b) {
            return Math.abs(a[1] - b[1]);
        }
    }

    public ImagePlus getPlot() {
        return plot;
    }

    public List<CentroidCluster<Clusterable>> getBestClusters() {
        return bestClusters;
    }

}
