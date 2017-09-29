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

import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class ClusterablePoint extends WeightedObservedPoint implements Clusterable {

    public ClusterablePoint(double weight, double x, double y) {
        super(weight, x, y);
    }

    public ClusterablePoint(double[] point) {
        this(1.0, point[0], point[1]);
    }

    public double[] getPoint() {
        return new double[]{this.getX(), this.getY()};
    }

}
