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
package Process.Colocalise;

import Cell3D.Cell3D;
import Cell3D.SpotFeatures;
import Extrema.MultiThreadedMaximaFinder;
import IAClasses.Utils;
import IO.BioFormats.BioFormatsImg;
import IO.DataWriter;
import Process.MultiThreadedProcess;
import UtilClasses.GenUtils;
import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedColocalise extends MultiThreadedProcess {

    private Objects3DPopulation cellPop;

    public MultiThreadedColocalise(MultiThreadedProcess[] inputs, Objects3DPopulation cellPop) {
        super(inputs);
        this.cellPop = cellPop;
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
    }

    public void run() {
        int lInputs = inputs.length;
        for (int i = 0; i < inputs.length - 2; i++) {
            assignParticlesToCells(((MultiThreadedMaximaFinder) inputs[i]).getSpotMaxima(), inputs[lInputs - 2].getOutput(), inputs[lInputs - 1].getOutput());
        }
        calcNucParticleDistances();
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

    void assignParticlesToCells(List<Spot> spots, ImagePlus cellLabelImage, ImagePlus nucLabelImage) {
        ImageStack cellLabelStack = cellLabelImage.getImageStack();
        ImageStack nucLabelStack = nucLabelImage.getImageStack();
        int series = Integer.parseInt(props.getProperty(propLabels[0]));
        double xySpatialRes = img.getXYSpatialRes(series).value().doubleValue();
        double zSpatialRes = img.getZSpatialRes(series).value().doubleValue();
        int N = spots.size();
        LinkedHashMap<Integer, Integer> idToIndexMap = new LinkedHashMap<Integer, Integer>();
        ArrayList<Object3D> cells = cellPop.getObjectsList();
        for (int i = 0; i < cells.size(); i++) {
            idToIndexMap.put(((Cell3D) cells.get(i)).getID(), i);
        }
        for (int i = 0; i < N; i++) {
            Spot p = spots.get(i);
            int xp = (int) Math.round(p.getFeature(Spot.POSITION_X) / xySpatialRes);
            int yp = (int) Math.round(p.getFeature(Spot.POSITION_Y) / xySpatialRes);
            int zp = (int) Math.round(p.getFeature(Spot.POSITION_Z) / zSpatialRes);
            Integer cellLabelValue = idToIndexMap.get((int) Math.round(cellLabelStack.getVoxel(xp, yp, zp)));
            int nucLabelValue = (int) Math.round(nucLabelStack.getVoxel(xp, yp, zp));
            if (cellLabelValue != null) {
                if (nucLabelValue > 0) {
                    p.putFeature(SpotFeatures.NUCLEAR, 1.0);
                } else {
                    p.putFeature(SpotFeatures.NUCLEAR, 0.0);
                }
                ((Cell3D) cells.get(cellLabelValue)).addSpot(p, (int) Math.round(p.getFeature(SpotFeatures.CHANNEL)));
            }
        }
    }

    void calcNucParticleDistances() {
        ArrayList<Object3D> cells = cellPop.getObjectsList();
        int N = cells.size();
        for (int i = 0; i < N; i++) {
            Cell3D c = (Cell3D) cells.get(i);
            double[] nucCentroid = c.getNucleus().getCenterAsArray();
            ArrayList<ArrayList<Spot>> allSpots = c.getSpots();
            if (allSpots != null) {
                int M = allSpots.size();
                for (int j = 0; j < M; j++) {
                    ArrayList<Spot> spots = allSpots.get(j);
                    int L = spots.size();
                    for (int k = 0; k < L; k++) {
                        Spot s = spots.get(k);
                        double[] spotPosition = new double[3];
                        s.localize(spotPosition);
                        s.putFeature(SpotFeatures.DIST_TO_NUC_CENTRE, Utils.calcEuclidDist(nucCentroid, spotPosition));
                    }
                }
            }
        }
    }

    void calcNearestNeighbours() {
        ArrayList<Object3D> cells = cellPop.getObjectsList();
        int N = cells.size();
        for (int i = 0; i < N; i++) {
            Cell3D c = (Cell3D) cells.get(i);
            double[] nucCentroid = c.getNucleus().getCentroid();
            ArrayList<ArrayList<Spot>> allSpots = c.getSpots();
            if (allSpots != null) {
                int M = allSpots.size();
                for (int j = 0; j < M; j++) {
                    ArrayList<Spot> spots = allSpots.get(j);
                    int L = spots.size();
                    for (int k = 0; k < L; k++) {
                        Spot s = spots.get(k);
                        double[] spotPosition = new double[3];
                        s.localize(spotPosition);
                        s.putFeature(SpotFeatures.DIST_TO_NUC_CENTRE, Utils.calcEuclidDist(nucCentroid, spotPosition));
                    }
                }
            }
        }
    }

    void saveData() throws IOException {
        ResultsTable rt = new ResultsTable();
        ArrayList<Object3D> cells = cellPop.getObjectsList();
        for (Object3D c : cells) {
            ArrayList<ArrayList<Spot>> allSpots = ((Cell3D) c).getSpots();
            if (allSpots != null) {
                for (ArrayList<Spot> spots : allSpots) {
                    for (Spot s : spots) {
                        rt.incrementCounter();
                        rt.addValue("Cell ID", c.getValue());
                        rt.addValue(SpotFeatures.CHANNEL, s.getFeature(SpotFeatures.CHANNEL));
                        rt.addValue(SpotFeatures.NUCLEAR, s.getFeature(SpotFeatures.NUCLEAR));
                        rt.addValue(SpotFeatures.DIST_TO_NUC_CENTRE, s.getFeature(SpotFeatures.DIST_TO_NUC_CENTRE));
                    }
                }
            }
        }
        DataWriter.saveResultsTable(rt, new File(String.format("%s%s%s", props.getProperty(propLabels[1]), File.separator, "Spot_Data.csv")));
    }
}
