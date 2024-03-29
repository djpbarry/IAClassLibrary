/*
 * Copyright (C) 2019 David Barry <david.barry at crick dot ac dot uk>
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
package net.calm.iaclasslibrary.Cell3D;

import fiji.plugin.trackmate.Spot;
import mcib3d.geom.Object3D;
import mcib3d.geom.Voxel3D;

import java.util.LinkedList;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class Spot3D extends CellRegion3D {

    private final Spot spot;
//    private final LinkedList<Voxel3D> voxels;

    public Spot3D(Object3D object) {
        super(object);
        this.spot = null;
    }

    public Spot3D(Spot spot, LinkedList<Voxel3D> voxels) {
        super(voxels);
        this.spot = spot;
    }

    public Spot3D(Object3D object, Spot spot) {
        super(object);
        this.spot = spot;
    }

    public Spot getSpot() {
        return spot;
    }



}
