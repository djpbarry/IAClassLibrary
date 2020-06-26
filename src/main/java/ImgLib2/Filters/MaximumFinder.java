/*
 * Copyright (C) 2018 David Barry <david.barry at crick dot ac dot uk>
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
package ImgLib2.Filters;

import java.util.ArrayList;
import java.util.Arrays;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.CenteredRectangleShape;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MaximumFinder {

    /**
     * Checks all pixels in the image if they are a local minima and draws a
     * circle into the output if they are
     *
     * @param source - the image data to work on
     * @param imageFactory - the factory for the output img
     * @return - an Img with circles on locations of a local minimum
     */
    public static < T extends RealType< T>, U extends RealType< U>> Img< BitType> findAndDisplayLocalMaxima(
            RandomAccessibleInterval< T> source, long[] dimensions, T tolerance, int[] radius, boolean absolute) {
        // Create a new image for the output
//        Img< U> output = imageFactory.create(source);
        Img<BitType> output = (new ArrayImgFactory(new BitType())).create(dimensions);

        // define an interval that is one pixel smaller on each side in each dimension,
        // so that the search in the 8-neighborhood (3x3x3...x3) never goes outside
        // of the defined interval
        Interval interval = source;
        for (int r = 0; r < radius.length; r++) {
            interval = Intervals.expand(interval, -radius[r], r);
        }

        // create a view on the source with this interval
        source = Views.interval(source, interval);

        // create a Cursor that iterates over the source and checks in a 8-neighborhood
        // if it is a minima
        final Cursor< T> center = Views.iterable(source).cursor();

        // instantiate a RectangleShape to access rectangular local neighborhoods
        // of radius 1 (that is 3x3x...x3 neighborhoods), skipping the center pixel
        // (this corresponds to an 8-neighborhood in 2d or 26-neighborhood in 3d, ...)
        final CenteredRectangleShape shape = new CenteredRectangleShape(radius, true);

        // iterate over the set of neighborhoods in the image
        for (final Neighborhood< T> localNeighborhood : shape.neighborhoods(source)) {
            // what is the value that we investigate?
            // (the center cursor runs over the image in the same iteration order as neighborhood)
            final T centerValue = center.next();

            // keep this boolean true as long as no other value in the local neighborhood
            // is larger or equal
            boolean isMaximum = true;

            // check if all pixels in the local neighborhood that are smaller
            for (final T value : localNeighborhood) {
                // test if the center is smaller than the current pixel value
                if (centerValue.compareTo(value) <= 0) {
                    isMaximum = false;
                    break;
                }
            }
            Arrays.sort(radius);
            int maxRadius = radius[radius.length-1];
            if (isMaximum && centerValue.getRealFloat() > tolerance.getRealFloat()) {
                // draw a sphere of radius one in the new image
                HyperSphere< BitType> hyperSphere = new HyperSphere<>(output, center, maxRadius);

                // set every value inside the sphere to 1
                for (BitType value : hyperSphere) {
                    value.setOne();
                }
            }
        }
        return output;
    }
}
