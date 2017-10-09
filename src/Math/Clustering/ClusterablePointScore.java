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

import java.util.List;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.evaluation.ClusterEvaluator;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class ClusterablePointScore<T extends Clusterable> extends ClusterEvaluator<T> {

    public ClusterablePointScore() {
        super();
    }

    public ClusterablePointScore(DistanceMeasure measure) {
        super(measure);
    }

    public double score(List<? extends Cluster<T>> clusters) {
        double score = 0.0;
        for (Cluster c : clusters) {
            score += calcVariance(c)[1];
        }
        return score;
    }

    public boolean isBetterScore(double score1, double score2) {
        return score1 < score2;
    }

    public double[] calcVariance(Cluster cluster) {
        List<Clusterable> points = cluster.getPoints();
        if (points == null) {
            return null;
        }
        DescriptiveStatistics statsY = new DescriptiveStatistics();
        DescriptiveStatistics statsX = new DescriptiveStatistics();
        for (Clusterable c : points) {
            statsX.addValue(c.getPoint()[0]);
            statsY.addValue(c.getPoint()[1]);
        }
        return new double[]{statsX.getVariance(), statsY.getVariance()};
    }

    double calculateSlope(Cluster cluster) {
        List points = cluster.getPoints();
        if (points == null || !(points.get(0) instanceof WeightedObservedPoint)) {
            return Double.NaN;
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        return fitter.fit(cluster.getPoints())[1];
    }
}
