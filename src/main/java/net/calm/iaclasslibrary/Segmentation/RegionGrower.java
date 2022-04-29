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
package net.calm.iaclasslibrary.Segmentation;

import ij.*;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.*;
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.IAClasses.Region;
import net.calm.iaclasslibrary.IAClasses.Utils;
import net.calm.iaclasslibrary.Process.Segmentation.MultiThreadedRegionGrower;
import net.calm.iaclasslibrary.Process.Segmentation.MultiThreadedWatershed;
import net.calm.iaclasslibrary.UserVariables.UserVariables;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class RegionGrower {

    private static boolean simple = true;
    public static short terminal;
    public static short intermediate;
    private static double lambda = 100.0, filtRad = 10.0; // parameter used in construction of Voronoi manifolds. See Jones et al., 2005: dx.doi.org/10.1007/11569541_54
    private static int SIXTEEN_TO_EIGHT_OFFSET = (int) (Math.round(Math.pow(2, 16) - Math.pow(2, 8)));

    public static int initialiseROIs(ByteProcessor masks, int threshold, int start, ImageProcessor input, PointRoi roi, int width, int height, int size, ArrayList<CellData> cellData, UserVariables uv, boolean protMode, boolean selectiveOutput) {
        ArrayList<short[]> initP = new ArrayList<>();
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
//            IJ.saveAs(new ImagePlus("", binary), "PNG", String.format("E:\\Debug\\Adapt\\%s_%d.png", "Residuals", (start - 2)));
            double minArea = protMode ? getMinFilArea(uv) : getMinCellArea(uv);
            getSeedPoints(binary, initP, minArea);
            n = initP.size();
        }
        if (cellData == null) {
            cellData = new ArrayList<>();
        }
        int s = cellData.size();
        int N = s + n;
        for (int i = s; i < N; i++) {
            CellData cell = new CellData(start, selectiveOutput ? start == 1 : true);
            cell.setImageWidth(width);
            cell.setImageHeight(height);
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
                cell.setInitialRegion(new Region(mask, init));
                cell.setEndFrame(size);
            } else {
                cell.setInitialRegion(null);
                cell.setEndFrame(0);
            }
            cellData.add(cell);
        }
        return n;
    }

    @Deprecated
    /**
     * Find cell regions in the given image
     *
     * @param inputProc Input image
     * @param rois      Regions of interest on which to initialise the regions
     * @param t         Percentile to use in manual threshold calculation
     * @param method    Method to use for automatic threshold calculation
     * @return List of regions
     */
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
        ArrayList<CellData> cells = new ArrayList<>();
        RegionGrower.initialiseROIs(null, -1, 0, inputProc, proi, inputProc.getWidth(), inputProc.getHeight(), 1, cells, null, false, false);
        return findCellRegions(inputProc, getThreshold(inputProc, true, t, method), cells);
    }

    @Deprecated
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
        ArrayList<Region> singleImageRegions = new ArrayList<>();
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
                mask.multiply(i + 1);
                mask.multiply(1.0 / Region.MASK_BACKGROUND);
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
        growRegions(indexedRegions, inputDup, singleImageRegions, threshold);
        return singleImageRegions;
    }

    /**
     * Produces a region image following conditional dilation of the regions in
     * the input regionImage
     *
     * @param regionImage        net.calm.iaclasslibrary.Image to be dilated
     * @param inputImage
     * @param singleImageRegions List of regions in image
     * @param threshold          Grey level threshold for dilation
     * @return Dilated region image
     */
    private static ShortProcessor growRegions(ShortProcessor regionImage, ImageProcessor inputImage, ArrayList<Region> singleImageRegions, double threshold) {
        (new MultiThreadedRegionGrower(regionImage, inputImage, singleImageRegions, threshold, null)).run();
        return regionImage;
    }

    static void saveDistanceMaps(float[][][] distancemaps, String label) {
        int cellNum = distancemaps.length;
        for (int i = 0; i < cellNum; i++) {
            FloatProcessor distanceMapImage = new FloatProcessor(distancemaps[i].length, distancemaps[i][0].length);
            for (int x = 0; x < distancemaps[i].length; x++) {
                for (int y = 0; y < distancemaps[i][x].length; y++) {
                    distanceMapImage.putPixelValue(x, y, distancemaps[i][x][y]);
                }
            }
//            IJ.saveAs(new ImagePlus("", distanceMapImage), "TIF", String.format("C:\\Users\\barryd\\Debugging\\adapt_debug\\%s_%d.tif", label, i));
        }
    }

    static void initDistanceMaps(ImageProcessor inputImage, ShortProcessor regionImage, ArrayList<Region> singleImageRegions, float[][][] distancemaps, double filtRad, double thresh) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int widthheight = width * height;
        int cellNum = singleImageRegions.size();
        float[] inputPix = (float[]) inputImage.getPixels();
        ImageStack regionStack = new ImageStack(width, height);
        short[][] checkImagePix = new short[cellNum][widthheight];
        short[][] countImagePix = new short[cellNum][widthheight];
        short[][] tempRegionPix = new short[cellNum][widthheight];
        short[] expandedImagePix = new short[widthheight];
        ImageProcessor texture = inputImage.duplicate();
        texture.findEdges();
        (new GaussianBlur()).blurGaussian(texture, filtRad, filtRad, 0.01);
        float[] texturePix = (float[]) texture.getPixels();
        for (int n = 0; n < cellNum; n++) {
            Arrays.fill(checkImagePix[n], Region.MASK_FOREGROUND);
            Arrays.fill(countImagePix[n], Region.MASK_FOREGROUND);
            for (int x = 0; x < width; x++) {
                Arrays.fill(distancemaps[n][x], Float.MAX_VALUE);
            }
            Region cell = singleImageRegions.get(n);
            if (cell != null) {
                Rectangle bounds = cell.getBounds();
                ImageProcessor mask = cell.getMask();
                for (int i = bounds.x; i < bounds.x + bounds.width; i++) {
                    for (int j = bounds.y; j < bounds.y + bounds.height; j++) {
                        if (mask.getPixel(i, j) == Region.MASK_FOREGROUND) {
                            distancemaps[n][i][j] = 0.0f;
                        }
                    }
                }
                LinkedList<short[]> borderPix = cell.getBorderPix();
                int bordersize = borderPix.size();
                for (int s = 0; s < bordersize; s++) {
                    short[] pix = borderPix.get(s);
                    distancemaps[n][pix[0]][pix[1]] = 0.0f;
                }
            }
            regionStack.addSlice(regionImage.duplicate());
        }
        boolean totChange = true;
        while (totChange) {
            totChange = false;
            for (int i = 0; i < cellNum; i++) {
                Arrays.fill(tempRegionPix[i], Region.MASK_FOREGROUND);
                short[] regionImagePix = (short[]) regionStack.getProcessor(i + 1).getPixels();
                Region cell = singleImageRegions.get(i);
                if (cell != null && cell.isActive()) {
                    Arrays.fill(expandedImagePix, Region.MASK_BACKGROUND);
                    LinkedList<short[]> borderPix = cell.getBorderPix();
                    int borderLength = borderPix.size();
                    boolean thisChange = false;
                    for (int j = 0; j < borderLength; j++) {
                        short[] thispix = borderPix.get(j);
                        int offset = thispix[1] * width;
                        if (checkImagePix[i][thispix[0] + offset] == Region.MASK_FOREGROUND) {
                            boolean thisResult = buildDistanceMaps(regionImagePix, inputPix, cell, thispix, distancemaps[i], thresh, texturePix, i + 1, lambda, expandedImagePix, width, height, countImagePix[i], tempRegionPix[i]);
                            thisChange = thisResult || thisChange;
                            if (!thisResult) {
                                checkImagePix[i][thispix[0] + offset]++;
                            }
                        }
                        cell.setActive(thisChange);
                        totChange = thisChange || totChange;
                    }
                }
            }
            expandRegions(singleImageRegions, regionStack, cellNum, terminal, tempRegionPix);
        }
