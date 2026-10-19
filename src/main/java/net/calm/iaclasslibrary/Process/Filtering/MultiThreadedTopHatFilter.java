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
import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import net.calm.iaclasslibrary.Process.Parameter;
import net.calm.iaclasslibrary.Process.ProcessParams;
import net.calm.iaclasslibrary.Process.TransformProcess;

import java.util.concurrent.Executors;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedTopHatFilter extends TransformProcess {

    @Deprecated
    public static final int SERIES_LABEL = 0;
    @Deprecated
    public static final int CHANNEL_LABEL = 1;
    @Deprecated
    public static final int FILT_RAD_LABEL = 2;
    @Deprecated
    public static final int RESIZE_FACTOR_LABEL = 3;
    @Deprecated
    public static final int ENABLE_FILTER_LABEL = 4;
    @Deprecated
    public static final int N_PROP_LABELS = 5;

    public static final Parameter SERIES = new Parameter("series", Parameter.Type.INT, 0, "Series");
    public static final Parameter CHANNEL = new Parameter("channel", Parameter.Type.INT, 0, "Channel");
    public static final Parameter RADIUS = new Parameter("radius", Parameter.Type.DOUBLE, 1.0, "Radius");
    public static final Parameter RESIZE_FACTOR = new Parameter("resize_factor", Parameter.Type.DOUBLE, 1.0, "Resize factor");
    public static final Parameter ENABLE_FILTER = new Parameter("enable_filter", Parameter.Type.BOOLEAN, false, "Enable filter");

    @Override
    protected Parameter[] getParamSlots() {
        return new Parameter[]{SERIES, CHANNEL, RADIUS, RESIZE_FACTOR, ENABLE_FILTER};
    }

    private double[] sigma;

    public MultiThreadedTopHatFilter(MultiThreadedProcess[] inputs) {
        super(inputs);
    }

    @Override
    public void setup(BioFormatsImg img, ProcessParams params) {
        initParams(img, params);
//        ImagePlus image = img.getLoadedImage();
//        (new StackConverter(img.getProcessedImage())).convertToGray32();
    }

    public void run() {
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.series = getInt(SERIES);
        sigma = getCalibratedDoubleSigma(series, RADIUS, RADIUS, RADIUS);

        ImagePlus imp = inputs[0].getOutput();

        if (getBoolean(ENABLE_FILTER)) {

            ImageStack stack = imp.getImageStack();

            int resizeFactor = (int) Math.round(getDouble(RESIZE_FACTOR));

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

    @Override
    protected MultiThreadedProcess copy() {
        return new MultiThreadedTopHatFilter(inputs);
    }
}
