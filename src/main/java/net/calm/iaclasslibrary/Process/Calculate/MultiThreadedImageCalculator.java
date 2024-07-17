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
package net.calm.iaclasslibrary.Process.Calculate;

import net.calm.iaclasslibrary.IO.BioFormats.LocationAgnosticBioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import ij.plugin.ImageCalculator;
import java.util.Properties;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedImageCalculator extends MultiThreadedProcess {

    private final String outputName;
    private final String operation;

    public MultiThreadedImageCalculator(MultiThreadedProcess[] inputs, String outputName, String operation) {
        super(inputs);
        this.outputName = outputName;
        this.operation = operation;
    }

    public void setup(LocationAgnosticBioFormatsImg img, Properties props, String[] propLabels) {

    }

    public void run() {
        this.output = (new ImageCalculator()).run(String.format("%s create stack", operation), inputs[0].getOutput(), inputs[1].getOutput());
        labelOutput(inputs[0].getOutput().getTitle(), outputName);
    }

    public MultiThreadedImageCalculator duplicate() {
        MultiThreadedImageCalculator newProcess = new MultiThreadedImageCalculator(inputs, outputName, operation);
        this.updateOutputDests(newProcess);
        return newProcess;
    }
}
