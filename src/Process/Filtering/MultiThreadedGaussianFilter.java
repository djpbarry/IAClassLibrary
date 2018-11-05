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
import ij.ImagePlus;
import ij.plugin.GaussianBlur3D;
import ij.process.StackConverter;
import java.util.Properties;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedGaussianFilter extends MultiThreadedProcess {

    private final double[] sigma;
    private final int series, channel;

    public MultiThreadedGaussianFilter(BioFormatsImg img, Properties props) {
        this(img, null, 0, 0, props);
    }

    public MultiThreadedGaussianFilter(BioFormatsImg img, double[] sigma, int series, int channel, Properties props) {
        super(img, props);
        this.sigma = sigma;
        this.series = series;
        this.channel = channel;
    }

    public void setup() {

    }

    public void run() {
        img.setImg(series, channel, channel);
        ImagePlus imp = img.getImg();
        (new StackConverter(imp)).convertToGray32();
        GaussianBlur3D.blur(imp, sigma[0], sigma[1], sigma[2]);
        imp.show();
        img.setTempImg(imp);
    }
}