//        IJ.saveAs(new ImagePlus("", regionStack), "TIF", String.format("C:\\Users\\barryd\\Debugging\\adapt_debug\\%s.tif", "RegionImage"));
    }

    static boolean buildDistanceMaps(short[] regionImagePix, float[] greyPix, Region cell, short[] point, float[][] distancemap, double thresh, float[] gradientPix, int index, double lambda, short[] expandedImagePix, int width, int height, short[] countPix, short[] tempImagePix) {
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
                    int r = regionImagePix[i + jOffset];
                    float g = greyPix[i + jOffset];
                    if ((r == Region.MASK_FOREGROUND || r == intermediate) && (g > thresh)) {
                        short[] p = new short[]{(short) i, (short) j};
                        regionImagePix[i + jOffset] = intermediate;
                        dilate = true;
                        if (expandedImagePix[i + jOffset] != Region.MASK_FOREGROUND) {
                            float dist = calcDistance(point, p, gradientPix, lambda, width);
                            distancemap[i][j] = distancemap[x][y] + dist;
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

    static float calcDistance(short[] point1, short[] point2, float[] gradPix, double lambda, int width) {
        return (float) ((Math.pow(gradPix[point2[1] * width + point2[0]] - gradPix[point1[0] + point1[1] * width], 2.0) + lambda) / (1.0 + lambda));
    }

    /**
     * Conditionally dilate regions
     *
     * @param regionImagePix   Pixel object representation region image
     * @param greyPix          Grey level pixels
     * @param cell             Region object considered for dilation
     * @param point            current point being queried for dilation
     * @param intermediate     Value to assign to pixels in region image if dilation
     *                         is possible
     * @param greyThresh       Grey level threshold criteria for dilation
     * @param index            Current region index
     * @param expandedImagePix Candidate pixels for dilation
     * @param width            net.calm.iaclasslibrary.Image width
     * @param height           net.calm.iaclasslibrary.Image height
     * @param countPix         Reference grid for keeping track of how often pixels are
     *                         queried
     * @param tempImagePix
     * @param voronoiPix       Pixel object representing voronoi segmentation
     * @return True if dilation is possible, false otherwise
     */
    private static boolean simpleDilate(short[] regionImagePix, float[] greyPix, Region cell, short[] point, short intermediate, double greyThresh, short index, short[] expandedImagePix, int width, int height, short[] countPix, short[] tempImagePix, byte[] voronoiPix) {
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

    static boolean dijkstraDilate(short[] regionImagePix, Region cell, short[] point, float[] greyPix, double greyThresh, float[] gradPix, short intermediate, int index, short[] expandedImagePix, int width, int height, short[] countPix, short[] tempImagePix, float[][] distanceMap) {
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
        float minDist = Float.MAX_VALUE;
        short[] p = null;
        for (int j = y > 0 ? y - 1 : 0; j < height && j <= y + 1; j++) {
            int jOffset = j * width;
            for (int i = x > 0 ? x - 1 : 0; i < width && i <= x + 1; i++) {
//                if (countPix[i + jOffset] == 0) {
//                    countPix[i + jOffset]++;
                short r = regionImagePix[i + jOffset];
                double g = greyPix[jOffset + i];
                if ((r == Region.MASK_FOREGROUND || r == intermediate) && (g > greyThresh)) {
                    float dist = distanceMap[x][y] + calcDistance(point, new short[]{(short) i, (short) j}, gradPix, lambda, width);
                    if (dist < minDist) {
                        minDist = dist;
                        p = new short[]{(short) i, (short) j};
                    }
                }
//                }
            }
        }
        if (p != null) {
            int i = p[0];
            int jOffset = width * p[1];
            regionImagePix[i + jOffset] = intermediate;
            dilate = true;
            distanceMap[p[0]][p[1]] = minDist;
            if (expandedImagePix[i + jOffset] != Region.MASK_FOREGROUND) {
                cell.addExpandedBorderPix(p);
                expandedImagePix[i + jOffset] = Region.MASK_FOREGROUND;
                tempImagePix[i + jOffset]++;
            }
        }
        for (int j = y > 0 ? y - 1 : 0; j < height && j <= y + 1; j++) {
            int jOffset = j * width;
            for (int i = x > 0 ? x - 1 : 0; i < width && i <= x + 1; i++) {
                short r = regionImagePix[i + jOffset];
                remove = (r == intermediate || r == index) && remove;
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
     * Updates regionImages according to the expanded border sets in regions.
     * When complete, borders are dilated to expanded borders and expanded
     * borders are set to null.
     *
     * @param regions       Regions list to be expanded
     * @param regionImage   net.calm.iaclasslibrary.Image depicting regions
     * @param N             Number of regions to be expanded
     * @param terminal      Value used in region image to depict termination of
     *                      expansion
     * @param tempRegionPix Contains candidate points for expansion
     */
    static void expandRegions(ArrayList<Region> regions, ShortProcessor regionImage, int N, short terminal, short[] tempRegionPix) {
        int width = regionImage.getWidth();
        for (int i = 0; i < N; i++) {
            expandRegion(regions.get(i), width, tempRegionPix, regionImage, i);
        }
        //        IJ.saveAs((new ImagePlus("", tempRegionImage)), "PNG", "c:\\users\\barry05\\desktop\\masks\\tempRegionImage.png");
    }

    /**
     * Updates regionImages according to the expanded border set in region. When
     * complete, border is dilated to expanded border and expanded border is set
     * to null.
     *
     * @param cell          Region to be expanded
     * @param width         net.calm.iaclasslibrary.Image width
     * @param tempRegionPix Contains candidate points for expansion
     * @param regionImage   net.calm.iaclasslibrary.Image depicting regions
     * @param i             Region index
     */
    static void expandRegion(Region cell, int width, short[] tempRegionPix, ShortProcessor regionImage, int i) {
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

    static void expandRegions(ArrayList<Region> regions, ImageStack regionImageStack, int N, short terminal, short[][] tempRegionPix) {
        int width = regionImageStack.getWidth();
        for (int i = 0; i < N; i++) {
            Region cell = regions.get(i);
            ImageProcessor regionImage = regionImageStack.getProcessor(i + 1);
            if (cell != null) {
                LinkedList<short[]> pixels = cell.getExpandedBorder();
                int borderLength = pixels.size();
                for (int j = 0; j < borderLength; j++) {
                    short[] current = pixels.get(j);
                    int x = current[0];
                    int y = current[1];
                    int yOffset = y * width;
                    if (tempRegionPix[i][x + yOffset] > 1) {
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

    public static ArrayList<Region> watershedRegions(ImageProcessor inputProc, double threshold, ArrayList<CellData> cellData) {
        ShortProcessor indexedRegions = new ShortProcessor(inputProc.getWidth(), inputProc.getHeight());
        indexedRegions.setValue(Region.MASK_FOREGROUND);
        indexedRegions.fill();
        //indexedRegions.setColor(outVal);
        ShortBlitter bb = new ShortBlitter(indexedRegions);
        ArrayList<Region> singleImageRegions = new ArrayList<>();
        for (int i = 0; i < cellData.size(); i++) {
            Region region = cellData.get(i).getInitialRegion();
            if (region != null) {
                ShortProcessor mask = (ShortProcessor) (new TypeConverter(region.getMask(), false)).convertToShort();
                mask.invert();
                mask.subtract(RegionGrower.SIXTEEN_TO_EIGHT_OFFSET);
                mask.multiply(i + 1);
                mask.multiply(1.0 / Region.MASK_BACKGROUND);
                bb.copyBits(mask, 0, 0, Blitter.COPY_ZERO_TRANSPARENT);
            }
            singleImageRegions.add(region);
        }
        ImageProcessor mask = inputProc.duplicate();
        mask.threshold((int) Math.round(threshold));
//        IJ.saveAs(new ImagePlus("", mask), "PNG", "E:/Debug/Adapt/watershed_mask.png");
//        IJ.saveAs(new ImagePlus("", inputProc), "PNG", "E:/Debug/Adapt/watershed_input.png");
//        IJ.saveAs(new ImagePlus("", indexedRegions), "PNG", "E:/Debug/Adapt/watershed_seeds.png");
//        IJ.saveAs(MultiThreadedWatershed.watershed(new ImagePlus("", mask), new ImagePlus("", indexedRegions), new ImagePlus("", mask)),
//                "PNG", "E:/Debug/Adapt/watershed_output.png");

        ImagePlus watershedOutput = MultiThreadedWatershed.watershed(new ImagePlus("", mask), new ImagePlus("", indexedRegions), new ImagePlus("", mask));

        for (int i = 0; i < cellData.size(); i++) {
            Region initialRegion = cellData.get(i).getInitialRegion();
            if (initialRegion != null) {
                float[] centre = initialRegion.getCentre();
                if (watershedOutput.getProcessor().getPixelValue((int) Math.round(centre[0]), (int) Math.round(centre[1])) > 0.0) {
                    ImageProcessor regionMask = watershedOutput.getProcessor().duplicate();
                    regionMask.setThreshold(i + 1, i + 1, ImageProcessor.NO_LUT_UPDATE);
                    regionMask = regionMask.createMask();
                    regionMask.invert();
//                    IJ.saveAs(new ImagePlus("", regionMask), "PNG", "E:/Debug/Adapt/region_mask" + (i + 1) + ".png");
                    singleImageRegions.set(i, new Region(regionMask));
                }
            }
        }

        return singleImageRegions;
    }

}
