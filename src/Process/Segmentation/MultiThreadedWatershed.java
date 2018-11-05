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
import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.process.StackConverter;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import mcib3d.image3d.regionGrowing.Watershed3D;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedWatershed extends MultiThreadedProcess {

    private final double[] sigma;
    private final int series, channel;
    private final double thresh;

    public MultiThreadedWatershed(BioFormatsImg img, Properties props) {
        this(img, null, null, 0, 0, 0f, props);
    }

    public MultiThreadedWatershed(BioFormatsImg img, ExecutorService exec, double[] sigma, int series, int channel, double thresh, Properties props) {
        super(img, props);
        this.sigma = sigma;
        this.series = series;
        this.channel = channel;
        this.thresh = thresh;
    }

    public void setup() {

    }

    public void run() {
        ImagePlus maxima = img.getTempImg().duplicate();
        img.setImg(series, channel, channel);
        ImagePlus cells = img.getImg();
        (new StackConverter(cells)).convertToGray32();

        GaussianBlur3D.blur(cells, sigma[0], sigma[1], sigma[2]);
        Watershed3D water = new Watershed3D(cells.getImageStack(), maxima.getImageStack(), thresh, 0);
        water.setLabelSeeds(true);
        water.getWatershedImage3D().show();

        img.setTempImg(water.getWatershedImage3D().getImagePlus());
    }
}
