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
import Process.MultiThreadedProcess;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.process.StackConverter;
import java.util.Properties;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedGaussianFilter extends MultiThreadedProcess {

    private double[] sigma;

    /**
     * 
     * @param img
     * @param props
     * @param propLabels SERIES_SELECT_LABEL, CHANNEL_SELECT_LABEL, FILT_RAD_XY_LABEL,
     * FILT_RAD_XY_LABEL, FILT_RAD_Z_LABEL.
     */
    public MultiThreadedGaussianFilter(BioFormatsImg img, Properties props, String[] propLabels) {
        super(img, props, propLabels);
    }

    protected void setup() {
        int series = Integer.parseInt(props.getProperty(propLabels[0]));
        sigma = getDoubleSigma(series, propLabels[2], propLabels[2], propLabels[3]);
//        img.setImg(series, channel, channel, null);
        ImagePlus image = img.getImg();
        (new StackConverter(image)).convertToGray32();
    }

    public void run() {
        setup();
        ImagePlus imp = img.getImg();
        (new StackConverter(imp)).convertToGray32();
        GaussianBlur3D.blur(imp, sigma[0], sigma[1], sigma[2]);
//        imp.show();
        img.setTempImg(imp);
        IJ.log("Gaussian filtering done.");
    }
}
