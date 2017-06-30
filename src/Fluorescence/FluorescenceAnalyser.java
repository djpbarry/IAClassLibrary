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
package Fluorescence;

import Cell.Cell;
import Cell.CellRegion;
import Cell.Cytoplasm;
import Cell.Nucleus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class FluorescenceAnalyser {

    public static double[][] analyseCellFluorescenceDistribution(ImageProcessor image, int measurements, Cell[] cells) {
        int N = cells.length;
        double[][] vals = new double[N][];
        for (int i = 0; i < N; i++) {
            Cell cell = cells[i];
            if (cell.getID() > 0) {
                Nucleus nucleus = (Nucleus) cell.getRegion(new Nucleus());
                Cytoplasm cyto = (Cytoplasm) cell.getRegion(new Cytoplasm());
                Roi nucRoi = nucleus.getRoi();
                ByteProcessor nucMask = (ByteProcessor) nucRoi.getMask();
                image.setRoi(nucRoi);
                image.setMask(nucMask);
                ImageStatistics nucstats = ImageStatistics.getStatistics(image, measurements, null);
                Roi cytoRoi = cyto.getRoi();
                ByteProcessor cytoMask = (ByteProcessor) cytoRoi.getMask();
                image.setRoi(cytoRoi);
                image.setMask(cytoMask);
                ImageStatistics cellstats = ImageStatistics.getStatistics(image, measurements, null);
                int xc = nucRoi.getBounds().x - cytoRoi.getBounds().x;
                int yc = nucRoi.getBounds().y - cytoRoi.getBounds().y;
                (new ByteBlitter(cytoMask)).copyBits(nucMask, xc, yc, Blitter.SUBTRACT);
                image.setRoi(cytoRoi);
                image.setMask(cytoMask);
                ImageStatistics cytostats = ImageStatistics.getStatistics(image, measurements, null);
                vals[i] = new double[]{cell.getID(), cellstats.mean, cellstats.stdDev, nucstats.mean, nucstats.stdDev, cytostats.mean, cytostats.stdDev, nucstats.mean / cytostats.mean, nucstats.stdDev / cytostats.stdDev};
            }
        }
        return vals;
    }

    public static Cell[] filterCells(ImageProcessor image, CellRegion regionType, double threshold, int measurement, Cell[] cells) {
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
        ArrayList<Cell> cells2 = new ArrayList();
        for (int i = 0, j = 0; i < cells.length; i++) {
            if (selected[i] && measures[j++] > percentile) {
                cells2.add(cells[i]);
            }
        }
        return cells2.toArray(new Cell[]{});
    }
    
}
