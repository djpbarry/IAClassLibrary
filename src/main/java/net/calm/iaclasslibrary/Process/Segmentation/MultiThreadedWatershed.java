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
package net.calm.iaclasslibrary.Process.Segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import ij.process.StackProcessor;
import inra.ijpb.watershed.MarkerControlledWatershedTransform3D;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageShort;
import mcib3d.image3d.distanceMap3d.EDT;
import net.calm.iaclasslibrary.Binary.BinaryMaker;
import net.calm.iaclasslibrary.Cell3D.*;
import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import net.calm.iaclasslibrary.Process.Calculate.MultiThreadedImageCalculator;
import net.calm.iaclasslibrary.Process.DistanceTransform.RiemannianDistanceTransform;
import net.calm.iaclasslibrary.Process.Mapping.MapPixels;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import net.calm.iaclasslibrary.Process.TransformProcess;
import net.calm.iaclasslibrary.Process.Parameter;
import net.calm.iaclasslibrary.Process.ProcessParams;
import net.calm.iaclasslibrary.Stacks.StackThresholder;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;

import java.util.List;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedWatershed extends TransformProcess {

    @Deprecated
    public static final int SERIES_LABEL = 0;
    @Deprecated
    public static final int THRESHOLD_LABEL = 1;
    @Deprecated
    public static final int VOL_MARKER_LABEL = 2;
    @Deprecated
    public static final int MEMB_MARKER_LABEL = 3;
    @Deprecated
    public static final int LAMBDA_LABEL = 4;
    @Deprecated
    public static final int N_PROP_LABELS = 5;

    public static final Parameter SERIES = new Parameter("series", Parameter.Type.INT, 0, "Series");
    public static final Parameter THRESHOLD = new Parameter("threshold", Parameter.Type.STRING, "Default", "Threshold method");
    public static final Parameter VOL_MARKER = new Parameter("volume_marker", Parameter.Type.BOOLEAN, false, "Use volume marker");
    public static final Parameter MEMB_MARKER = new Parameter("membrane_marker", Parameter.Type.BOOLEAN, false, "Use membrane marker");
    public static final Parameter LAMBDA = new Parameter("lambda", Parameter.Type.FLOAT, 0.0f, "Lambda");

    @Override
    protected Parameter[] getParamSlots() {
        return new Parameter[]{SERIES, THRESHOLD, VOL_MARKER, MEMB_MARKER, LAMBDA};
    }

    private double[] calibration;
    private final String objectName;
    private final boolean remap;
    private boolean volumeMarker;
    private final boolean combine;
    private final boolean binaryImage;
    private final Objects3DPopulation cells;
    private final int segmentationType;
    private float lambda;
    public final static int CELLS = 10, NUCLEI = 11, SPOTS = 12;

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

    @Override
    public void setup(BioFormatsImg img, ProcessParams params) {
        initParams(img, params);
        this.volumeMarker = getBoolean(VOL_MARKER);
        this.series = getInt(SERIES);
        this.lambda = getFloat(LAMBDA);
        calibration = getCalibration(series);
    }

    public void run() {
        ImagePlus seeds = inputs[0].getOutput();
        ImagePlus image = inputs[1].getOutput();
        int thresh = getThreshold();
        IJ.log(String.format("Watershedding \"%s\" with a threshold of %d, using \"%s\" as seeds...", image.getTitle(), thresh, seeds.getTitle()));
        ImagePlus mask = image.duplicate();
        StackThresholder.thresholdStack(mask, thresh);
        (new StackProcessor(mask.getImageStack())).invert();
        if (volumeMarker) {
            ImageFloat edt = EDT.run(ImageHandler.wrap(seeds), 1, (float) calibration[0], (float) calibration[2], true, Runtime.getRuntime().availableProcessors());
            output = watershed(edt.getImagePlus(), seeds, mask);
        } else {
            ImageFloat rdt = (new RiemannianDistanceTransform()).run(new ImageFloat(image), new ImageShort(seeds), 0, (float) calibration[0], (float) calibration[2], lambda);
            output = watershed(rdt.getImagePlus(), seeds, mask);
        }
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
            case CELLS -> {
                labelOutput(image.getTitle(), CellRegion3D.CYTO);
                addCytoplasmToCells(output.getTitle());
            }
            case NUCLEI -> {
                labelOutput(image.getTitle(), CellRegion3D.NUCLEUS);
                initialiseCellsWithNuclei(output.getTitle());
            }
            default -> { }
        }
    }

    void initialiseCellsWithNuclei(String label) {
        while (cells.getNbObjects() > 0) {
            cells.removeObject(0);
        }
        Objects3DPopulation nucleiPop = new Objects3DPopulation(ImageInt.wrap(output), 0);
        List<Object3D> nuclei = nucleiPop.getObjectsList();
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
        List<Object3D> cellObjects = cellPop.getObjectsList();
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

    void addSpotsToCells(String label) {
//        Objects3DPopulation spotPop = new Objects3DPopulation(ImageInt.wrap(output), 0);
//        List<Object3D> spotObjects = spotPop.getObjectsList();
//        for (int i = 0; i < spotObjects.size(); i++) {
//            Cell3D cell = (Cell3D) cells.getObject(i);
//            Spot3D spot = new Spot3D(spotObjects.get(i));
//            spot.setName(String.format("%s_%d", label, spot.getValue()));
//            if (spot.getVoxels().size() > 0) {
//                cell.addSpot();
//            }
//        }
    }

    @Deprecated
    private int getThreshold() {
        return BinaryMaker.getThreshold(inputs[1].getOutput(),
                AutoThresholder.Method.valueOf(getString(THRESHOLD)));
    }

    @Override
    protected MultiThreadedProcess copy() {
        return new MultiThreadedWatershed(inputs, objectName, remap, combine, binaryImage, cells, segmentationType);
    }

    public static ImagePlus watershed(ImagePlus inputImage, ImagePlus seeds, ImagePlus mask) {
        MarkerControlledWatershedTransform3D watershed = new MarkerControlledWatershedTransform3D(inputImage, seeds, mask);
        return watershed.applyWithPriorityQueueAndDams();
    }
}
