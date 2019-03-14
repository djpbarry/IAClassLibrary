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
import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import org.apache.commons.math3.linear.ArrayRealVector;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedROIConstructor extends MultiThreadedProcess {

    private final Objects3DPopulation cells;
    int selectedChannels;
    int series;
    private String outputPath;
    private final String[] PIX_HEADINGS = {"Channel", "Index", "Mean Pixel Value",
        "Pixel Standard Deviation", "Min Pixel Value", "Max Pixel Value", "Integrated Density"};
    private final String LOCAT_HEAD = "Normalised Distance to Centre";

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
        this.series = Integer.parseInt(props.getProperty(propLabels[0]));
        this.selectedChannels = Integer.parseInt(props.getProperty(propLabels[1]));
        this.outputPath = props.getProperty(propLabels[2]);
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
            combinedCell.setName(String.format("%s_%d", constructOutputName(inputs[0].getOutput().getTitle(), "Cells"), nuc.getValue()));
            combinedCell.setValue(nuc.getValue());
            combinedCell.setComment(CellRegion3D.CELL);
            subPops[2].addObject(combinedCell);
        }
        for (Objects3DPopulation pop : subPops) {
            processObjectPop(pop);
            saveAllRois(outputPath, pop);
        }
    }

    private void processObjectPop(Objects3DPopulation cells) {
        String calUnit = img.getXYSpatialRes(series).unit().getSymbol();
        String[] geomHeadings = getGeomHeadings(calUnit);
        cells.setCalibration(img.getXYSpatialRes(series).value().doubleValue(), img.getZSpatialRes(series).value().doubleValue(), calUnit);
        ResultsTable rt = Analyzer.getResultsTable();
        int nChan = img.getSizeC();
        for (int c = 0; c < nChan; c++) {
            if (((int) Math.pow(2, c) & selectedChannels) != 0) {
                img.loadPixelData(series, c, c + 1, null);
                ImagePlus imp = img.getLoadedImage();
                IJ.log(String.format("Measuring %s defined by %s.", imp.getTitle(), cells.getObject(0).getName()));
                ArrayList<double[]> pixMeasures = cells.getMeasuresStats(imp.getImageStack());
                ArrayList<double[]> geomMeasures = cells.getMeasuresGeometrical();
                double[] distMeasures = getLocationMetrics(cells);
                for (int i = 0; i < pixMeasures.size(); i++) {
                    double[] pixM = pixMeasures.get(i);
                    double[] geomM = geomMeasures.get(i);
                    rt.incrementCounter();
                    rt.addLabel(cells.getObject(i).getName());
                    rt.addValue(PIX_HEADINGS[0], c);
                    rt.addValue(PIX_HEADINGS[1], cells.getObject(i).getValue());
                    for (int j = 2; j <= pixM.length; j++) {
                        rt.addValue(PIX_HEADINGS[j], pixM[j - 1]);
                    }
                    for (int j = pixM.length + 1; j < pixM.length + geomM.length; j++) {
                        rt.addValue(geomHeadings[j - pixM.length], geomM[j - pixM.length]);
                    }
                    rt.addValue(LOCAT_HEAD, distMeasures[i]);
                }
            }
        }
//        output = labels;
    }

    public MultiThreadedROIConstructor duplicate() {
        MultiThreadedROIConstructor newProcess = new MultiThreadedROIConstructor(inputs, cells);
        this.updateOutputDests(newProcess);
        return newProcess;
    }

    private void saveAllRois(String path, Objects3DPopulation cells) {
        if (path == null || !new File(path).exists()) {
            return;
        }
        cells.saveObjects(String.format("%s%s%s.zip", path, File.separator, constructOutputName(cells.getObject(0).getName(), cells.getObject(0).getComment())));
    }

    private String[] getGeomHeadings(String calUnit) {
        return new String[]{"Index", "Volume (Voxels)",
            String.format("Volume (%s^3)", calUnit),
            "Surface Area (Voxels)",
            String.format("Surface Area (%s^2)", calUnit)};
    }

    private double[] getLocationMetrics(Objects3DPopulation cells) {
        double xSum = 0.0;
        double ySum = 0.0;
        double zSum = 0.0;
        ArrayList<double[]> centroids = cells.getMeasureCentroid();
        for (double[] c : centroids) {
            xSum += c[1];
            ySum += c[2];
            zSum += c[3];
        }
        ArrayRealVector volumeCentroid = new ArrayRealVector(new double[]{xSum / centroids.size(), ySum / centroids.size(), zSum / centroids.size()});
        double[] distances = new double[centroids.size()];
        double maxDist = -Double.MAX_VALUE;
        for (int i = 0; i < centroids.size(); i++) {
            double[] c = centroids.get(i);
            distances[i] = volumeCentroid.getDistance(new ArrayRealVector(new double[]{c[1], c[2], c[3]}));
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
