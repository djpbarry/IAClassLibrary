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
import Process.Mapping.MapPixels;
import Process.MultiThreadedProcess;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.process.AutoThresholder;
import ij.process.StackStatistics;
import java.util.Properties;
import mcib3d.image3d.regionGrowing.Watershed3D;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedWatershed extends MultiThreadedProcess {

    private double thresh;
    private final String objectName;
    private final boolean remap;

    public MultiThreadedWatershed(MultiThreadedProcess[] inputs, String objectName, boolean remap) {
        super(inputs);
        this.objectName = objectName;
        this.remap = remap;
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.props = props;
        this.propLabels = propLabels;
        this.thresh = getThreshold();
    }

    public void run() {
        ImagePlus seeds = inputs[0].getOutput();
        ImagePlus image = inputs[1].getOutput();
        IJ.log(String.format("Watershedding with threshold of %f", thresh));
        Watershed3D water = new Watershed3D(image.getImageStack(), seeds.getImageStack(), thresh, 0);
        water.setLabelSeeds(true);
        output = water.getWatershedImage3D().getImagePlus();
        output.setTitle(objectName);
        try {
            if (remap) {
                MapPixels mp = new MapPixels(new MultiThreadedProcess[]{inputs[0], this});
                mp.start();
                mp.join();
                output = mp.getOutput();
            }
        } catch (InterruptedException e) {
            GenUtils.logError(e, "Unable to remap pixels.");
        }
    }

    private double getThreshold() {
        StackStatistics stats = new StackStatistics(inputs[1].getOutput());
        int tIndex = (new AutoThresholder()).getThreshold(AutoThresholder.Method.valueOf(props.getProperty(propLabels[0])), stats.histogram);
        return stats.histMin + stats.binSize * tIndex;
    }
}
