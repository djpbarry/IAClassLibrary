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

import ij.process.ByteProcessor;
import java.io.File;
import loci.common.services.ServiceFactory;
import loci.formats.FormatTools;
import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

/**
 * Demonstrates the minimum amount of metadata necessary to write out an image
 * plane.
 */
public class BioFormatsImageWriter {

    public BioFormatsImageWriter() {

    }

    public static void saveImage(ByteProcessor ip, File filename) throws Exception {
        String id = filename.getAbsolutePath();

        int w = ip.getWidth(), h = ip.getHeight(), c = 1;
        int pixelType = FormatTools.UINT16;
        byte[] img = (byte[]) ip.getPixels();

        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata meta = service.createOMEXMLMetadata();

        MetadataTools.populateMetadata(meta, 0, null, false, "XYZCT",
                FormatTools.getPixelTypeString(pixelType), w, h, 1, c, 1, c);
//        RoiManager rm = RoiManager.getInstance2();
//        if (rm == null) {
//            rm = new RoiManager();
//        }
//        ROIHandler.saveROIs(meta);

        IFormatWriter writer = new ImageWriter();
        writer.setMetadataRetrieve(meta);

        writer.setId(id);
        writer.setCompression("LZW");
        writer.saveBytes(0, img);
        writer.close();
    }

}
