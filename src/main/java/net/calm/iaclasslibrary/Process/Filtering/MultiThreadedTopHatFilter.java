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
package net.calm.iaclasslibrary.Process.Filtering;

import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import ij.IJ;

import ij.ImagePlus;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.strel.EllipsoidStrel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import net.imagej.ImageJ;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedTopHatFilter extends MultiThreadedProcess {

    public static int SERIES_LABEL = 0;
    public static int CHANNEL_LABEL = 1;
    public static int FILT_RAD_LABEL = 2;
    public static int N_PROP_LABELS = 3;

    private double[] sigma;
    private int channel;
    private int series;

    public MultiThreadedTopHatFilter(MultiThreadedProcess[] inputs) {
        super(inputs);
    }

    /**
     *
     * @param img
     * @param props
     * @param propLabels SERIES_SELECT_LABEL, CHANNEL_SELECT_LABEL,
     * FILT_RAD_XY_LABEL, FILT_RAD_XY_LABEL, FILT_RAD_Z_LABEL.
     */
    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
//        ImagePlus image = img.getLoadedImage();
//        (new StackConverter(img.getProcessedImage())).convertToGray32();
    }

    public void run() {
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.series = Integer.parseInt(props.getProperty(propLabels[SERIES_LABEL]));
        this.channel = Integer.parseInt(props.getProperty(propLabels[CHANNEL_LABEL]));
        sigma = getCalibratedDoubleSigma(series, propLabels[FILT_RAD_LABEL], propLabels[FILT_RAD_LABEL], propLabels[FILT_RAD_LABEL]);
        img.loadPixelData(series, channel, channel + 1, null);
        ImagePlus imp = img.getLoadedImage();
        IJ.log(String.format("Top-Hat Filtering \"%s\" with a sigma of %f pixels in XY and %f in Z.", imp.getTitle(), sigma[0], sigma[2]));
//        List<Shape> shapes = new ArrayList();
//        shapes.add(new HyperSphereShape((long)Math.round(sigma[0])));
//        Img<UnsignedShortType> img = ImagePlusAdapter.wrap(imp);
//        (new ImageJ()).op().morphology().topHat(img, shapes);
        
        imp.setStack(Morphology.whiteTopHat(imp.getImageStack(), EllipsoidStrel.fromRadiusList(sigma[0], sigma[1], sigma[2])));
//        imp.show();
        output = imp;
        labelOutput(imp.getTitle(), "TopHatFiltered");
    }

    public MultiThreadedTopHatFilter duplicate() {
        MultiThreadedTopHatFilter newProcess = new MultiThreadedTopHatFilter(inputs);
        this.updateOutputDests(newProcess);
        return newProcess;
    }
}
