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

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.process.StackProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.morphology.strel.EllipsoidStrel;
import net.calm.iaclasslibrary.IO.BioFormats.LocationAgnosticBioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;

import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedTopHatFilter extends MultiThreadedProcess {

    public static int SERIES_LABEL = 0;
    public static int CHANNEL_LABEL = 1;
    public static int FILT_RAD_LABEL = 2;
    public static int RESIZE_FACTOR_LABEL = 3;
    public static int ENABLE_FILTER_LABEL = 4;
    public static int N_PROP_LABELS = 5;

    private double[] sigma;
    private int channel;
    private int series;

    public MultiThreadedTopHatFilter(MultiThreadedProcess[] inputs) {
        super(inputs);
    }

    /**
     * @param img
     * @param props
     * @param propLabels SERIES_SELECT_LABEL, CHANNEL_SELECT_LABEL,
     *                   FILT_RAD_XY_LABEL, FILT_RAD_XY_LABEL, FILT_RAD_Z_LABEL.
     */
    public void setup(LocationAgnosticBioFormatsImg img, Properties props, String[] propLabels) {
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

        ImagePlus imp;
        if (inputs != null) {
            imp = inputs[0].getOutput();
        } else {
            img.loadPixelData(series, channel, channel, null);
            imp = img.getLoadedImage();
        }

        if (Boolean.parseBoolean(props.getProperty(propLabels[ENABLE_FILTER_LABEL]))) {

            ImageStack stack = imp.getImageStack();

            int resizeFactor = (int) Math.round(Double.parseDouble(props.getProperty(propLabels[RESIZE_FACTOR_LABEL])));

            ImageStack smallStack = (new StackProcessor(stack.duplicate())).resize(stack.getWidth() / resizeFactor, stack.getHeight() / resizeFactor, true);

            //IJ.log(String.format("Top-Hat Filtering \"%s\" with a sigma of %f pixels in XY and %f in Z.", imp.getTitle(), sigma[0], sigma[2]));
            Strel3D ball = EllipsoidStrel.fromRadiusList(sigma[0] / resizeFactor, sigma[1] / resizeFactor, sigma[2]);

            ImageStack eroded = Morphology.erosion(smallStack, ball);

            ImageStack background = (new StackProcessor(Morphology.dilation(eroded, ball))).resize(stack.getWidth(), stack.getHeight(), true);

            (new ImageCalculator()).run("subtract stack", imp, new ImagePlus("Background", background));

            //(new StackProcessor(Morphology.whiteTopHat(imp.getImageStack(), EllipsoidStrel.fromRadiusList(sigma[0]/resizeFactor, sigma[1]/resizeFactor, sigma[2])))).resize(stack.getWidth(), stack.getHeight(), true);
            //imp.setStack(Morphology.whiteTopHat(imp.getImageStack(), EllipsoidStrel.fromRadiusList(sigma[0]/resizeFactor, sigma[1]/resizeFactor, sigma[2])));
        }
        output = imp.duplicate();
        labelOutput(imp.getTitle(), "TopHatFiltered");
        img.clearImageData();
    }

    public MultiThreadedTopHatFilter duplicate() {
        MultiThreadedTopHatFilter newProcess = new MultiThreadedTopHatFilter(inputs);
        this.updateOutputDests(newProcess);
        return newProcess;
    }
}
