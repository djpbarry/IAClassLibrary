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
package net.calm.iaclasslibrary.Lut;

import ij.plugin.LutLoader;
import java.awt.image.IndexColorModel;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class LUTCreator extends LutLoader {

    public LUTCreator() {
        super();
    }

    public IndexColorModel getRedGreen() {
        byte[] red = new byte[256];
        byte[] green = new byte[256];
        byte[] blue = new byte[256];
        redGreen(red, green, blue);
        return new IndexColorModel(8, 256, red, green, blue);
    }

    private void redGreen(byte[] reds, byte[] greens, byte[] blues) {
        for (int i = 0; i < 128; i++) {
            reds[i] = (byte) (256 - i * 2);
            greens[i] = (byte) 0;
            blues[i] = (byte) 0;
        }
        for (int i = 128; i < 256; i++) {
            reds[i] = (byte) 0;
            greens[i] = (byte) (i * 2);
            blues[i] = (byte) 0;
        }
    }
}
