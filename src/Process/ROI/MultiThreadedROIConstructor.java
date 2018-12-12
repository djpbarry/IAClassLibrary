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
import Process.MultiThreadedProcess;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Executors;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageInt;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedROIConstructor extends MultiThreadedProcess {

    final ArrayList<ArrayList<Roi>> allRois;
    Objects3DPopulation objectPop;
    int selectedChannels;
    int series;

    public MultiThreadedROIConstructor(MultiThreadedProcess[] inputs) {
        super(inputs);
        this.allRois = new ArrayList();
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
        this.series = Integer.parseInt(props.getProperty(propLabels[0]));
        this.selectedChannels = Integer.parseInt(props.getProperty(propLabels[1]));
    }

    @Override
    public void run() {
        for (MultiThreadedProcess p : inputs) {
            processLabeledImage(p.getOutput());
        }
    }

    private void processLabeledImage(ImagePlus labels) {
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        objectPop = new Objects3DPopulation(ImageInt.wrap(labels), 1);
        ArrayList<Object3D> objects = objectPop.getObjectsList();
        int[] dims = new int[]{labels.getWidth(), labels.getHeight(), labels.getNSlices()};
        for (int i = 0; i < objects.size(); i++) {
            Object3D object = objects.get(i);
            allRois.add(new ArrayList());
            exec.submit(new RunnableRoiConstructor(allRois, object, dims, i));
        }
        terminate("Error encountered building ROIs.");
        ResultsTable rt = Analyzer.getResultsTable();
        String[] headings = {"Channel", "Index", "Mean Pixel Value", "Pixel Standard Deviation", "Min Pixel Value", "Max Pixel Value", "Integrated Density"};
        int nChan = img.getChannelCount();
        for (int c = 0; c < nChan; c++) {
            if (((int) Math.pow(2, c) & selectedChannels) != 0) {
                img.loadPixelData(series, c, c + 1, null);
                ImagePlus imp = img.getLoadedImage();
                IJ.log(String.format("Measuring %s defined by %s.", imp.getTitle(), labels.getTitle()));
                ArrayList<double[]> measures = objectPop.getMeasuresStats(imp.getImageStack());
                for (double[] m : measures) {
                    rt.incrementCounter();
                    rt.addLabel(labels.getTitle());
                    rt.addValue(headings[0], c);
                    for (int i = 1; i <= m.length; i++) {
                        rt.addValue(headings[i], m[i - 1]);
                    }
                }
            }
        }
        rt.updateResults();
        rt.show("Measures");
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
}
