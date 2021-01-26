/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2005 - 2017 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package net.calm.iaclasslibrary.IO.BioFormats;

import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.out.TiffWriter;
import loci.formats.services.OMEXMLService;
import loci.formats.tiff.TiffCompression;

import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Demonstrates the minimum amount of metadata necessary to write out an image
 * plane.
 */
public class BioFormatsImageWriter {

    public BioFormatsImageWriter() {

    }

    public static void saveImage(FloatProcessor ip, File filename, IndexColorModel lut) throws DependencyException, ServiceException, FormatException, IOException {
        String id = filename.getAbsolutePath();

        int w = ip.getWidth(), h = ip.getHeight();
        int pixelType = FormatTools.FLOAT;
        float[] floatPix = (float[]) ip.getPixels();
        byte[] img = new byte[floatPix.length * 4];
        for (int i = 0; i < floatPix.length; i++) {
            byte[] currentPix = ByteBuffer.allocate(4).putFloat(floatPix[i]).array();
            for (int j = 0; j < 4; j++) {
                img[i * 4 + j] = currentPix[j];
            }
        }
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata meta = service.createOMEXMLMetadata();

        MetadataTools.populateMetadata(meta, 0, filename.getName(), false, "XYZCT",
                FormatTools.getPixelTypeString(pixelType), w, h, 1, 1, 1, 1);

        TiffWriter writer = new TiffWriter();
        writer.setMetadataRetrieve(meta);
        writer.setValidBitsPerPixel(32);
        writer.setId(id);
        if (lut != null) {
            writer.setColorModel(lut);
        }
        writer.setCompression("LZW");
        writer.saveBytes(0, img);
        writer.close();
    }

    public static void saveStack(ImageStack stack, File filename, IndexColorModel lut, int pixelType, String dimOrder, int[] dims, boolean bigTiff) throws DependencyException, ServiceException, FormatException, IOException {
        String id = filename.getAbsolutePath();

        int bitDepth;
        switch (pixelType) {
            case FormatTools.FLOAT:
                bitDepth = 32;
                break;
            case FormatTools.UINT16:
                bitDepth = 16;
                break;
            default:
                bitDepth = 8;
        }

        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata meta = service.createOMEXMLMetadata();
        int nSlices = stack.getSize();
        for (int s = 0; s < nSlices; s++) {
            MetadataTools.populateMetadata(meta, s, filename.getName(), false, dimOrder,
                    FormatTools.getPixelTypeString(pixelType), dims[0], dims[1], dims[2], dims[3], dims[4], 1);
        }
        TiffWriter writer = new TiffWriter();
        writer.setMetadataRetrieve(meta);
        writer.setValidBitsPerPixel(bitDepth);
        writer.setId(id);
        if (lut != null) {
            writer.setColorModel(lut);
        }
        writer.setCompression(TiffCompression.LZW.toString());
        for (int s = 0; s < nSlices; s++) {
            MetadataTools.populateMetadata(meta, s, filename.getName(), false, dimOrder,
                    FormatTools.getPixelTypeString(pixelType), dims[0], dims[1], dims[2], dims[3], dims[4], 1);
            byte[] img;
            switch (pixelType) {
                case (FormatTools.FLOAT):
                    img = getFloatPix(stack.getProcessor(s + 1));
                    break;
                case (FormatTools.UINT16):
                    img = getShortPix(stack.getProcessor(s + 1));
                    break;
                default:
                    img = (byte[]) ((stack.getProcessor(s + 1)).getPixels());
            }
            writer.saveBytes(s, img);
        }
        writer.close();
    }

    static byte[] getFloatPix(ImageProcessor ip) {
        float[] floatPix = (float[]) ip.getPixels();
        byte[] img = new byte[floatPix.length * 4];
        for (int i = 0; i < floatPix.length; i++) {
            byte[] currentPix = ByteBuffer.allocate(4).putFloat(floatPix[i]).array();
            for (int j = 0; j < 4; j++) {
                img[i * 4 + j] = currentPix[j];
            }
        }
        return img;
    }

    static byte[] getShortPix(ImageProcessor ip) {
        short[] shortPix = (short[]) ip.getPixels();
        byte[] img = new byte[shortPix.length * 2];
        for (int i = 0; i < shortPix.length; i++) {
            byte[] currentPix = ByteBuffer.allocate(2).putShort(shortPix[i]).array();
            for (int j = 0; j < 2; j++) {
                img[i * 2 + j] = currentPix[j];
            }
        }
        return img;
    }
}
