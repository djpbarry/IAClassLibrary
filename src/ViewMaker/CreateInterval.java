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
package ViewMaker;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class CreateInterval {

    public static RandomAccessibleInterval< FloatType> createInterval(int nD, Img img, int channel) {
        RandomAccessibleInterval< FloatType> view = Views.hyperSlice(img, 2, channel);
//        if (nD > 3) {
//            view = Intervals.createMinMax(0, 0, channel, 0, img.max(0), img.max(1), channel, img.max(3));
//        } else if (nD > 2) {
//            view = Intervals.createMinMax(0, 0, channel, img.max(0), img.max(1), channel);
//        } else {
//            view = Intervals.createMinMax(0, 0, img.max(0), img.max(1));
//        }
        return view;
    }
}
