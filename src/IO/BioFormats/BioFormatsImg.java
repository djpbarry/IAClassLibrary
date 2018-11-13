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
package IO.BioFormats;

import UtilClasses.GenUtils;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.HyperStackConverter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.io.IOException;
import java.nio.ByteBuffer;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.in.ImporterOptions;
import ome.units.quantity.Length;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BioFormatsImg {

    private ImporterOptions io;
    private final ImageReader reader;
    private IMetadata meta;
    private String id;
    private ImagePlus img = new ImagePlus();
    private ImagePlus tempImg;
    private boolean validID;

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
        } catch (IOException e) {
            GenUtils.logError(e, String.format("Problem encountered opening %s.", id));
        } catch (DependencyException e) {
            GenUtils.logError(e, "Problem initialising Bio-Formats services.");
        } catch (ServiceException e) {
            GenUtils.logError(e, "Could not initialise metadata object.");
        }
        if (id != null) {
            this.setId(id);
        }
    }

    public String toString(int series) {
        Length xy = getXYSpatialRes(series);
        Length z = getZSpatialRes(series);
        return String.format("%s\nXY Spatial Res (%s): %f\nZ Spatial Res (%s): %f\n", id, xy.unit().getSymbol(), xy.value().floatValue(), z.unit().getSymbol(), z.value().floatValue());
    }

    public int getSeriesCount() {
        return reader.getSeriesCount();
    }

    private String getDimOrder() {
        return reader.getDimensionOrder();
    }

    public int getChannelCount() {
        return reader.getSizeC();
    }

    public int getSizeZ() {
        return reader.getSizeZ();
    }

    public Length getXYSpatialRes(int series) {
        return meta.getPixelsPhysicalSizeX(series);
    }

    public Length getZSpatialRes(int series) {
        return meta.getPixelsPhysicalSizeZ(series);
    }

    public ImagePlus getImg() {
        return img;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        validID = false;
        try {
            this.io.setId(id);
            this.reader.setId(id);
        } catch (IOException e) {
            GenUtils.logError(e, String.format("Problem encountered opening %s.", id));
            return;
        } catch (FormatException e) {
            GenUtils.logError(e, String.format("%s is not a supported format.", id));
            return;
        }
        validID = true;
    }

    public void setImg(int series) {
        setImg(series, 0, this.getChannelCount(), reader.getDimensionOrder());
    }

    public void setImg(int series, int cBegin, int cEnd, String dimOrder) {
        try {
            reader.setSeries(series);
            int[] limits = getLimits(dimOrder, cBegin, cEnd);
            int width = reader.getSizeX();
            int height = reader.getSizeY();
            int bitDepth = reader.getBitsPerPixel();
            ImageStack stack = new ImageStack(width, height);
            for (int k = limits[6]; k < limits[7]; k++) {
                for (int j = limits[3]; j < limits[4]; j++) {
                    for (int i = limits[0]; i < limits[1]; i++) {
                        ImageProcessor ip;
                        byte[] bytePix = reader.openBytes(k * limits[5] * limits[2] + j * limits[2] + i);
                        if (bitDepth == 16) {
                            short[] shortPix = new short[width * height];
                            for (int index = 0; index < shortPix.length; index++) {
                                shortPix[index] = ByteBuffer.wrap(new byte[]{bytePix[2 * index + 1], bytePix[2 * index]}).getShort();
                            }
                            ip = new ShortProcessor(width, height);
                            ip.setPixels(shortPix);
                        } else {
                            ip = new ByteProcessor(width, height);
                            ip.setPixels(bytePix);
                        }
                        stack.addSlice(ip);
                    }
                }
            }
            ImagePlus stackImp = new ImagePlus(id, stack);
            img = stackImp;
        } catch (Exception e) {
            GenUtils.logError(e, "There seems to be a problem opening that image!");
        }
    }

    private int[] getLimits(String dimOrder, int cBegin, int cEnd) {
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
                limits[5] = reader.getSizeC();
                limits[6] = 0;
                limits[7] = reader.getSizeT();
                limits[8] = reader.getSizeT();
                break;
            case "XYCTZ":
                limits[0] = cBegin;
                limits[1] = cEnd;
                limits[2] = reader.getSizeC();
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
                limits[2] = reader.getSizeC();
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
                limits[5] = reader.getSizeC();
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
                limits[8] = reader.getSizeC();
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
                limits[8] = reader.getSizeC();
                break;
        }
        return limits;
    }

    public void setTempImg(ImagePlus tempImg) {
        this.tempImg = tempImg;
    }

    public ImagePlus getTempImg() {
        return tempImg;
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

    public float[][] getTempStackPixels() {
        if (tempImg == null) {
            return null;
        }
        Object[] pixels = tempImg.getImageStack().getImageArray();
        float[][] output = new float[pixels.length][];
        for (int i = 0; i < pixels.length; i++) {
            output[i] = (float[]) pixels[i];
        }
        return output;
    }

    public boolean isValidID() {
        return validID;
    }

}
