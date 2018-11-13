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
package Process.ROI;

import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import ij.ImagePlus;
import ij.gui.Roi;
import java.util.ArrayList;
import java.util.Properties;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageInt;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedROIConstructor extends MultiThreadedProcess {

    final ArrayList<ArrayList<Roi>> allRois;
    Objects3DPopulation objectPop;

    public MultiThreadedROIConstructor(BioFormatsImg img, Properties props) {
        super(img, props, null);
        this.allRois = new ArrayList();
    }

    public void setup() {

    }

    @Override
    public void run() {
        ImagePlus labels = img.getProcessedImage();
        objectPop = new Objects3DPopulation(ImageInt.wrap(labels), 1);
        Object3D[] objects = objectPop.getObjectsArray();
        int[] dims = new int[]{labels.getWidth(), labels.getHeight(), labels.getNSlices()};
        for (int i = 0; i < objects.length; i++) {
            Object3D object = objects[i];
            allRois.add(new ArrayList());
            exec.submit(new RunnableRoiConstructor(allRois, object, dims, i));
        }
        terminate("Error encountered building ROIs.");
    }

    public ArrayList<ArrayList<Roi>> getAllRois() {
        return allRois;
    }

    public Objects3DPopulation getObjectPop() {
        return objectPop;
    }
}
