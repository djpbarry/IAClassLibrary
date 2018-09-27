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
package Process.Measure;

import Process.MultiThreadedProcess;
import ij.gui.Roi;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedMeasureROI extends MultiThreadedProcess {

    ArrayList<ArrayList<Roi>> allRois;
    int measurements;

    public MultiThreadedMeasureROI(ArrayList<ArrayList<Roi>> allRois, int measurements, Properties props) {
        super(null, props);
        this.allRois = allRois;
        this.measurements = measurements;
    }

    public void setup() {

    }

    @Override
    public void run() {

    }

}
