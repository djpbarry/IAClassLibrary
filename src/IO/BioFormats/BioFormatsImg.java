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
package IO.BioFormats;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BioFormatsImg {

    private final RandomAccessibleInterval< FloatType> interval;
    private final Img< FloatType> img;
    private final double xySpatRes;
    private final double zSpatRes;

    public BioFormatsImg(Img< FloatType> img, RandomAccessibleInterval< FloatType> interval, double xySpatRes, double zSpatRes) {
        this.img = img;
        this.interval = interval;
        this.xySpatRes = xySpatRes;
        this.zSpatRes = zSpatRes;
    }

    public Img< FloatType> getImg() {
        return img;
    }

    public double getXySpatRes() {
        return xySpatRes;
    }

    public double getzSpatRes() {
        return zSpatRes;
    }

    public RandomAccessibleInterval< FloatType> getInterval() {
        return interval;
    }

}
