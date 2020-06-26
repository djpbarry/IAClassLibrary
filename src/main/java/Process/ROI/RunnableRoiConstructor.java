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

import IAClasses.Region;
import Process.RunnableProcess;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import mcib3d.geom.Object3D;
import mcib3d.geom.ObjectCreator3D;
import mcib3d.image3d.ImageByte;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class RunnableRoiConstructor extends RunnableProcess {

    int[] dims;
    Object3D object;
    ArrayList<ArrayList<Roi>> allRois;
    int index;

    public RunnableRoiConstructor(ArrayList<ArrayList<Roi>> allRois, Object3D object, int[] dims, int index) {
        super(null);
        this.allRois = allRois;
        this.object = object;
        this.dims = dims;
        this.index = index;
    }

    @Override
    public void run() {
        ImageByte labelImage = new ImageByte("Mask", dims[0], dims[1], dims[2]);
        labelImage.fill(Region.MASK_FOREGROUND);
        ObjectCreator3D creator3D = new ObjectCreator3D(labelImage);
        creator3D.drawObject(object);
        ArrayList<Roi> objRois = new ArrayList();
        for (int z = 0; z < dims[2]; z++) {
            ByteProcessor maskSlice = new ByteProcessor(dims[0], dims[1], (byte[]) labelImage.getArray1D(z));
            maskSlice.setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE);
            Roi r = ThresholdToSelection.run(new ImagePlus("mask " + z, maskSlice));
            if (r != null) {
                r.setPosition(z + 1);
                objRois.add(r);
            }
        }
        allRois.set(index, objRois);
    }
}
