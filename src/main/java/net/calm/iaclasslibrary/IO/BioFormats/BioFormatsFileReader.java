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
package net.calm.iaclasslibrary.IO.BioFormats;

import net.calm.iaclasslibrary.MetaData.ParamsReader;
import ij.ImagePlus;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;

import java.io.IOException;

/**
 *
 * // * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BioFormatsFileReader {
    
    public static int getSeriesCount(String fileName) throws FormatException, IOException {
        ImageReader reader = new ImageReader();
        reader.setId(fileName);
        return reader.getSeriesCount();
    }
    
    public static int getChannelCount(String fileName, int series) throws FormatException, IOException {
        ImageReader reader = new ImageReader();
        reader.setId(fileName);
        reader.setSeries(series);
        return reader.getSizeC();
    }

    public static double getXYSpatialRes(String fileName, int series) throws FormatException, IOException {
        ImporterOptions io = new ImporterOptions();
        io.setId(fileName);
        io.setSeriesOn(series, true);
        ImagePlus[] imps = BF.openImagePlus(io);
        ParamsReader pr = new ParamsReader(imps[0]);
        return pr.getXYSpatialRes();
    }
    
    public static double getZSpatialRes(String fileName, int series) throws FormatException, IOException {
        ImporterOptions io = new ImporterOptions();
        io.setId(fileName);
        io.setSeriesOn(series, true);
        ImagePlus[] imps = BF.openImagePlus(io);
        ParamsReader pr = new ParamsReader(imps[0]);
        return pr.getzSpatialRes();
    }
    
    public static Img openImage(String fileName, int series) throws FormatException, IOException {
        ImporterOptions io = new ImporterOptions();
        io.setId(fileName);
        io.setSeriesOn(series, true);
        return ImagePlusAdapter.wrap(BF.openImagePlus(io)[0]);
    }
}
