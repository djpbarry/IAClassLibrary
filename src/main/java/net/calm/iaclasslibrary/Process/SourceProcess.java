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
package net.calm.iaclasslibrary.Process;

/**
 * A process that loads its own raw pixels rather than reading from upstream
 * processes. Sources have no {@code inputs}; {@link #loadPixels(int, int)} owns
 * the dimension → load mapping.
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public abstract class SourceProcess extends MultiThreadedProcess {

    protected SourceProcess() {
        super(null);
    }

    /**
     * Loads pixel data for the configured series/channel/time into the managed
     * image, owning the dimension → load mapping. Equivalent to
     * {@code img.loadPixelData(series, cBegin, cEnd, 0, img.getSizeZ(series) - 1, time, time, null)}.
     */
    protected void loadPixels(int cBegin, int cEnd) {
        img.loadPixelData(series, cBegin, cEnd, 0, img.getSizeZ(series) - 1, time, time, null);
    }
}
