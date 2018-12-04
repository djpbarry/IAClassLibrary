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
package Process.Mapping;

import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import java.util.LinkedHashMap;
import java.util.Properties;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MapPixels extends MultiThreadedProcess {

    private final LinkedHashMap<Integer, Integer> map;

    public MapPixels(MultiThreadedProcess[] inputs) {
        super(inputs);
        this.map = new LinkedHashMap();
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {

    }

    public void run() {
        ImagePlus imp1 = inputs[0].getOutput();
        ImagePlus imp2 = inputs[1].getOutput();
        ImageStack stack1 = imp1.getImageStack();
        ImageStack stack2 = imp2.getImageStack();
        int width = stack1.getWidth();
        int height = stack1.getHeight();
        int depth = stack1.getSize();
        map.put(0, 0);
        for (int z = 1; z <= depth; z++) {
            ImageProcessor ip1 = stack1.getProcessor(z);
            ImageProcessor ip2 = stack2.getProcessor(z);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int p1 = ip1.getPixel(x, y);
                    if (p1 > 0) {
                        map.put(ip2.getPixel(x, y), ip1.getPixel(x, y));
                    }
                }
            }
        }
        for (int z = 1; z <= depth; z++) {
            ImageProcessor ip2 = stack2.getProcessor(z);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    ip2.putPixel(x, y, map.get(ip2.getPixel(x, y)));
                }
            }
        }
        output = imp2;
    }

}
