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
package Image;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.TypeConverter;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class ImageChecker {

    /**
     *
     * @param binaryImage
     */
    public static ByteProcessor checkBinaryImage(ImageProcessor image) {
        image.resetMinAndMax();
        ByteProcessor binaryImage = (ByteProcessor) (new TypeConverter(image, true)).convertToByte();
        binaryImage.autoThreshold();
        if (binaryImage.isInvertedLut()) {
            binaryImage.invertLut();
        }
        ImageStatistics stats = binaryImage.getStatistics();
        if (stats.histogram[0] > stats.histogram[255]) {
            binaryImage.invert();
        }
        return binaryImage;
    }
    
}
