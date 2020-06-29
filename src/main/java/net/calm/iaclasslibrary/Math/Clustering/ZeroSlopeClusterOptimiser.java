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
package net.calm.iaclasslibrary.Math.Clustering;

import java.util.List;
import java.util.Random;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class ZeroSlopeClusterOptimiser<T extends CentroidCluster> {

    private List<T> clusters;

    public ZeroSlopeClusterOptimiser(List<T> clusters) {
        this.clusters = clusters;
    }

    public void optimise(int iterations) {
        Random r = new Random();
        int nc = clusters.size();
        for (int i = 0; i < iterations; i++) {
            int c1 = r.nextInt(nc);
//            int c2 = c1;
//            while (c1 == c2) {
//                c2 = r.nextInt(nc);
//            }
            CentroidCluster cluster1 = clusters.get(c1);
            List<Clusterable> points = cluster1.getPoints();
            Clusterable point = points.get(r.nextInt(points.size()));
            CentroidCluster cluster2 = getNearestCluster(c1, point);
            if (evaluateChange(cluster1, cluster2, point)) {
                movePoint(cluster1, cluster2, point);
            }
        }
    }

    public List<T> getClusters() {
        return clusters;
    }

    boolean evaluateChange(Cluster cluster1, Cluster cluster2, Clusterable point) {
        double mc1 = calcStandardDeviation(cluster1);
        double mc2 = calcStandardDeviation(cluster2);
        movePoint(cluster1, cluster2, point);
        double mc12 = calcStandardDeviation(cluster1);
        double mc22 = calcStandardDeviation(cluster2);
        movePoint(cluster2, cluster1, point);
        return Math.abs(mc12) < Math.abs(mc1) && Math.abs(mc22) < Math.abs(mc2);
    }

    void movePoint(Cluster cluster1, Cluster cluster2, Clusterable point) {
        cluster1.getPoints().remove(point);
        cluster2.addPoint(point);
    }

    double calculateSlope(Cluster cluster) {
        List points = cluster.getPoints();
        if (points == null || !(points.get(0) instanceof WeightedObservedPoint)) {
            return Double.NaN;
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        return fitter.fit(cluster.getPoints())[1];
    }

    double calcStandardDeviation(Cluster cluster) {
        List<Clusterable> points = cluster.getPoints();
        if (points == null) {
            return Double.NaN;
        }
        DescriptiveStatistics statsY = new DescriptiveStatistics();
        for (Clusterable c : points) {
            statsY.addValue(c.getPoint()[1]);
        }
//        DescriptiveStatistics statsX = new DescriptiveStatistics();
//        for(Clusterable c:points){
//            statsX.addValue(c.getPoint()[1]);
//        }
        return statsY.getStandardDeviation();
    }

    CentroidCluster getNearestCluster(int clusterIndex, Clusterable point) {
        int minDist = Integer.MAX_VALUE;
        int minIndex = -1;
        for (int i = 0; i < clusters.size(); i++) {
            CentroidCluster c = clusters.get(i);
            if (i != clusterIndex) {
                Clusterable centre = c.getCenter();
                int dist = (int) Math.abs(centre.getPoint()[0] - point.getPoint()[0]);
                if (dist < minDist) {
                    minDist = dist;
                    minIndex = i;
                }
            }
        }
        return clusters.get(minIndex);
    }
}
