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
package net.calm.iaclasslibrary.Fluorescence;

import net.calm.iaclasslibrary.Cell.Cell;
import net.calm.iaclasslibrary.Cell.CellRegion;
import net.calm.iaclasslibrary.Cell.Cytoplasm;
import net.calm.iaclasslibrary.Cell.Nucleus;
import net.calm.iaclasslibrary.IAClasses.Region;
import net.calm.iaclasslibrary.IO.DataWriter;
import ij.IJ;
import ij.ImageStack;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class FluorescenceAnalyser {

    public static double[][] analyseCellFluorescenceDistribution(ImageProcessor image, int measurements, Cell[] cells, double normFactor) {
        int N = cells.length;
        double[][] vals = new double[N][];
        ImageProcessor image2 = image.duplicate();
        image2.multiply(normFactor);
        for (int i = 0; i < N; i++) {
            Cell cell = cells[i];
            if (cell.getID() > 0) {
                Nucleus nucleus = (Nucleus) cell.getRegion(new Nucleus());
                Cytoplasm cyto = (Cytoplasm) cell.getRegion(new Cytoplasm());
                Roi nucRoi = nucleus.getRoi();
                ByteProcessor nucMask = (ByteProcessor) nucRoi.getMask();
                image2.setRoi(nucRoi);
                image2.setMask(nucMask);
                ImageStatistics nucstats = ImageStatistics.getStatistics(image2, measurements, null);
                nucleus.setFluorStats(nucstats);
                Roi cytoRoi = cyto.getRoi();
                ByteProcessor cytoMask = (ByteProcessor) cytoRoi.getMask();
                image2.setRoi(cytoRoi);
                image2.setMask(cytoMask);
                ImageStatistics cellstats = ImageStatistics.getStatistics(image2, measurements, null);
                cell.setFluorStats(cellstats);
                int xc = nucRoi.getBounds().x - cytoRoi.getBounds().x;
                int yc = nucRoi.getBounds().y - cytoRoi.getBounds().y;
                (new ByteBlitter(cytoMask)).copyBits(nucMask, xc, yc, Blitter.SUBTRACT);
                image2.setRoi(cytoRoi);
                image2.setMask(cytoMask);
                ImageStatistics cytostats = ImageStatistics.getStatistics(image2, measurements, null);
                cyto.setFluorStats(cytostats);
                vals[i] = new double[]{cell.getID(), nucleus.getCentroid()[0], nucleus.getCentroid()[1],
                    cellstats.mean, cellstats.stdDev, nucstats.mean, nucstats.stdDev, cytostats.mean,
                    cytostats.stdDev, nucstats.mean / cytostats.mean, nucstats.stdDev / cytostats.stdDev};
            }
        }
        return vals;
    }

    public static Cell[] filterCells(ImageProcessor image, CellRegion regionType, double threshold, int measurement, Cell[] cells, boolean aboveThreshold) {
        DescriptiveStatistics ds = new DescriptiveStatistics();
        boolean[] selected = new boolean[cells.length];
        Arrays.fill(selected, false);
        int b = 0;
        for (Cell cell : cells) {
            CellRegion cr = cell.getRegion(regionType);
            if (cr != null) {
                selected[b] = true;
                image.setRoi(cr.getRoi());
                ImageStatistics stats = ImageStatistics.getStatistics(image, measurement, null);
                switch (measurement) {
                    case Measurements.MEAN:
                        ds.addValue(stats.mean);
                        break;
                    case Measurements.STD_DEV:
                        ds.addValue(stats.stdDev);
                        break;
                    default:
                        ds.addValue(0.0);
                }
            }
            b++;
        }
        double percentile = threshold > 0.0 ? ds.getPercentile(threshold) : 0.0;
        double[] measures = ds.getValues();
        ArrayList<Cell> cells2 = new ArrayList<>();
        for (int i = 0, j = 0; i < cells.length; i++) {
            if (aboveThreshold && selected[i] && measures[j++] > percentile) {
                cells2.add(cells[i]);
            } else if (!aboveThreshold && selected[i] && measures[j++] < percentile) {
                cells2.add(cells[i]);
            }
        }
        return cells2.toArray(new Cell[]{});
    }

    public static ImageStack getMeanFluorDists(Cell[] cells, int height, ImageStack stack, int key, int steps, int stepSize) {
        ImageStack output = new ImageStack(1, height);
        Region[] regions = new Region[cells.length];
        for (int i = 0; i < cells.length; i++) {
            ByteProcessor mask = new ByteProcessor(stack.getWidth(), stack.getHeight());
            mask.setValue(Region.MASK_BACKGROUND);
            mask.fill();
            Cytoplasm cyto = (Cytoplasm) cells[i].getRegion(new Cytoplasm());
            if (cyto != null) {
                Roi roi = cyto.getRoi();
                ImageProcessor roiMask = roi.getMask().duplicate();
                roiMask.invert();
                (new ByteBlitter(mask)).copyBits(roiMask, roi.getBounds().x, roi.getBounds().y, Blitter.COPY);
                regions[i] = new Region(mask, cells[i].getNucleus().getCentroid());
            } else {
                regions[i] = null;
            }
        }
        for (Region region : regions) {
            output.addSlice(getFluorDists(height, stack, ImageProcessor.MAX, steps, stepSize, new Region[]{region}, 1, 1)[0]);
        }
        return output;
    }

    public static FloatProcessor[] getFluorDists(int height, ImageStack stack, int key, int steps, int stepSize, Region[] regions, int start, int end) {
        FloatProcessor[] dists = new FloatProcessor[2];
        int width = 1 + end - start;
        dists[0] = new FloatProcessor(width, height);
        dists[1] = new FloatProcessor(width, height);
        FloatBlitter meanBlitter = new FloatBlitter(dists[0]);
        FloatBlitter stdBlitter = new FloatBlitter(dists[1]);
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = start; i <= end; i++) {
            IJ.showStatus(String.format("Quantifying fluorescence localisation %d%%",(int)Math.round((i-start) * 100.0 / (end-start))));
            Region r = regions[i - 1];
            if (r == null) {
                continue;
            }
            Region current = new Region(r.getMask(), r.getCentre());
            ArrayList<Double> means = new ArrayList<>();
            ArrayList<Double> stds = new ArrayList<>();
            int index = 0, s = 0;
            while (current.morphFilter(stepSize, false, index, key) && s++ < steps) {
                float[][] pix = current.buildMapCol(stack.getProcessor(i), height, 3);
                if (pix != null) {
                    for (int j = 0; j < height; j++) {
                        stats.addValue(pix[j][2]);
                    }
                    means.add(stats.getMean());
                    stds.add(stats.getStandardDeviation());
                    index++;
                }
            }
            if (index > 0) {
                FloatProcessor meanCol = new FloatProcessor(1, means.size());
                FloatProcessor stdCol = new FloatProcessor(1, stds.size());
                for (int k = 0; k < meanCol.getHeight(); k++) {
                    meanCol.putPixelValue(0, k, means.get(k));
                    stdCol.putPixelValue(0, k, stds.get(k));
                }
                meanCol.setInterpolate(true);
                meanCol.setInterpolationMethod(ImageProcessor.BILINEAR);
                stdCol.setInterpolate(true);
                stdCol.setInterpolationMethod(ImageProcessor.BILINEAR);
                meanBlitter.copyBits(meanCol.resize(1, height), i - start, 0, Blitter.COPY);
                stdBlitter.copyBits(stdCol.resize(1, height), i - start, 0, Blitter.COPY);
            }
        }
        return dists;
    }

    public static void generateFluorMapsPerCellOverTime(FloatProcessor[] fluorMaps, File childDir) {
        File mean;
        File std;
        PrintWriter meanStream;
        PrintWriter stdStream;
        try {
            mean = new File(childDir + File.separator + "MeanFluorescenceIntensity.csv");
            meanStream = new PrintWriter(new FileOutputStream(mean));
            std = new File(childDir + File.separator + "STDFluorescenceIntensity.csv");
            stdStream = new PrintWriter(new FileOutputStream(std));
            meanStream.print("Normalised Distance from net.calm.iaclasslibrary.Cell Edge,");
            stdStream.print("Normalised Distance from net.calm.iaclasslibrary.Cell Edge,");
            int mapHeight = fluorMaps[0].getHeight();
            int mapWidth = fluorMaps[0].getWidth();
            for (int x = 0; x < mapWidth; x++) {
                meanStream.print("Mean net.calm.iaclasslibrary.Fluorescence Intensity (AU) Frame " + x + ",");
                stdStream.print("Standard Deviation of net.calm.iaclasslibrary.Fluorescence Intensity (AU) Frame " + x + ",");
            }
            meanStream.println();
            stdStream.println();
            for (int y = 0; y < mapHeight; y++) {
                String normDist = String.valueOf(((double) y) / (mapHeight - 1.0));
                meanStream.print(normDist + ",");
                stdStream.print(normDist + ",");
                for (int x = 0; x < mapWidth; x++) {
                    meanStream.print(fluorMaps[0].getPixelValue(x, y) + ",");
                    stdStream.print(fluorMaps[1].getPixelValue(x, y) + ",");
                }
                meanStream.println();
                stdStream.println();
            }
            meanStream.close();
            stdStream.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.toString());
        }
    }

    public static void generateFluorMapsFromStack(ImageStack fluorMaps, String dir, String[] headings) {
        File mean = new File(String.format("%s%s%s", dir, File.separator, "MeanFluorescenceIntensity.csv"));
        headings = ArrayUtils.addAll(new String[]{"Normalised Distance From Boundary"}, headings);
        int mapHeight = fluorMaps.getHeight();
        int mapWidth = fluorMaps.getWidth();
        double[][] data = new double[mapHeight][fluorMaps.getSize() + 1];
        for (int y = 0; y < mapHeight; y++) {
            data[y][0] = ((double) y) / (mapHeight - 1);
            for (int i = 1; i <= fluorMaps.size(); i++) {
                ImageProcessor fluorMap = fluorMaps.getProcessor(i);
                for (int x = 0; x < mapWidth; x++) {
                    data[y][i] = fluorMap.getPixelValue(x, y);
                }
            }
        }
        try {
            DataWriter.saveValues(data, mean, headings, null, false);
        } catch (IOException e) {
            IJ.log(String.format("Failed to save mean fluorescence data: %s", e.toString()));
        }
    }

}
