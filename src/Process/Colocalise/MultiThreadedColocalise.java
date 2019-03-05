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
import Process.MultiThreadedProcess;
import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import ij.ImageStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedColocalise extends MultiThreadedProcess {

    private ArrayList<Cell3D> cells;

    public MultiThreadedColocalise(MultiThreadedProcess[] inputs, ArrayList<Cell3D> cells) {
        super(inputs);
        this.cells = cells;
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {

    }

    public void run() {
        assignParticlesToCells(((MultiThreadedMaximaFinder) inputs[0]).getSpotMaxima(), inputs[1].getOutput(), inputs[2].getOutput());
    }

    public MultiThreadedColocalise duplicate() {
        MultiThreadedColocalise newProcess = new MultiThreadedColocalise(inputs, cells);
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
        for (int i = 0; i < cells.size(); i++) {
            idToIndexMap.put(cells.get(i).getID(), i);
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
                cells.get(cellLabelValue).addSpot(p);
            }
        }
    }

    void calcNucParticleDistances() {
        int N = cells.size();
        for (int i = 0; i < N; i++) {
            Cell3D c = cells.get(i);
            double[] nucCentroid = c.getNucleus().getCentroid();
            ArrayList<Spot> spots = c.getSpots();
            if (spots != null) {
                int M = spots.size();
                for (int j = 0; j < M; j++) {
                    Spot s = spots.get(j);
                    double[] spotPosition = new double[3];
                    s.localize(spotPosition);
                    s.putFeature(SpotFeatures.DIST_TO_NUC_CENTRE, Utils.calcEuclidDist(nucCentroid, spotPosition));
                }
            }
        }
    }
}
