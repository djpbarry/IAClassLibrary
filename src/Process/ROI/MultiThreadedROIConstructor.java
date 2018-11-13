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

    public MultiThreadedROIConstructor() {
        super();
        this.allRois = new ArrayList();
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public void run() {
        ImagePlus labels = img.getProcessedImage();
        objectPop = new Objects3DPopulation(ImageInt.wrap(labels), 1);
        ArrayList<Object3D> objects = objectPop.getObjectsList();
        int[] dims = new int[]{labels.getWidth(), labels.getHeight(), labels.getNSlices()};
        for (int i = 0; i < objects.size(); i++) {
            Object3D object = objects.get(i);
            allRois.add(new ArrayList());
            exec.submit(new RunnableRoiConstructor(allRois, object, dims, i));
        }
        terminate("Error encountered building ROIs.");
        ArrayList<double[]> measures = objectPop.getMeasuresStats(img.getLoadedImage().getImageStack());
        ResultsTable rt = Analyzer.getResultsTable();
        String[] headings = {"Index", "Mean Pixel Value", "Pixel Standard Deviation", "Min Pixel Value", "Max Pixel Value", "Integrated Density"};
        for (double[] m : measures) {
            rt.incrementCounter();
            for (int i = 0; i < m.length; i++) {
                rt.addValue(headings[i], m[i]);
            }
        }
        rt.updateResults();
        rt.show("Measures");
    }

    public ArrayList<ArrayList<Roi>> getAllRois() {
        return allRois;
    }

    public Objects3DPopulation getObjectPop() {
        return objectPop;
    }
}
