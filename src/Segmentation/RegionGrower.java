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
package Segmentation;

import Cell.CellData;
import IAClasses.Region;
import IAClasses.Utils;
import UserVariables.UserVariables;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortBlitter;
import ij.process.ShortProcessor;
import ij.process.TypeConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class RegionGrower {

    public static short terminal;
    public static short intermediate;

    public static int initialiseROIs(ByteProcessor masks, int threshold, int start, ImageProcessor input, PointRoi roi, int width, int height, int size, ArrayList<CellData> cellData, UserVariables uv, boolean protMode) {
        ArrayList<short[]> initP = new ArrayList();
        int n;
        if (roi != null) {
            if (roi.getType() == Roi.POINT) {
                n = roi.getNCoordinates();
            } else {
                IJ.error("Point selection required.");
                return -1;
            }
        } else {
            if (threshold < 0) {
                threshold = getThreshold(input, uv.isAutoThreshold(), uv.getGreyThresh(), uv.getThreshMethod());
            }
            ByteProcessor binary = (ByteProcessor) input.convertToByteProcessor(true);
            binary.threshold(threshold);
            if (masks != null) {
                ByteBlitter bb = new ByteBlitter(binary);
                bb.copyBits(masks, 0, 0, Blitter.SUBTRACT);
            }
            double minArea = protMode ? getMinFilArea(uv) : getMinCellArea(uv);
            getSeedPoints(binary, initP, minArea);
            n = initP.size();
        }
        if (cellData == null) {
            cellData = new ArrayList();
        }
        int s = cellData.size();
        int N = s + n;
        for (int i = s; i < N; i++) {
            cellData.add(new CellData(start));
            cellData.get(i).setImageWidth(width);
            cellData.get(i).setImageHeight(height);
            short[] init;
            if (roi != null) {
                init = new short[]{(short) (roi.getXCoordinates()[i] + roi.getBounds().x), (short) (roi.getYCoordinates()[i] + roi.getBounds().y)};
            } else {
                init = initP.get(i - s);
            }
            if (!Utils.isEdgePixel(init[0], init[1], width, height, 1)) {
                ByteProcessor mask = new ByteProcessor(width, height);
                mask.setColor(Region.MASK_BACKGROUND);
                mask.fill();
                mask.setColor(Region.MASK_FOREGROUND);
                mask.drawPixel(init[0], init[1]);
                cellData.get(i).setInitialRegion(new Region(mask, init));
                cellData.get(i).setEndFrame(size);
            } else {
                cellData.get(i).setInitialRegion(null);
                cellData.get(i).setEndFrame(0);
            }
        }
        return n;
    }

    public static ArrayList<Region> findCellRegions(ImageProcessor inputProc, Roi[] rois, double t, String method) {
        PointRoi proi = null;
        for (Roi r : rois) {
            double[] centroid = r.getContourCentroid();
            if (proi == null) {
                proi = new PointRoi(centroid[0], centroid[1]);
            } else {
                proi.addPoint(centroid[0], centroid[1]);
            }
        }
        ArrayList<CellData> cells = new ArrayList();
        RegionGrower.initialiseROIs(null, -1, 0, inputProc, proi, inputProc.getWidth(), inputProc.getHeight(), 1, cells, null, false);
        return findCellRegions(inputProc, getThreshold(inputProc, true, t, method), cells);
    }

    /*
     * Detects the cells in the specified image and, if showPreview is true,
     * returns an image illustrating the detected boundary.
     */
    public static ArrayList<Region> findCellRegions(ImageProcessor inputProc, double threshold, ArrayList<CellData> cellData) {
        int outVal = 1;
        ImageProcessor inputFloatProc = (new TypeConverter(inputProc, true)).convertToFloat(null);
        ImageProcessor inputDup = inputFloatProc.duplicate();
        int width = inputDup.getWidth();
        int height = inputDup.getHeight();
        int n = cellData.size();
        ArrayList<Region> singleImageRegions = new ArrayList();
        /*
         * Create image depicting regions to be "grown". Regions initialised
         * using centroids.
         */
        ShortProcessor indexedRegions = new ShortProcessor(width, height);
        indexedRegions.setValue(Region.MASK_FOREGROUND);
        indexedRegions.fill();
        indexedRegions.setColor(outVal);
        ShortBlitter bb = new ShortBlitter(indexedRegions);
        for (int i = 0; i < n; i++) {
            Region region = cellData.get(i).getInitialRegion();
            if (region != null) {
                ShortProcessor mask = (ShortProcessor) (new TypeConverter(region.getMask(), false)).convertToShort();
                mask.invert();
                mask.multiply((i + 1) / Region.MASK_BACKGROUND);
                bb.copyBits(mask, 0, 0, Blitter.COPY_ZERO_TRANSPARENT);
            }
            singleImageRegions.add(region);
            outVal++;
        }
        intermediate = (short) (singleImageRegions.size() + 1);
        terminal = (short) (intermediate + 1);
        /*
         * Filter image to be used as basis for region growing.
         */
        IJ.saveAs(new ImagePlus("", growRegions(indexedRegions, inputDup, singleImageRegions, threshold)), "TIF", "C:\\Users\\barryd\\Debugging\\particle_mapper_debug");
        return singleImageRegions;
    }

    /*
     * Conditional dilate the regions in regionImage based on the information in
     * inputImage.
     */
    private static ShortProcessor growRegions(ShortProcessor regionImage, ImageProcessor inputImage, ArrayList<Region> singleImageRegions, double threshold) {
        int i;
        int j;
        int width = regionImage.getWidth();
        int height = regionImage.getHeight();
        int widthheight = width * height;
        boolean totChange = true;
        boolean thisChange;
        float[] inputPix = (float[]) inputImage.getPixels();
        short[] regionImagePix = (short[]) regionImage.getPixels();
        short[] checkImagePix = new short[widthheight];
        short[] tempRegionPix = new short[widthheight];
        short[] countImagePix = new short[widthheight];
        short[] expandedImagePix = new short[widthheight];
        Arrays.fill(checkImagePix, Region.MASK_FOREGROUND);
        Arrays.fill(countImagePix, Region.MASK_FOREGROUND);
        /*
         * Image texture (and grey levels) used to control region growth.
         * Standard deviation of grey levels is used as a simple measure of
         * texture.
         */
        int cellNum = singleImageRegions.size();
        //        float distancemaps[][][] = null;
        //        if (!simple) {
        //            distancemaps = new float[cellNum][width][height];
        //        }
        //        ByteProcessor regionImages[] = new ByteProcessor[cellNum];
        //        if (!simple) {
        //            initDistanceMaps(inputImage, regionImage, singleImageRegions, distancemaps,
        //                    regionImages, width, 1.0, threshold);
        //        }
        /*
         * Reset regionImages
         */
        //        for (int n = 0; n < cellNum; n++) {
        //            regionImages[n] = (ByteProcessor) regionImage.duplicate();
        //        }
        /*
         * Grow regions according to texture, grey levels and distance maps
         */
        //        ImageStack regionImageStack = new ImageStack(regionImage.getWidth(), regionImage.getHeight());
        //        ImageStack expandedImageStack = new ImageStack(regionImage.getWidth(), regionImage.getHeight());
        while (totChange) {
            totChange = false;
            for (i = 0; i < cellNum; i++) {
                //                ImageStack distancemapStack = new ImageStack(distancemaps[0].length, distancemaps[0][0].length);
                //                for (int n = 0; n < distancemaps.length; n++) {
                //                    FloatProcessor distanceMapImage = new FloatProcessor(distancemaps[i].length, distancemaps[i][0].length);
                //                    for (int x = 0; x < distancemaps[i].length; x++) {
                //                        for (int y = 0; y < distancemaps[i][x].length; y++) {
                //                            distanceMapImage.putPixelValue(x, y, distancemaps[i][x][y]);
                //                        }
                //                    }
                //                    distancemapStack.addSlice(distanceMapImage);
                //                }
                //                IJ.saveAs(new ImagePlus("", distanceMapImage), "TIF", "C:/users/barry05/desktop/distanceMapImage_" + i + ".tif");}
                //                ByteProcessor ref = (ByteProcessor) regionImages[i].duplicate();
                Region cell = singleImageRegions.get(i);
                if (cell != null && cell.isActive()) {
                    Arrays.fill(expandedImagePix, Region.MASK_BACKGROUND);
                    Arrays.fill(tempRegionPix, Region.MASK_FOREGROUND);
                    LinkedList<short[]> borderPix = cell.getBorderPix();
                    int borderLength = borderPix.size();
                    thisChange = false;
                    for (j = 0; j < borderLength; j++) {
                        short[] thispix = borderPix.get(j);
                        int offset = thispix[1] * width;
                        //                        if (!simple) {
                        //                            thisChange = dijkstraDilate(ref, cell, thispix,
                        //                                    distancemaps, intermediate, i + 1) || thisChange;
                        //                        } else {
                        if (checkImagePix[thispix[0] + offset] == Region.MASK_FOREGROUND) {
                            boolean thisResult = simpleDilate(regionImagePix, inputPix, cell, thispix, intermediate, threshold, (short) (i + 1), expandedImagePix, width, height, countImagePix, tempRegionPix);
                            thisChange = thisResult || thisChange;
                            if (!thisResult) {
                                checkImagePix[thispix[0] + offset]++;
                            }
                        }
                        //                        }
                        //                        regionImageStack.addSlice(regionImage.duplicate());
                        //                        IJ.saveAs((new ImagePlus("", regionImageStack)), "TIF", "c:\\users\\barry05\\desktop\\masks\\regions.tif");
                        //                        expandedImageStack.addSlice(expandedImage.duplicate());
                    }
                    cell.setActive(thisChange);
                    totChange = thisChange || totChange;
                }
                //                }
            }
            //            regionImageStack.addSlice(regionImage.duplicate());
            //            IJ.saveAs((new ImagePlus("", regionImageStack)), "TIF", "/Users/Dave/Desktop/EMSeg Test Output/regions.tif");
            //            if (simple) {
            expandRegions(singleImageRegions, regionImage, cellNum,terminal, tempRegionPix);
            //            } else {
            //                expandRegions(singleImageRegions, regionImages, cellNum);
            //            }
            //            regionImageStack.addSlice(regionImage.duplicate());
            //            IJ.saveAs((new ImagePlus("", regionImageStack)), "TIF", "c:\\users\\barry05\\desktop\\regions.tif");
        }
        //        for (i = 0; i < cellNum; i++) {
        //            Region cell = singleImageRegions.get(i);
        //            cell.clearPixels();
        //        }
        //        IJ.saveAs((new ImagePlus("", regionImageStack)), "TIF", "C:\\Users\\barryd\\Debugging\\particle_tracker_debug\\regions.tif");
        //        IJ.saveAs((new ImagePlus("", expandedImageStack)), "TIF", "c:\\users\\barry05\\desktop\\masks\\expandedimages.tif");
        //        IJ.saveAs(new ImagePlus("", inputImage), "TIF", "C:/users/barry05/desktop/inputImage.tif");
        return regionImage;
    }

    private static boolean simpleDilate(short[] regionImagePix, float[] greyPix, Region cell, short[] point, short intermediate, double greyThresh, short index, short[] expandedImagePix, int width, int height, short[] countPix, short[] tempImagePix) {
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
                    if ((r == Region.MASK_FOREGROUND || r == intermediate) && (g > greyThresh)) {
                        short[] p = new short[]{(short) i, (short) j};
                        regionImagePix[i + jOffset] = intermediate;
                        dilate = true;
                        if (expandedImagePix[i + jOffset] != Region.MASK_FOREGROUND) {
                            cell.addExpandedBorderPix(p);
                            expandedImagePix[i + jOffset] = Region.MASK_FOREGROUND;
                            tempImagePix[x + yOffset]++;
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

    /*
     * Updates regionImages according to the expanded border sets in regions.
     * When complete, borders are dilated to expanded borders and expanded
     * borders are set to null.
     */
    static void expandRegions(ArrayList<Region> regions, ShortProcessor regionImage, int N, short terminal, short[] tempRegionPix) {
        int width = regionImage.getWidth();
        for (int i = 0; i < N; i++) {
            Region cell = regions.get(i);
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
        //        IJ.saveAs((new ImagePlus("", tempRegionImage)), "PNG", "c:\\users\\barry05\\desktop\\masks\\tempRegionImage.png");
    }

    static void getSeedPoints(ByteProcessor binary, ArrayList<short[]> pixels, double minArea) {
        binary.invert();
        if (binary.isInvertedLut()) {
            binary.invertLut();
        }
        ResultsTable rt = Analyzer.getResultsTable();
        rt.reset();
        Prefs.blackBackground = false;
        ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, Measurements.CENTROID, rt, minArea, Double.POSITIVE_INFINITY);
        analyzeDetections(null, binary, analyzer);
        int count = rt.getCounter();
        if (count > 0) {
            float[] x = rt.getColumn(rt.getColumnIndex("X"));
            float[] y = rt.getColumn(rt.getColumnIndex("Y"));
            for (int i = 0; i < count; i++) {
                pixels.add(new short[]{(short) Math.round(x[i]), (short) Math.round(y[i])});
            }
        }
    }

    public static int getThreshold(ImageProcessor image, boolean auto, double thresh, String method) {
        if (auto) {
            return (new AutoThresholder()).getThreshold(method, image.getStatistics().histogram);
        } else {
            return (int) Math.round(Utils.getPercentileThresh(image, thresh));
        }
    }

    public static double getMinFilArea(UserVariables uv) {
        return uv.getFiloSizeMin() / (Math.pow(uv.getSpatialRes(), 2.0));
    }

    public static double getMinCellArea(UserVariables uv) {
        if (uv != null) {
            return uv.getMorphSizeMin() / (Math.pow(uv.getSpatialRes(), 2.0));
        }
        return 0.0;
    }

    public static void hideWindows() {
        if (WindowManager.getImageCount() > 0) {
            WindowManager.getImage(WindowManager.getIDList()[WindowManager.getImageCount() - 1]).hide();
        }
    }

    public static void analyzeDetections(RoiManager manager, ImageProcessor binmap, ParticleAnalyzer analyzer) {
        ParticleAnalyzer.setRoiManager(manager);
        if (!analyzer.analyze(new ImagePlus("", binmap))) {
            IJ.log("Protrusion analysis failed.");
        }
        RegionGrower.hideWindows();
    }
    
}
