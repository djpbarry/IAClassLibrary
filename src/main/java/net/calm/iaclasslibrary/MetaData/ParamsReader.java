/*
 * Copyright (C) 2017 Dave Barry <david.barry at crick.ac.uk>
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
package net.calm.iaclasslibrary.MetaData;

import ij.ImagePlus;

public class ParamsReader {

    private final double xySpatialRes;
    private final double zSpatialRes;
    private final double framesPerSecond;

    public ParamsReader(ImagePlus imp) {
        this.xySpatialRes = imp.getCalibration().pixelWidth;
        this.zSpatialRes = imp.getCalibration().pixelDepth;
        this.framesPerSecond = 1.0 / imp.getCalibration().frameInterval;
    }

    public double getXYSpatialRes() {
        return xySpatialRes;
    }

    public double getFrameRate() {
        return framesPerSecond;
    }

    public double getzSpatialRes() {
        return zSpatialRes;
    }

}
