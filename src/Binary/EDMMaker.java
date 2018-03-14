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
package Binary;

import ij.ImageStack;
import ij.plugin.filter.EDM;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class EDMMaker {

    public static ImageStack makeEDM(ImageStack binaryInput) {
        int s = binaryInput.size();
        ImageStack edmStack = new ImageStack(binaryInput.getWidth(), binaryInput.getHeight());
        for (int i = 1; i <= s; i++) {
            ImageProcessor slice = binaryInput.getProcessor(i);
            ShortProcessor edm = (new EDM()).make16bitEDM(slice);
            edm.multiply(1.0 / EDM.ONE);
            edmStack.addSlice(edm);
        }
        return edmStack;
    }
}
