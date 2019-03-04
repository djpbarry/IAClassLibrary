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

import IO.BioFormats.BioFormatsImg;
import Process.Calculate.MultiThreadedImageCalculator;
import Process.MultiThreadedProcess;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageInt;
import org.apache.commons.math3.linear.ArrayRealVector;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedROIConstructor extends MultiThreadedProcess {

    ArrayList<ArrayList<Roi>> allRois;
    Objects3DPopulation objectPop;
    int selectedChannels;
    int series;
    private String outputPath;
    private final String[] PIX_HEADINGS = {"Channel", "Index", "Mean Pixel Value",
        "Pixel Standard Deviation", "Min Pixel Value", "Max Pixel Value", "Integrated Density"};
    private final String LOCAT_HEAD = "Normalised Distance to Centre";

    public MultiThreadedROIConstructor(MultiThreadedProcess[] inputs) {
        super(inputs);
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
        inputs = new MultiThreadedProcess[]{inputs[0], inputs[1],
            new MultiThreadedImageCalculator(inputs, "Cytoplasm", "Difference")};
        for (MultiThreadedProcess p : inputs) {
            allRois = new ArrayList();
            processLabeledImage(p.getOutput());
            saveAllRois(outputPath, p.getOutput().getTitle());
        }
    }

    private void processLabeledImage(ImagePlus labels) {
        objectPop = new Objects3DPopulation(ImageInt.wrap(labels), 0);
        String calUnit = img.getXYSpatialRes(series).unit().getSymbol();
        String[] geomHeadings = getGeomHeadings(calUnit);
        objectPop.setCalibration(img.getXYSpatialRes(series).value().doubleValue(), img.getZSpatialRes(series).value().doubleValue(), calUnit);
        ResultsTable rt = Analyzer.getResultsTable();
        int nChan = img.getSizeC();
        for (int c = 0; c < nChan; c++) {
            if (((int) Math.pow(2, c) & selectedChannels) != 0) {
                img.loadPixelData(series, c, c + 1, null);
                ImagePlus imp = img.getLoadedImage();
                IJ.log(String.format("Measuring %s defined by %s.", imp.getTitle(), labels.getTitle()));
                ArrayList<double[]> pixMeasures = objectPop.getMeasuresStats(imp.getImageStack());
                ArrayList<double[]> geomMeasures = objectPop.getMeasuresGeometrical();
                double[] distMeasures = getLocationMetrics();
                for (int i = 0; i < pixMeasures.size(); i++) {
                    double[] pixM = pixMeasures.get(i);
                    double[] geomM = geomMeasures.get(i);
                    rt.incrementCounter();
                    rt.addLabel(labels.getTitle());
                    rt.addValue(PIX_HEADINGS[0], c);
                    rt.addValue(PIX_HEADINGS[1], i + 1);
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
        output = labels;
    }

    public ArrayList<ArrayList<Roi>> getAllRois() {
        return allRois;
    }

    public Objects3DPopulation getObjectPop() {
        return objectPop;
    }

    public MultiThreadedROIConstructor duplicate() {
        MultiThreadedROIConstructor newProcess = new MultiThreadedROIConstructor(inputs);
        this.updateOutputDests(newProcess);
        return newProcess;
    }

    private void saveAllRois(String path, String name) {
        if (path == null || !new File(path).exists()) {
            return;
        }
        objectPop.saveObjects(String.format("%s%s%s.zip", path, File.separator, name));
    }

    private String[] getGeomHeadings(String calUnit) {
        return new String[]{"Index", "Volume (Voxels)",
            String.format("Volume (%s^3)", calUnit),
            "Surface Area (Voxels)",
            String.format("Surface Area (%s^2)", calUnit)};
    }

    private double[] getLocationMetrics() {
        double xSum = 0.0;
        double ySum = 0.0;
        double zSum = 0.0;
        ArrayList<double[]> centroids = objectPop.getMeasureCentroid();
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
