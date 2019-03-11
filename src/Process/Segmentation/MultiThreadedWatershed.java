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
package Process.Segmentation;

import Cell3D.Cell3D;
import Cell3D.Cytoplasm3D;
import Cell3D.Nucleus3D;
import IO.BioFormats.BioFormatsImg;
import Process.Calculate.MultiThreadedImageCalculator;
import Process.Mapping.MapPixels;
import Process.MultiThreadedProcess;
import Stacks.StackThresholder;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import ij.process.StackProcessor;
import ij.process.StackStatistics;
import java.util.ArrayList;
import java.util.Properties;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.regionGrowing.Watershed3D;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedWatershed extends MultiThreadedProcess {

    private final String objectName;
    private final boolean remap;
    private boolean volumeMarker;
    private final boolean combine;
    private final boolean binaryImage;
    private final Objects3DPopulation cells;
    private final int segmentationType;
    public final static int CELLS = 0, NUCLEI = 1, SPOTS = 2;
    private final static String CELL_LABEL = "Cell", NUC_LABEL = "Nucleus", CYTO_LABEL = "Cytoplasm", SPOT_LABEL = "Spot";

    public MultiThreadedWatershed(MultiThreadedProcess[] inputs, String objectName, boolean remap, boolean combine, boolean binaryImage, Objects3DPopulation cells) {
        this(inputs, objectName, remap, combine, binaryImage, cells, -1);
    }

    public MultiThreadedWatershed(MultiThreadedProcess[] inputs, String objectName, boolean remap, boolean combine, boolean binaryImage, Objects3DPopulation cells, int segmentationType) {
        super(inputs);
        this.objectName = objectName;
        this.remap = remap;
        this.combine = combine;
        this.binaryImage = binaryImage;
        this.cells = cells;
        this.segmentationType = segmentationType;
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
        this.volumeMarker = Boolean.parseBoolean(props.getProperty(propLabels[1]));
    }

    public void run() {
        ImagePlus seeds = inputs[0].getOutput();
        ImagePlus image = inputs[1].getOutput();
        if (!volumeMarker) {
            (new StackProcessor(image.getImageStack())).invert();
        }
        int thresh = getThreshold();
        IJ.log(String.format("Watershedding \"%s\" with a threshold of %d, using \"%s\" as seeds...", image.getTitle(), thresh, seeds.getTitle()));
//        if (binaryImage) {
        StackThresholder.thresholdStack(image, thresh);
        thresh = 0;
//        }
        Watershed3D water = new Watershed3D(image.getImageStack(), seeds.getImageStack(), thresh, 0);
        water.setLabelSeeds(true);
        output = water.getWatershedImage3D().getImagePlus();
        try {
            if (remap) {
                MapPixels mp = new MapPixels(new MultiThreadedProcess[]{inputs[0], this});
                mp.start();
                mp.join();
                output = mp.getOutput();
            }
        } catch (InterruptedException e) {
            GenUtils.logError(e, "Unable to remap pixels.");
        }
        try {
            if (combine) {
                MultiThreadedImageCalculator ic = new MultiThreadedImageCalculator(new MultiThreadedProcess[]{inputs[0], this}, objectName, "Max");
                ic.start();
                ic.join();
                output = ic.getOutput();
            }
        } catch (InterruptedException e) {
            GenUtils.logError(e, "Unable to remap pixels.");
        }
        switch (segmentationType) {
            case CELLS:
                labelOutput(image.getTitle(), "Cytoplasm");
                addCytoplasmToCells(output.getTitle());
                break;
            case NUCLEI:
                labelOutput(image.getTitle(), "Nuclei");
                initialiseCellsWithNuclei(output.getTitle());
                break;
            default:
                addSpotsToCells();
        }
    }

    void initialiseCellsWithNuclei(String label) {
        while (cells.getNbObjects() > 0) {
            cells.removeObject(0);
        }
        Objects3DPopulation nucleiPop = new Objects3DPopulation(ImageInt.wrap(output), 0);
        ArrayList<Object3D> nuclei = nucleiPop.getObjectsList();
        for (Object3D nucleus : nuclei) {
            Cell3D cell = new Cell3D();
            Nucleus3D nuc3D = new Nucleus3D(nucleus);
            nuc3D.setName(String.format("%s_%d", label, nucleus.getValue()));
            cell.setNucleus(nuc3D);
            cell.setName(String.format("%s_%d", label, nucleus.getValue()));
            cell.setID(nucleus.getValue());
            cells.addObject(cell);
        }
    }

    void addCytoplasmToCells(String label) {
        Objects3DPopulation cellPop = new Objects3DPopulation(ImageInt.wrap(output), 0);
        ArrayList<Object3D> cellObjects = cellPop.getObjectsList();
        for (int i = 0; i < cellObjects.size(); i++) {
            Cell3D cell = (Cell3D) cells.getObject(i);
            Cytoplasm3D cyto = new Cytoplasm3D(cellObjects.get(i));
            cyto.substractObject(cell.getNucleus());
            cyto.setName(String.format("%s_%d", label, cyto.getValue()));
            if (cyto.getVoxels().size() > 0) {
                cell.setCytoplasm(cyto);
            } else {
                cell.setCytoplasm(null);
            }
        }
    }

    void addSpotsToCells() {

    }

    private int getThreshold() {
        StackStatistics stats = new StackStatistics(inputs[1].getOutput());
        int tIndex = (new AutoThresholder()).getThreshold(AutoThresholder.Method.valueOf(props.getProperty(propLabels[0])), stats.histogram);
        return (int) Math.round(stats.histMin + stats.binSize * tIndex);
    }

    public MultiThreadedWatershed duplicate() {
        MultiThreadedWatershed newProcess = new MultiThreadedWatershed(inputs, objectName, remap, combine, binaryImage, cells, segmentationType);
        this.updateOutputDests(newProcess);
        return newProcess;
    }
}
