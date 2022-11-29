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

import ij.ImagePlus;
import ij.ImageStack;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FileInfo;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.in.ImporterOptions;
import net.calm.iaclasslibrary.Process.IO.MultiThreadedImageLoader;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BioFormatsImg {

    private ImporterOptions io;
    private final ImageReader reader;
    private IMetadata meta;
    private String id;
    private ImagePlus img = new ImagePlus();
    private ImagePlus processedImage;
    private boolean validID;
    public static char SERIES_SEP = '-';
    public static char LABEL_SEP = '_';
    public static char REPLACEMENT_SEP = '.';

    public BioFormatsImg() {
        this(null);
    }

    public BioFormatsImg(String id) {
        reader = new ImageReader();
        try {
            this.io = new ImporterOptions();
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            this.meta = service.createOMEXMLMetadata();
            this.reader.setMetadataStore(meta);
            if (id != null) {
                this.setId(id);
            }
        } catch (IOException e) {
            GenUtils.logError(e, String.format("Problem encountered opening %s.", id));
        } catch (DependencyException e) {
            GenUtils.logError(e, "Problem initialising Bio-Formats services.");
        } catch (ServiceException e) {
            GenUtils.logError(e, "Could not initialise metadata object.");
        } catch (FormatException e) {
            GenUtils.logError(e, "Unrecognised image format.");
        }
    }

    public String toString(int series) {
        Length xy = getXYSpatialRes(series);
        Length z = getZSpatialRes(series);
        return String.format("%s\nXY Spatial Res (%s): %f\nZ Spatial Res (%s): %f\n", id, xy.unit().getSymbol(), xy.value().floatValue(), z.unit().getSymbol(), z.value().floatValue());
    }

    public int getSeriesCount() {
        try {
            return reader.getSeriesCount();
        } catch (IllegalStateException e) {
            return -1;
        }
    }

    public int getCurrentSeries() {
        return reader.getSeries();
    }

    public String getDimOrder(int series) {
        reader.setSeries(series);
        return reader.getDimensionOrder();
    }

    public int getPixelType(int series) {
        reader.setSeries(series);
        return reader.getPixelType();
    }

    public int getSizeC(int series) {
        reader.setSeries(series);
        return reader.getSizeC();
    }

    public int getSizeZ(int series) {
        reader.setSeries(series);
        return reader.getSizeZ();
    }

    public Length getXYSpatialRes(int series) {
        if (series >= getSeriesCount()) {
            return null;
        }
        return meta.getPixelsPhysicalSizeX(series);
    }

    public Length getZSpatialRes(int series) {
        if (series >= getSeriesCount()) {
            return null;
        }
        Length zRes = meta.getPixelsPhysicalSizeZ(series);
        if (zRes == null && getSizeZ(series) > 1) {
            zRes = new Length(1.0, getXYSpatialRes(series).unit());
        }
        return zRes;
    }

    public ImagePlus getLoadedImage() {
        ImagePlus dup = img.duplicate();
        dup.setTitle(img.getTitle());
        return dup;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) throws IOException, FormatException {
        this.id = id;
        this.io.setId(id);
        this.reader.setId(id);
    }

    public boolean checkID(String id) {
        return this.reader.isThisType(id);
    }

    public void loadPixelData(int series) {
        loadPixelData(series, 0, this.getSizeC(series), reader.getDimensionOrder());
    }

    public void loadPixelData(int series, int cBegin, int cEnd, String dimOrder) {
        if (series >= getSeriesCount() || cBegin >= getSizeC(series) || cEnd > getSizeC(series)) {
            img = null;
            return;
        }
        try {
            reader.setSeries(series);
            int sizeC = reader.getSizeC();
            if (reader.getRGBChannelCount() > 1) {
                sizeC = 1;
                cEnd = 1;
            }
            int[] limits = getLimits(dimOrder, cBegin, cEnd, sizeC);
            int width = reader.getSizeX();
            int height = reader.getSizeY();
            ImageStack stack = new ImageStack(width, height, (cEnd - cBegin) * reader.getSizeT() * reader.getSizeZ() * reader.getRGBChannelCount());
            MultiThreadedImageLoader loader = new MultiThreadedImageLoader(limits, reader, stack);
            loader.start();
            loader.join();
            ImagePlus stackImp = new ImagePlus(id, stack);
            stackImp.setDimensions(cEnd - cBegin, reader.getSizeZ(), reader.getSizeT());
            stackImp.setOpenAsHyperStack(true);
            img = stackImp;
            img.setTitle(String.format("%s-S%d_C%d", reformatFileName(FilenameUtils.getName(getId())), series, cBegin));
        } catch (Exception e) {
            GenUtils.logError(e, "There seems to be a problem opening that image!");
        }
    }

    public String reformatFileName() {
        return reformatFileName(FilenameUtils.getName(id));
    }

    String reformatFileName(String originalFileName) {
        return originalFileName.replace(LABEL_SEP, REPLACEMENT_SEP).replace(SERIES_SEP, REPLACEMENT_SEP);
    }

    private int[] getLimits(String dimOrder, int cBegin, int cEnd, int sizeC) {
        if (dimOrder == null) {
            dimOrder = reader.getDimensionOrder();
        }
        int[] limits = new int[9];
        switch (dimOrder) {
            case "XYZCT":
                limits[0] = 0;
                limits[1] = reader.getSizeZ();
                limits[2] = reader.getSizeZ();
                limits[3] = cBegin;
                limits[4] = cEnd;
                limits[5] = sizeC;
                limits[6] = 0;
                limits[7] = reader.getSizeT();
                limits[8] = reader.getSizeT();
                break;
            case "XYCTZ":
                limits[0] = cBegin;
                limits[1] = cEnd;
                limits[2] = sizeC;
                limits[3] = 0;
                limits[4] = reader.getSizeT();
                limits[5] = reader.getSizeT();
                limits[6] = 0;
                limits[7] = reader.getSizeZ();
                limits[8] = reader.getSizeZ();
                break;
            case "XYCZT":
                limits[0] = cBegin;
                limits[1] = cEnd;
                limits[2] = sizeC;
                limits[3] = 0;
                limits[4] = reader.getSizeZ();
                limits[5] = reader.getSizeZ();
                limits[6] = 0;
                limits[7] = reader.getSizeT();
                limits[8] = reader.getSizeT();
                break;
            case "XYTCZ":
                limits[0] = 0;
                limits[1] = reader.getSizeT();
                limits[2] = reader.getSizeT();
                limits[3] = cBegin;
                limits[4] = cEnd;
                limits[5] = sizeC;
                limits[6] = 0;
                limits[7] = reader.getSizeZ();
                limits[8] = reader.getSizeZ();
                break;
            case "XYTZC":
                limits[0] = 0;
                limits[1] = reader.getSizeT();
                limits[2] = reader.getSizeT();
                limits[3] = 0;
                limits[4] = reader.getSizeZ();
                limits[5] = reader.getSizeZ();
                limits[6] = cBegin;
                limits[7] = cEnd;
                limits[8] = sizeC;
                break;
            case "XYZTC":
                limits[0] = 0;
                limits[1] = reader.getSizeZ();
                limits[2] = reader.getSizeZ();
                limits[3] = 0;
                limits[4] = reader.getSizeT();
                limits[5] = reader.getSizeT();
                limits[6] = cBegin;
                limits[7] = cEnd;
                limits[8] = sizeC;
                break;
        }
        return limits;
    }

    public void setProcessedImage(ImagePlus processedImage) {
        this.processedImage = processedImage;
    }

    public ImagePlus getProcessedImage() {
        return processedImage;
    }

    public String getInfo(int s) {
        if (isValidID()) {
            return String.format("%s\n"
                            + "XY Spatial Res: %f\n"
                            + "Z Spatial Res: %f", getId(),
                    getXYSpatialRes(s).value().floatValue(),
                    getZSpatialRes(s).value().floatValue());
        } else {
            return "Invalid image ID";
        }
    }

    public float[][] getProcessedStackPixels() {
        if (processedImage == null) {
            return null;
        }
        Object[] pixels = processedImage.getImageStack().getImageArray();
        float[][] output = new float[pixels.length][];
        for (int i = 0; i < pixels.length; i++) {
            output[i] = (float[]) pixels[i];
        }
        return output;
    }

    public boolean isValidID() {
        return validID;
    }

    public IMetadata getMeta() {
        return meta;
    }

    public int getSizeX(int series) {
        reader.setSeries(series);
        return reader.getSizeX();
    }

    public int getSizeY(int series) {
        reader.setSeries(series);
        return reader.getSizeY();
    }

    public int getSizeT(int series) {
        reader.setSeries(series);
        return reader.getSizeT();
    }

    public String getSeriesName(int series) {
        return meta.getImageName(series);
    }

    public double[] getCalibration(int series) {
        return new double[]{getXYSpatialRes(series).value().doubleValue(),
                getXYSpatialRes(series).value().doubleValue(),
                getZSpatialRes(series).value().doubleValue()};
    }

    public String[] getFileList() {
        FileInfo[] files = reader.getAdvancedUsedFiles(false);
        String[] output = new String[files.length];
        for (int f = 0; f < files.length; f++) {
            output[f] = files[f].filename;
        }
        return output;
    }

    public String getUnits(int series) {
        Unit<Length> u = getXYSpatialRes(series).unit();
        if (u != null) return u.getSymbol();
        else return "microns";
    }
}
