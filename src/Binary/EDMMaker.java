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
package Binary;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.EDM;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class EDMMaker {

    /**
     * Generate a stack of distance maps from an input stack
     *
     * @param binaryInput The binary input stack
     * @return A stack of distance map images
     */
    public static ImageStack makeEDMStack(ImageStack binaryInput) {
        int s = binaryInput.size();
        ImageStack edmStack = new ImageStack(binaryInput.getWidth(), binaryInput.getHeight());
        for (int i = 1; i <= s; i++) {
            edmStack.addSlice(get16BitEDM(binaryInput.getProcessor(i)));
        }
        return edmStack;
    }

    /**
     * Returns a distance map image as a pixel object
     *
     * @param pix The input image pixels
     * @param width Input image width
     * @param height Input image height
     * @return A pixel object, representing a distance map of the input
     */
    public static short[] makeEDMPix(short[] pix, int width, int height) {
        ShortProcessor sp = new ShortProcessor(width, height);
        sp.setPixels(pix);
        return makeEDMPix(sp);
    }

    /**
     * Returns a distance map image as a pixel object
     *
     * @param image The input image
     * @return A distance map pixel object
     */
    public static short[] makeEDMPix(ImageProcessor image) {
        return (short[]) get16BitEDM(image).getPixels();
    }

    /**
     * Returns a distance map image
     *
     * @param image The input image
     * @return A distance map image
     */
    public static ShortProcessor get16BitEDM(ImageProcessor image) {
        ShortProcessor edm = (new EDM()).make16bitEDM(image);
        edm.multiply(1.0 / EDM.ONE);
        return edm;
    }

    /**
     * Converts the input binary image into a voronoi segmentation
     * 
     * @param image Input binary image
     */
    public static void voronoi(ImageProcessor image) {
        EDM edm = new EDM();
        edm.setup("voronoi", new ImagePlus("", image));
        edm.run(image);
        image.threshold(0);
    }

    /**
     * Generates a pixel object representation of a voronoi segmentation
     * 
     * @param pix Input binary image pixels
     * @param width Input image width
     * @param height Input image height
     * @return A pixel object representing a voronoi segmentation
     */
    public static byte[] makeVoronoiPix(short[] pix, int width, int height) {
        ShortProcessor sp = new ShortProcessor(width, height);
        sp.setPixels(pix);
        return makeVoronoiPix(sp);
    }

    /**
     * Generates a pixel object representation of a voronoi segmentation
     * 
     * @param image The input image
     * @return A pixel object representing a voronoi segmentation
     */
    public static byte[] makeVoronoiPix(ImageProcessor image) {
        image.invert();
        voronoi(image);
        return (byte[]) image.getPixels();
    }
}
