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
import net.calm.iaclasslibrary.Process.Parameter;
import net.calm.iaclasslibrary.Process.ProcessParams;
import net.calm.iaclasslibrary.Process.SourceProcess;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.process.StackConverter;
import java.util.concurrent.Executors;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedGaussianFilter extends SourceProcess {

    /**
     * @deprecated legacy positional index; superseded by {@link #SERIES}.
     */
    @Deprecated
    public static final int SERIES_LABEL = 0;
    /**
     * @deprecated legacy positional index; superseded by {@link #CHANNEL}.
     */
    @Deprecated
    public static final int CHANNEL_LABEL = 1;
    /**
     * @deprecated legacy positional index; superseded by {@link #RADIUS}.
     */
    @Deprecated
    public static final int FILT_RAD_LABEL = 2;
    @Deprecated
    public static final int N_PROP_LABELS = 3;

    public static final Parameter SERIES = new Parameter("series", Parameter.Type.INT, 0, "Series");
    public static final Parameter CHANNEL = new Parameter("channel", Parameter.Type.INT, 0, "Channel");
    public static final Parameter RADIUS = new Parameter("radius", Parameter.Type.DOUBLE, 1.0, "Radius");

    @Override
    protected Parameter[] getParamSlots() {
        return new Parameter[]{SERIES, CHANNEL, RADIUS};
    }

    private double[] sigma;

    public MultiThreadedGaussianFilter() {
        super();
    }

    /**
     * @deprecated a source has no inputs; use {@link #MultiThreadedGaussianFilter()}.
     */
    @Deprecated
    public MultiThreadedGaussianFilter(MultiThreadedProcess[] inputs) {
        super();
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
        this.channel = getInt(CHANNEL);
        sigma = getCalibratedDoubleSigma(series, RADIUS, RADIUS, RADIUS);
        loadPixels(channel, channel);
        ImagePlus imp = img.getLoadedImage();
        (new StackConverter(imp)).convertToGray32();
        IJ.log(String.format("Filtering \"%s\" with a sigma of %f pixels in XY and %f in Z.", imp.getTitle(), sigma[0], sigma[2]));
        GaussianBlur3D.blur(imp, sigma[0], sigma[1], sigma[2]);
//        imp.show();
        output = imp;
        labelOutput(imp.getTitle(), "Filtered");
        img.clearImageData();
    }

    @Override
    protected MultiThreadedProcess copy() {
        return new MultiThreadedGaussianFilter();
    }
}
