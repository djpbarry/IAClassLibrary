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

import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.process.StackConverter;
import java.util.Properties;
import mcib3d.image3d.regionGrowing.Watershed3D;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedWatershed extends MultiThreadedProcess {

    private double[] sigma;
    private int series, channel;
    private double thresh;

    public MultiThreadedWatershed() {
        super();
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
        this.series = Integer.parseInt(props.getProperty(propLabels[0]));
        this.channel = Integer.parseInt(props.getProperty(propLabels[1]));
        this.thresh = Double.parseDouble(props.getProperty(propLabels[2]));
        this.sigma = getDoubleSigma(series, propLabels[3], propLabels[3], propLabels[4]);
    }

    public void run() {
        ImagePlus maxima = img.getProcessedImage().duplicate();
        img.loadPixelData(series, channel, channel + 1, null);
        ImagePlus cells = img.getLoadedImage();
        (new StackConverter(cells)).convertToGray32();

        GaussianBlur3D.blur(cells, sigma[0], sigma[1], sigma[2]);
        Watershed3D water = new Watershed3D(cells.getImageStack(), maxima.getImageStack(), thresh, 0);
        water.setLabelSeeds(true);
        img.setProcessedImage(water.getWatershedImage3D().getImagePlus());
    }
}
