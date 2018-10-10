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
package IO.BioFormats;

import ij.process.FloatProcessor;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.out.TiffWriter;
import loci.formats.services.OMEXMLService;

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

}
