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

import Process.RunnableProcess;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.Analyzer;
import java.util.ArrayList;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class RunnableMeasureROI extends RunnableProcess {

    ImagePlus image;
    ArrayList<Roi> allRois;

    public RunnableMeasureROI(ImagePlus image, ArrayList<Roi> allRois) {
        super(null);
        this.image = image;
        this.allRois = allRois;
    }

    @Override
    public void run() {
        for (Roi r : allRois) {
            image.setRoi(r);
            (new Analyzer(image)).measure();
        }
    }
}
