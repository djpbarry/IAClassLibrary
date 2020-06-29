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
package net.calm.iaclasslibrary.Math.Correlation;

import ij.WindowManager;
import ij.gui.Roi;
import ij.macro.ExtensionDescriptor;
import ij.macro.MacroExtension;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.awt.Rectangle;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class ImageCorrelator implements MacroExtension {

    private Roi roi;
    private ImageProcessor image1;
    private ImageProcessor image2;
    private String label;

    private final String[] extensionFunctionNames = new String[]{"runImageCorrelation"};

    public ImageCorrelator() {

    }

    void run() {
        ImageProcessor mask = roi.getMask();
        Rectangle bounds = roi.getBounds();
        ImageStatistics stats = ImageStatistics.getStatistics(mask);
        int nMaskPix = stats.histogram[255];
        double[] fp1Pix = new double[nMaskPix];
        double[] fp2Pix = new double[nMaskPix];
        int count = 0;
        for (int y = bounds.y; y < bounds.y + bounds.height && y < image1.getHeight(); y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width && x < image2.getWidth(); x++) {
                if (mask.getPixel(x - bounds.x, y - bounds.y) > 0) {
                    fp1Pix[count] = image1.getPixelValue(x, y);
                    fp2Pix[count] = image2.getPixelValue(x, y);
                    count++;
                }
            }
        }
        ResultsTable rt = ResultsTable.getResultsTable();
        rt.incrementCounter();
        rt.addValue("Pearsons", Correlation.pearsons(fp1Pix, fp2Pix));
        rt.addValue("Spearmans", Correlation.spearman(fp1Pix, fp2Pix));
        rt.setDecimalPlaces(rt.getColumnIndex("Pearsons"), 4);
        rt.setDecimalPlaces(rt.getColumnIndex("Spearmans"), 4);
        rt.addLabel(label);
        rt.show("Results");
    }

    public ExtensionDescriptor[] getExtensionFunctions() {
        return new ExtensionDescriptor[]{
            new ExtensionDescriptor(extensionFunctionNames[0], new int[]{
                MacroExtension.ARG_STRING, MacroExtension.ARG_STRING, MacroExtension.ARG_STRING
            }, this)
        };
    }

    public String handleExtension(String name, Object[] args) {
        if (name.contentEquals(extensionFunctionNames[0])) {
            if (!(args[0] instanceof String && args[1] instanceof String && args[2] instanceof String)) {
                System.out.print(String.format("Error: arguments passed to %s are not valid.", extensionFunctionNames[0]));
                return "";
            }
            this.roi = WindowManager.getImage((String) args[0]).getRoi();
            this.image1 = WindowManager.getImage((String) args[0]).getProcessor();
            this.image2 = WindowManager.getImage((String) args[1]).getProcessor();
            this.label = (String) args[2];
            run();
        }
        return null;
    }
}
