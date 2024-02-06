/*
 * Copyright (C) 2019 David Barry <david.barry at crick dot ac dot uk>
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
package net.calm.iaclasslibrary.Process.Colocalise;

import fiji.plugin.trackmate.Spot;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.process.StackProcessor;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom.Vector3D;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.distanceMap3d.EDT;
import net.calm.iaclasslibrary.Cell3D.Cell3D;
import net.calm.iaclasslibrary.Cell3D.CellRegion3D;
import net.calm.iaclasslibrary.Cell3D.Spot3D;
import net.calm.iaclasslibrary.Cell3D.SpotFeatures;
import net.calm.iaclasslibrary.Extrema.MultiThreadedMaximaFinder;
import net.calm.iaclasslibrary.IAClasses.Utils;
import net.calm.iaclasslibrary.IO.BioFormats.LocationAgnosticBioFormatsImg;
import net.calm.iaclasslibrary.IO.DataWriter;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import net.calm.iaclasslibrary.Process.ROI.MultiThreadedROIConstructor;
import net.calm.iaclasslibrary.Process.ROI.OverlayDrawer;
import net.calm.iaclasslibrary.Stacks.StackThresholder;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javax.xml.stream.Location;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedColocalise extends MultiThreadedProcess {

    public static int SERIES_LABEL = 0;
    public static int CHANNELS_LABEL = 1;
    public static int OUTPUT_LABEL = 2;
    public static int N_PROP_LABELS = 3;
    private Objects3DPopulation cellPop;
    private int series;
    private int selectedChannels;
    private int spotIndex;
    public static String CELL_INDEX = "Cell_Index";
    public static String N_SPOTS = "Number_of_Spots";
    public static String MEAN_NUC_DIST = "Mean_Distance_To_Nuclear_Centre_Microns";
    public static String MEAN_NEIGHBOUR_DIST = "Mean_Distance_To_Nearest_Neighbour_Microns";
    public static String MEAN_INTENS = "Mean_Intensity";

    public MultiThreadedColocalise(MultiThreadedProcess[] inputs, Objects3DPopulation cellPop) {
        super(inputs);
        this.cellPop = cellPop;
    }

    public void setup(LocationAgnosticBioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
        this.series = Integer.parseInt(props.getProperty(propLabels[SERIES_LABEL]));
        this.selectedChannels = Integer.parseInt(props.getProperty(propLabels[CHANNELS_LABEL]));
    }

    public void run() {
        this.spotIndex = 1;
        int lInputs = inputs.length;
        IJ.log("Assigning particles to cells...");
        for (int i = 0; i < inputs.length - 2; i++) {
            assignParticlesToCells(((MultiThreadedMaximaFinder) inputs[i]).getSpotMaxima(), inputs[lInputs - 2].getOutput(), inputs[lInputs - 1].getOutput());
        }
        //IJ.log("Calculating nuclear-particle distances...");
        //calcNucParticleDistances(inputs[lInputs - 1].getOutput());
        //IJ.log("Calculating inter-particle distances...");
        //calcNearestNeighbours();
        try {
            saveData();
        } catch (Exception e) {
            GenUtils.logError(e, "Failed to save spot data file.");
        }
    }

    public MultiThreadedColocalise duplicate() {
        MultiThreadedColocalise newProcess = new MultiThreadedColocalise(inputs, cellPop);
        this.updateOutputDests(newProcess);
        return newProcess;
    }

    void assignParticlesToCells(List<Spot3D> spots, ImagePlus cellLabelImage, ImagePlus nucLabelImage) {
        labelOutput(cellLabelImage.getTitle(), CellRegion3D.SPOT);
        ImageStack cellLabelStack = cellLabelImage.getImageStack();
        ImageStack nucLabelStack = nucLabelImage.getImageStack();
        double xySpatialRes = img.getXYSpatialRes(series).value().doubleValue();
        double zSpatialRes = img.getZSpatialRes(series).value().doubleValue();
        int N = spots.size();
        LinkedHashMap<Integer, Integer> idToIndexMap = new LinkedHashMap<Integer, Integer>();
        List<Object3D> cells = cellPop.getObjectsList();
        for (int i = 0; i < cells.size(); i++) {
            idToIndexMap.put(((Cell3D) cells.get(i)).getID(), i);
        }
        for (int i = 0; i < N; i++) {
            Spot3D spot = spots.get(i);
            Spot p = spot.getSpot();
            int xp = (int) Math.round(p.getFeature(Spot.POSITION_X) / xySpatialRes);
            int yp = (int) Math.round(p.getFeature(Spot.POSITION_Y) / xySpatialRes);
            int zp = (int) Math.round(p.getFeature(Spot.POSITION_Z) / zSpatialRes);
            int cellValue = ((int) Math.round(cellLabelStack.getVoxel(xp, yp, zp)));
            Integer cellIndex = idToIndexMap.get(cellValue);
            int nucLabelValue = (int) Math.round(nucLabelStack.getVoxel(xp, yp, zp));
            if (cellIndex != null) {
                if (nucLabelValue > 0) {
                    p.putFeature(SpotFeatures.NUCLEAR, 1.0);
                } else {
                    p.putFeature(SpotFeatures.NUCLEAR, 0.0);
                }
                if (spot.getVoxels().size() <= 1) {
                    spot = new Spot3D(p, OverlayDrawer.showOutput(p, null, img, props, null, false, p.getFeature(Spot.RADIUS), p.getFeature(Spot.RADIUS), cellIndex, series));
                }
                if (spot.getVoxels().size() < 1) {
                    continue;
                }
                spot.setComment("Channel_" + (int) Math.round(p.getFeature(SpotFeatures.CHANNEL)) + "_" + CellRegion3D.SPOT);
                spot.setName(String.format("%s_%s_%d_%d", output.getTitle(), "Channel",
                        (int) Math.round(p.getFeature(SpotFeatures.CHANNEL)), spotIndex++));
                spot.setValue(cellValue);
                ((Cell3D) cells.get(cellIndex)).addSpot(spot, (int) Math.round(p.getFeature(SpotFeatures.CHANNEL)));
            }
        }
    }

    void calcNucParticleDistances(ImagePlus nucLabelImage) {
        ImagePlus distanceMap = generateNuclearDistanceMap(nucLabelImage);
        int nThreads = Runtime.getRuntime().availableProcessors();
        List<Object3D> cells = cellPop.getObjectsList();
        SpotNucDistanceCalc[] distanceCalcs = new SpotNucDistanceCalc[nThreads];
        for (int thread = 0; thread < nThreads; thread++) {
            distanceCalcs[thread] = new SpotNucDistanceCalc(cells, thread, nThreads, distanceMap);
            distanceCalcs[thread].start();
        }
        try {
            for (int thread = 0; thread < nThreads; thread++) {
                distanceCalcs[thread].join();
            }
        } catch (InterruptedException ie) {
            GenUtils.logError(ie, "Problem encountered calculating cell-spot distances.");
        }
    }

    void calcNearestNeighbours() {
        List<Object3D> cells = cellPop.getObjectsList();
        int nThreads = Runtime.getRuntime().availableProcessors();
        SpotSpotDistanceCalc[] distanceCalcs = new SpotSpotDistanceCalc[nThreads];
        for (int thread = 0; thread < nThreads; thread++) {
            distanceCalcs[thread] = new SpotSpotDistanceCalc(cells, thread, nThreads);
            distanceCalcs[thread].start();
        }
        try {
            for (int thread = 0; thread < nThreads; thread++) {
                distanceCalcs[thread].join();
            }
        } catch (InterruptedException ie) {
            GenUtils.logError(ie, "Problem encountered calculating spot-spot distances.");
        }
    }

    void saveData() throws IOException {
        ResultsTable rt = new ResultsTable();
        List<Object3D> cells = cellPop.getObjectsList();
        Objects3DPopulation[] spotsPop = new Objects3DPopulation[img.getSizeC(series)];
        for (int i = 0; i < spotsPop.length; i++) {
            spotsPop[i] = new Objects3DPopulation();
        }
        for (Object3D c : cells) {
            ArrayList<ArrayList<Object3D>> allSpots = ((Cell3D) c).getSpots();
            int row = rt.getCounter();
            rt.setValue(CELL_INDEX, row, ((Cell3D) c).getID());
            if (allSpots != null) {
                for (ArrayList<Object3D> spots : allSpots) {
                    if (spots.size() < 1) {
                        continue;
                    }
                    for (Object3D s : spots) {
                        spotsPop[(int) Math.round(((Spot3D) s).getSpot().getFeature(SpotFeatures.CHANNEL))].addObject(s);
                    }
                    LinkedHashMap<String, DescriptiveStatistics> map = new LinkedHashMap<>();
                    for (Object3D spot : spots) {
                        Spot s = ((Spot3D) spot).getSpot();
                        Iterator<Entry<String, Double>> iter = s.getFeatures().entrySet().iterator();
                        while (iter.hasNext()) {
                            Entry<String, Double> e = iter.next();
                            DescriptiveStatistics stats = map.get(e.getKey());
                            if (stats == null) {
                                stats = new DescriptiveStatistics();
                                map.put(e.getKey(), stats);
                            }
                            stats.addValue(e.getValue());
                        }
                    }
                    Iterator<Entry<String, DescriptiveStatistics>> iter = map.entrySet().iterator();
                    rt.setValue(String.format("%s_C%d", N_SPOTS, (int) Math.round(((Spot3D) spots.get(0)).getSpot().getFeature(SpotFeatures.CHANNEL))), row, spots.size());
                    while (iter.hasNext()) {
                        Entry<String, DescriptiveStatistics> e = iter.next();
                        DescriptiveStatistics stats = e.getValue();
                        rt.setValue(String.format("Mean_%s_C%d", e.getKey(), (int) Math.round(((Spot3D) spots.get(0)).getSpot().getFeature(SpotFeatures.CHANNEL))), row, stats.getMean());
                    }
                }
            }
        }
        String outputDir = props.getProperty(propLabels[OUTPUT_LABEL]);
        DataWriter.saveResultsTable(rt, new File(String.format("%s%s%s", outputDir, File.separator, "spot_summary_data.csv")), false, true);
        for (Objects3DPopulation pop : spotsPop) {
            MultiThreadedROIConstructor.processObjectPop(pop, series, selectedChannels, img);
            MultiThreadedROIConstructor.saveAllRois(outputDir, pop);
            MultiThreadedROIConstructor.saveAllMasks(outputDir, pop, img, series);
        }
    }

    private ImagePlus generateNuclearDistanceMap(ImagePlus nucLabelImage) {
        double[] calibration = getCalibration(series);
        ImagePlus mask = nucLabelImage.duplicate();
        StackThresholder.thresholdStack(mask, 0);
        ImageFloat edtInner = EDT.run(ImageHandler.wrap(mask), 1, (float) calibration[0], (float) calibration[2], true, Runtime.getRuntime().availableProcessors());
        (new StackProcessor(mask.getImageStack())).invert();
        ImageFloat edtOuter = EDT.run(ImageHandler.wrap(mask), 1, (float) calibration[0], (float) calibration[2], true, Runtime.getRuntime().availableProcessors());
        edtOuter.subtract(edtInner);
        return edtOuter.getImagePlus();
    }

    class SpotNucDistanceCalc extends Thread {

        private final List<Object3D> cells;
        private final int thread;
        private final int nThreads;
        private final ImagePlus distanceMap;

        public SpotNucDistanceCalc(List<Object3D> cells, int thread, int nThreads, ImagePlus distanceMap) {
            this.cells = cells;
            this.thread = thread;
            this.nThreads = nThreads;
            this.distanceMap = distanceMap;
        }

        public void run() {
            double[] calibration = getCalibration(series);
            ImageStack distanceMapStack = distanceMap.getImageStack();
            for (int t = thread; t < cells.size(); t += nThreads) {
                Cell3D c = (Cell3D) cells.get(t);
                Vector3D centroid = c.getNucleus().getCenterAsVectorUnit();
                double[] nucCentroid = new double[]{centroid.x, centroid.y, centroid.z};
                ArrayList<ArrayList<Object3D>> allSpots = c.getSpots();
                if (allSpots != null) {
                    int M = allSpots.size();
                    for (int j = 0; j < M; j++) {
                        ArrayList<Object3D> spots = allSpots.get(j);
                        int L = spots.size();
                        for (int k = 0; k < L; k++) {
                            Spot3D s = (Spot3D) spots.get(k);
                            double[] spotPosition = new double[3];
                            s.getSpot().localize(spotPosition);
                            s.getSpot().putFeature(SpotFeatures.EUCLID_DIST_TO_NUC_CENTRE, Utils.calcEuclidDist(nucCentroid, spotPosition));
                            s.getSpot().putFeature(SpotFeatures.DIST_TO_NUC_MEMBRANE,
                                    distanceMapStack.getVoxel(
                                            (int) Math.round(spotPosition[0] / calibration[0]),
                                            (int) Math.round(spotPosition[1] / calibration[1]),
                                            (int) Math.round(spotPosition[2] / calibration[2])));
                        }
                    }
                }
            }
        }
    }

    class SpotSpotDistanceCalc extends Thread {

        private final List<Object3D> cells;
        private final int thread;
        private final int nThreads;

        public SpotSpotDistanceCalc(List<Object3D> cells, int thread, int nThreads) {
            this.cells = cells;
            this.thread = thread;
            this.nThreads = nThreads;
        }

        public void run() {
            for (int t = thread; t < cells.size(); t += nThreads) {
                Cell3D c = (Cell3D) cells.get(t);
                ArrayList<ArrayList<Object3D>> allSpots = c.getSpots();
                if (allSpots != null) {
                    int nAllSpots = allSpots.size();
                    for (int allSpotsIndex = 0; allSpotsIndex < nAllSpots; allSpotsIndex++) {
                        calcSpotSpotDistances(allSpots, allSpots.get(allSpotsIndex), allSpotsIndex);
                    }
                }
            }
        }

        void calcSpotSpotDistances(ArrayList<ArrayList<Object3D>> allSpots, ArrayList<Object3D> parentSpots, int allSpotsIndex) {
            int nAllSpots = allSpots.size();
            int nParentSpots = parentSpots.size();
            for (int parentSpotIndex = 0; parentSpotIndex < nParentSpots; parentSpotIndex++) {
                Spot3D parentSpot = (Spot3D) parentSpots.get(parentSpotIndex);
                double[] parentSpotPos = new double[3];
                parentSpot.getSpot().localize(parentSpotPos);
                for (int allSpotsColocIndex = 0; allSpotsColocIndex < nAllSpots; allSpotsColocIndex++) {
                    if (allSpotsColocIndex == allSpotsIndex) {
                        continue;
                    }
                    ArrayList<Object3D> colocSpots = allSpots.get(allSpotsColocIndex);
                    int nColocSpots = colocSpots.size();
                    double minDist = Double.MAX_VALUE;
                    int channel = -1;
                    for (int colocSpotIndex = 0; colocSpotIndex < nColocSpots; colocSpotIndex++) {
                        Spot3D colocSpot = (Spot3D) colocSpots.get(colocSpotIndex);
                        double[] colocSpotPos = new double[3];
                        colocSpot.getSpot().localize(colocSpotPos);
                        double dist = Utils.calcEuclidDist(parentSpotPos, colocSpotPos);
                        if (dist < minDist) {
                            minDist = dist;
                            channel = (int) Math.round(colocSpot.getSpot().getFeature(SpotFeatures.CHANNEL));
                        }
                    }
                    if (!(minDist < Double.MAX_VALUE)) {
                        minDist = -1.0;
                    }
                    parentSpot.getSpot().putFeature(String.format("%s_C%d", SpotFeatures.DIST_TO_NEAREST_NEIGHBOUR, channel), minDist);
                }
            }
        }
    }
}
