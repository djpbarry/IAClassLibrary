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
package net.calm.iaclasslibrary.Image;

import ij.measure.Measurements;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.ImageProcessor;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class ImageNormaliser {

    public final static int BYTE = 0, SHORT = 1, FLOAT = 2;

    public static ImageProcessor normaliseImage(ImageProcessor image, double normFactor, int type) {
        FloatProcessor floatImage = image.convertToFloatProcessor();
        FloatStatistics stats = new FloatStatistics(floatImage, Measurements.MIN_MAX, null);
        floatImage.subtract(stats.min);
        floatImage.multiply(normFactor / (stats.max - stats.min));
        floatImage.resetMinAndMax();
        ImageProcessor output;
        switch (type) {
            case BYTE:
                output = floatImage.convertToByteProcessor();
                break;
            case SHORT:
                output = floatImage.convertToShortProcessor();
                break;
            default:
                output = floatImage;
        }
        output.resetMinAndMax();
        return output;
    }
}
