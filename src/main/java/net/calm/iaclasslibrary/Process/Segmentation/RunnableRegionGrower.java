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
package net.calm.iaclasslibrary.Process.Segmentation;

import net.calm.iaclasslibrary.IAClasses.Region;
import net.calm.iaclasslibrary.IAClasses.Utils;
import net.calm.iaclasslibrary.Process.RunnableProcess;
import static net.calm.iaclasslibrary.Segmentation.RegionGrower.intermediate;
import static net.calm.iaclasslibrary.Segmentation.RegionGrower.terminal;
import ij.process.ShortProcessor;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Runnable version of region growing algorithm that grows a single region in 
 * a single image frame
 * 
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class RunnableRegionGrower extends RunnableProcess {

    Region cell;
    short[] expandedImagePix;
    int width;
    short[] checkImagePix;
    short[] regionImagePix;
    float[] inputPix;
    double threshold;
    int i;
    int height;
    short[] countImagePix;
    short[] tempRegionPix;
    byte[] voronoiPix;
    ShortProcessor regionImage;

    /**
     * Constructor method
     * 
     * @param cell The region to be grown
     * @param expandedImagePix Pixel array representing pixels that will be incorporated into region
     * @param width net.calm.iaclasslibrary.Image width
     * @param checkImagePix Pixel array ensures no pixel is polled more than once
     * @param regionImagePix Pixel object in region image
     * @param inputPix Pixel object representing image over which region is to be grown
     * @param threshold Grey level threshold governing region growing
     * @param i Frame index
     * @param height net.calm.iaclasslibrary.Image height
     * @param countImagePix Ensures no pixel is added to region border twice
     * @param tempRegionPix Keeps track of number of regions potentially overlapping each pixel
     * @param regionImage net.calm.iaclasslibrary.Image depicting segmented regions
     * @param voronoiPix Pixel object depicting voronoi segmentation based on initial regions
     * @param name Not used
     */
    public RunnableRegionGrower(Region cell, short[] expandedImagePix, int width,
            short[] checkImagePix, short[] regionImagePix, float[] inputPix, double threshold,
            int i, int height, short[] countImagePix, short[] tempRegionPix, ShortProcessor regionImage,
            byte[] voronoiPix, String name) {
        super(name);
        this.cell = cell;
        this.expandedImagePix = expandedImagePix;
        this.width = width;
        this.checkImagePix = checkImagePix;
        this.regionImagePix = regionImagePix;
        this.inputPix = inputPix;
        this.threshold = threshold;
        this.i = i;
        this.height = height;
        this.countImagePix = countImagePix;
        this.tempRegionPix = tempRegionPix;
        this.voronoiPix = voronoiPix;
        this.regionImage = regionImage;
    }

    @Override
    public void run() {
        boolean thisChange = true;
        while (thisChange) {
            thisChange = false;
            Arrays.fill(expandedImagePix, Region.MASK_BACKGROUND);
            LinkedList<short[]> borderPix = cell.getBorderPix();
            int borderLength = borderPix.size();
            for (int j = 0; j < borderLength; j++) {
                short[] thispix = borderPix.get(j);
                int offset = thispix[1] * width;
                if (checkImagePix[thispix[0] + offset] == Region.MASK_FOREGROUND) {
                    boolean thisResult = simpleDilate(regionImagePix, inputPix, cell, thispix, intermediate, threshold, (short) (i + 1), expandedImagePix, width, height, countImagePix, tempRegionPix, voronoiPix);
                    thisChange = thisResult || thisChange;
                    if (!thisResult) {
                        checkImagePix[thispix[0] + offset]++;
                    }
                }
                cell.setActive(thisChange);
            }
            expandRegion(cell, width, tempRegionPix, regionImage, i);
        }
    }
    
    /**
     * 
     * @param regionImagePix Pixel object in region image
     * @param greyPix Pixel object representing image over which region is to be grown
     * @param cell The region to be grown
     * @param point Current location
     * @param intermediate Assigned to candidate expansion pixels
     * @param greyThresh Grey level threshold governing region growing
     * @param index Region index
    * @param expandedImagePix Pixel array representing pixels that will be incorporated into region
  * @param width net.calm.iaclasslibrary.Image width
     * @param height net.calm.iaclasslibrary.Image height
     * @param countPix Ensures no pixel is added to region border twice
     * @param tempImagePix Keeps track of number of regions potentially overlapping each pixel
     * @param voronoiPix Pixel object depicting voronoi segmentation based on initial regions
     * @return 
     */
    boolean simpleDilate(short[] regionImagePix, float[] greyPix, Region cell, short[] point,
            short intermediate, double greyThresh, short index, short[] expandedImagePix,
            int width, int height, short[] countPix, short[] tempImagePix, byte[] voronoiPix) {
        int x = point[0];
        int y = point[1];
        int yOffset = y * width;
        if (regionImagePix[x + yOffset] > intermediate) {
            cell.addExpandedBorderPix(point);
            expandedImagePix[x + yOffset] = Region.MASK_FOREGROUND;
            tempImagePix[x + yOffset]++;
            return false;
        }
        boolean dilate = false;
        boolean remove = true;
        for (int j = y > 0 ? y - 1 : 0; j < height && j <= y + 1; j++) {
            int jOffset = j * width;
            for (int i = x > 0 ? x - 1 : 0; i < width && i <= x + 1; i++) {
                if (countPix[i + jOffset] == 0) {
                    countPix[i + jOffset]++;
                    short r = regionImagePix[i + jOffset];
                    double g = greyPix[jOffset + i];
                    int v = voronoiPix[jOffset + i];
                    if ((r == Region.MASK_FOREGROUND || r == intermediate) && (g > greyThresh)
                            && (v == Region.MASK_FOREGROUND)) {
                        short[] p = new short[]{(short) i, (short) j};
                        regionImagePix[i + jOffset] = intermediate;
                        dilate = true;
                        if (expandedImagePix[i + jOffset] != Region.MASK_FOREGROUND) {
                            cell.addExpandedBorderPix(p);
                            expandedImagePix[i + jOffset] = Region.MASK_FOREGROUND;
                            tempImagePix[i + jOffset]++;
                        }
                    }
                    r = regionImagePix[i + jOffset];
                    remove = (r == intermediate || r == index) && remove;
                }
            }
        }
        if (!remove) {
            cell.addExpandedBorderPix(point);
            expandedImagePix[x + yOffset] = Region.MASK_FOREGROUND;
            tempImagePix[x + yOffset]++;
            if (x < 1 || y < 1 || x >= width - 1 || y >= height - 1) {
                cell.setEdge(true);
            }
        } else if (Utils.isEdgePixel(x, y, width, height, 1)) {
            cell.addExpandedBorderPix(point);
            expandedImagePix[x + yOffset] = Region.MASK_FOREGROUND;
            tempImagePix[x + yOffset]++;
        }
        return dilate;
    }

    /**
     * Expand the current region based on contents of @tempRegionPix
     * 
     * @param cell Current region
     * @param width net.calm.iaclasslibrary.Image width
     * @param tempRegionPix Keeps track of number of regions potentially overlapping each pixel
     * @param regionImage net.calm.iaclasslibrary.Image depicting segmented regions
     * @param i Frame index
     */
    void expandRegion(Region cell, int width, short[] tempRegionPix, ShortProcessor regionImage, int i) {
        if (cell != null) {
            LinkedList<short[]> pixels = cell.getExpandedBorder();
            int borderLength = pixels.size();
            for (int j = 0; j < borderLength; j++) {
                short[] current = pixels.get(j);
                int x = current[0];
                int y = current[1];
                int yOffset = y * width;
                if (tempRegionPix[x + yOffset] > 1) {
                    regionImage.putPixelValue(x, y, terminal);
                } else {
                    regionImage.putPixelValue(x, y, i + 1);
                }
            }
            cell.expandBorder();
        }
    }

}
