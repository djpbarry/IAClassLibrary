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
package Process.ROI;

import Cell3D.Cell3D;
import Cell3D.CellRegion3D;
import Cell3D.Cytoplasm3D;
import Cell3D.Nucleus3D;
import Cell3D.Spot3D;
import Cell3D.SpotFeatures;
import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import fiji.plugin.trackmate.Spot;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom.Vector3D;
import org.apache.commons.math3.linear.ArrayRealVector;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedROIConstructor extends MultiThreadedProcess {

    public static int SERIES_LABEL = 0;
    public static int CHANNELS_LABEL = 1;
    public static int OUTPUT_LABEL = 2;
    public static int LOCALISE_LABEL = 3;
    public static int N_PROP_LABELS = 4;
    private final Objects3DPopulation cells;
    int selectedChannels;
    int series;
    private String outputPath;
    public static String[] PIX_HEADINGS = {"Channel", "Cell_Index", "Mean_Pixel_Value",
        "Pixel_Standard_Deviation", "Min_Pixel_Value", "Max_Pixel_Value", "Integrated_Density"};
    public static String LOCAT_HEAD = "Normalised_Distance_to_Centre";
    public static String X_CENTROID = "Centroid_X";
    public static String Y_CENTROID = "Centroid_Y";
    public static String Z_CENTROID = "Centroid_Z";

    public MultiThreadedROIConstructor(MultiThreadedProcess[] inputs) {
        this(inputs, null);
    }

    public MultiThreadedROIConstructor(MultiThreadedProcess[] inputs, Objects3DPopulation cells) {
        super(inputs);
        this.cells = cells;
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
        this.series = Integer.parseInt(props.getProperty(propLabels[SERIES_LABEL]));
        this.selectedChannels = Integer.parseInt(props.getProperty(propLabels[CHANNELS_LABEL]));
        this.outputPath = props.getProperty(propLabels[OUTPUT_LABEL]);
    }

    @Override
    public void run() {
        for (MultiThreadedProcess p : inputs) {
            p.getOutput();
        }
        Objects3DPopulation[] subPops = new Objects3DPopulation[3];
        for (int c = 0; c < subPops.length; c++) {
            subPops[c] = new Objects3DPopulation();
        }
        ArrayList<Object3D> cellPop = cells.getObjectsList();
        for (Object3D cell : cellPop) {
            Nucleus3D nuc = ((Cell3D) cell).getNucleus();
            nuc.setComment(CellRegion3D.NUCLEUS);
            subPops[0].addObject(nuc);
            Cytoplasm3D cyto = ((Cell3D) cell).getCytoplasm();
            if (cyto != null) {
                cyto.setComment(CellRegion3D.CYTO);
                subPops[1].addObject(cyto);
            }
            Cell3D combinedCell = new Cell3D();
            if (cyto != null) {
                combinedCell.addVoxelsUnion(nuc, cyto);
            } else {
                combinedCell.addVoxels(nuc.getVoxels());
            }
            combinedCell.setName(String.format("%s_%d", constructOutputName(inputs[0].getOutput().getTitle(), CellRegion3D.CELL), nuc.getValue()));
            combinedCell.setValue(nuc.getValue());
            combinedCell.setComment(CellRegion3D.CELL);
            subPops[2].addObject(combinedCell);
        }
        for (Objects3DPopulation pop : subPops) {
            processObjectPop(pop, series, selectedChannels, img);
            saveAllRois(outputPath, pop);
        }
    }

    public static void processObjectPop(Objects3DPopulation cells, int series, int selectedChannels, BioFormatsImg img) {
        if (cells.getNbObjects() < 1) {
            return;
        }
        String calUnit = img.getXYSpatialRes(series).unit().getSymbol();
        String[] geomHeadings = getGeomHeadings(calUnit);
        cells.setCalibration(img.getXYSpatialRes(series).value().doubleValue(), img.getZSpatialRes(series).value().doubleValue(), calUnit);
        ResultsTable rt = Analyzer.getResultsTable();
        ArrayList<double[]> geomMeasures = cells.getMeasuresGeometrical();
        double[] distMeasures = getLocationMetrics(cells);
        int firstRow = rt.getCounter();
        IJ.log(String.format("Measuring geometry of %s.", cells.getObject(0).getName()));
        for (int i = 0; i < geomMeasures.size(); i++) {
            int row = firstRow + i;
            double[] geomM = geomMeasures.get(i);
            Object3D object = cells.getObject(i);
            rt.setValue(PIX_HEADINGS[1], row, object.getValue());
            for (int j = 0; j < geomM.length; j++) {
                rt.setValue(geomHeadings[j], row, geomM[j]);
            }
            Vector3D centre = object.getCenterAsVectorUnit();
            rt.setValue(X_CENTROID, row, centre.x);
            rt.setValue(Y_CENTROID, row, centre.y);
            rt.setValue(Z_CENTROID, row, centre.z);
            rt.setValue(LOCAT_HEAD, row, distMeasures[i]);
            if (object instanceof Spot3D) {
                Spot s = ((Spot3D) object).getSpot();
                rt.setValue(SpotFeatures.NUCLEAR, row, (int) Math.round(s.getFeature(SpotFeatures.NUCLEAR)));
                Iterator<Entry<String, Double>> iter = s.getFeatures().entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<String, Double> e = iter.next();
                    if (e.getKey().contains(SpotFeatures.EUCLID_DIST_TO_NUC_CENTRE)) {
                        rt.setValue(e.getKey(), row, e.getValue());
                    } else if (e.getKey().contains(SpotFeatures.DIST_TO_NEAREST_NEIGHBOUR)) {
                        rt.setValue(e.getKey(), row, e.getValue());
                    } else if (e.getKey().contains(SpotFeatures.DIST_TO_NUC_MEMBRANE)) {
                        rt.setValue(e.getKey(), row, e.getValue());
                    }
                }
            }
            rt.setLabel(cells.getObject(i).getName(), row);
        }
        int nChan = img.getSizeC(series);
        for (int c = 0; c < nChan; c++) {
            if (((int) Math.pow(2, c) & selectedChannels) != 0) {
                img.loadPixelData(series, c, c + 1, null);
                ImagePlus imp = img.getLoadedImage();
                IJ.log(String.format("Measuring %s defined by %s.", imp.getTitle(), cells.getObject(0).getName()));
                ArrayList<double[]> pixMeasures = cells.getMeasuresStats(imp.getImageStack());
                for (int i = 0; i < pixMeasures.size(); i++) {
                    int row = firstRow + i;
                    double[] pixM = pixMeasures.get(i);
//                    rt.incrementCounter();
//                    rt.addLabel(cells.getObject(i).getName());
//                    rt.setValue(PIX_HEADINGS[0], row, c);
                    rt.setValue(PIX_HEADINGS[1], row, cells.getObject(i).getValue());
                    for (int j = 2; j <= pixM.length; j++) {
                        rt.setValue(String.format("%s_C%d", PIX_HEADINGS[j], c), row, pixM[j - 1]);
                    }
                }
            }
        }
    }

    public Objects3DPopulation getObjectPop() {
        return cells;
    }

    public MultiThreadedROIConstructor duplicate() {
        MultiThreadedROIConstructor newProcess = new MultiThreadedROIConstructor(inputs, cells);
        this.updateOutputDests(newProcess);
        return newProcess;
    }

    public static void saveAllRois(String path, Objects3DPopulation cells) {
        if (path == null || !new File(path).exists() || cells.getNbObjects() < 1) {
            return;
        }
        String outputName = constructOutputName(cells.getObject(0).getName(), cells.getObject(0).getComment());
        for (int c = 0; c < cells.getNbObjects(); c++) {
            cells.getObject(c).setComment("");
        }
        cells.saveObjects(String.format("%s%s%s.zip", path, File.separator, outputName));
    }

    public static String[] getGeomHeadings(String calUnit) {
        return new String[]{"Index", "Volume (Voxels)",
            String.format("Volume (%s^3)", calUnit),
            "Surface Area (Voxels)",
            String.format("Surface Area (%s^2)", calUnit)};
    }

    public static double[] getLocationMetrics(Objects3DPopulation cells) {
        double xSum = 0.0;
        double ySum = 0.0;
        double zSum = 0.0;
        ArrayList<Object3D> cellObjects = cells.getObjectsList();
        for (Object3D c : cellObjects) {
            Vector3D centroid = c.getCenterAsVectorUnit();
            xSum += centroid.x;
            ySum += centroid.y;
            zSum += centroid.z;
        }
        ArrayRealVector volumeCentroid = new ArrayRealVector(new double[]{xSum / cells.getNbObjects(), ySum / cells.getNbObjects(), zSum / cells.getNbObjects()});
        double[] distances = new double[cells.getNbObjects()];
        double maxDist = -Double.MAX_VALUE;
        for (int i = 0; i < cells.getNbObjects(); i++) {
            Vector3D c = cellObjects.get(i).getCenterAsVectorUnit();
            distances[i] = volumeCentroid.getDistance(new ArrayRealVector(new double[]{c.x, c.y, c.z}));
            if (distances[i] > maxDist) {
                maxDist = distances[i];
            }
        }
        for (int i = 0; i < distances.length; i++) {
            distances[i] = distances[i] / maxDist;
        }
        return distances;
    }
}
