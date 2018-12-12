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
package Process.Filtering;

import IO.BioFormats.BioFormatsImg;
import Process.Mapping.MapPixels;
import Process.MultiThreadedProcess;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.process.StackConverter;
import java.util.Properties;
import java.util.concurrent.Executors;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedGaussianFilter extends MultiThreadedProcess {

    private double[] sigma;
    private int channel;

    public MultiThreadedGaussianFilter(MultiThreadedProcess[] inputs) {
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
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int series = Integer.parseInt(props.getProperty(propLabels[0]));
        this.channel = Integer.parseInt(props.getProperty(propLabels[1]));
        sigma = getCalibratedDoubleSigma(series, propLabels[2], propLabels[2], propLabels[3]);
        img.loadPixelData(series, channel, channel + 1, null);
//        ImagePlus image = img.getLoadedImage();
//        (new StackConverter(img.getProcessedImage())).convertToGray32();
    }

    public void run() {
        ImagePlus imp = img.getLoadedImage();
        (new StackConverter(imp)).convertToGray32();
        IJ.log(String.format("Filtering channel %d with a sigma of %f pixels in XY and %f in Z.", channel, sigma[0], sigma[2]));
        GaussianBlur3D.blur(imp, sigma[0], sigma[1], sigma[2]);
//        imp.show();
        output = imp;
    }

    public MultiThreadedGaussianFilter duplicate() {
        MultiThreadedGaussianFilter newProcess = new MultiThreadedGaussianFilter(inputs);
        this.updateOutputDests(newProcess);
        return newProcess;
    }
}
