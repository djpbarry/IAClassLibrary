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
import java.util.Properties;
import mcib3d.image3d.regionGrowing.Watershed3D;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedWatershed extends MultiThreadedProcess {

    private double thresh;

    public MultiThreadedWatershed(MultiThreadedProcess[] inputs) {
        super(inputs);
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
        this.thresh = Double.parseDouble(props.getProperty(propLabels[0]));
    }

    public void run() {
        ImagePlus maxima = inputs[0].getOutput();
        ImagePlus cells = inputs[1].getOutput();
        Watershed3D water = new Watershed3D(cells.getImageStack(), maxima.getImageStack(), thresh, 0);
        water.setLabelSeeds(true);
        output = water.getWatershedImage3D().getImagePlus();
    }
}
