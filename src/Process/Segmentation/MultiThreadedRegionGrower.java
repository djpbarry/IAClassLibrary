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
package Process.Segmentation;

import Binary.BinaryMaker;
import Binary.EDMMaker;
import IAClasses.Region;
import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 * Multi-threaded execution of region-growing segmentation
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedRegionGrower extends MultiThreadedProcess {

    ShortProcessor regionImage;
    ImageProcessor inputImage;
    ArrayList<Region> singleImageRegions;
    double threshold;

    /**
     * Constructor method
     *
     * @param regionImage The image on which segmented regions will be drawn
     * @param inputImage The image upon which the segmentation is based
     * @param singleImageRegions The regions to be grown
     * @param threshold The grey level threshold to employ during region growing
     */
    public MultiThreadedRegionGrower(ShortProcessor regionImage, ImageProcessor inputImage, ArrayList<Region> singleImageRegions, double threshold, Properties props) {
        super(null);
        this.regionImage = regionImage;
        this.inputImage = inputImage;
        this.singleImageRegions = singleImageRegions;
        this.threshold = threshold;
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {

    }

    @Override
    public void run() {
        int width = regionImage.getWidth();
        int height = regionImage.getHeight();
        int widthheight = width * height;
        float[] inputPix = (float[]) inputImage.getPixels();
        short[] regionImagePix = (short[]) regionImage.getPixels();
        short[] checkImagePix = new short[widthheight];
        short[] tempRegionPix = new short[widthheight];
        short[] countImagePix = new short[widthheight];
        short[] expandedImagePix = new short[widthheight];
        Arrays.fill(checkImagePix, Region.MASK_FOREGROUND);
        Arrays.fill(countImagePix, Region.MASK_FOREGROUND);
        int cellNum = singleImageRegions.size();
        byte[] voronoiPix = EDMMaker.makeVoronoiPix(BinaryMaker.makeBinaryImage(regionImagePix, width, height, null, 0, 0));
        Arrays.fill(tempRegionPix, Region.MASK_FOREGROUND);
        for (int i = 0; i < cellNum; i++) {
            Region cell = singleImageRegions.get(i);
            if (cell != null && cell.isActive()) {
                exec.submit(new RunnableRegionGrower(cell, expandedImagePix, width,
                        checkImagePix, regionImagePix, inputPix, threshold,
                        i, height, countImagePix, tempRegionPix, regionImage,
                        voronoiPix, "RegionGrower_" + i));
            }
        }
        terminate("Error detecting cells.");
    }
}
