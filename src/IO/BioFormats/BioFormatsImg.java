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

import ij.ImagePlus;
import java.io.IOException;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import ome.units.quantity.Length;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BioFormatsImg {

    private final ImporterOptions io;
    private final ImageReader reader;
    private final IMetadata meta;
    private final String id;

    public BioFormatsImg(String id) throws IOException, DependencyException, ServiceException, FormatException {
        this.id = id;
        io = new ImporterOptions();
        reader = new ImageReader();
        io.setId(id);
        reader.setId(id);
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        meta = service.createOMEXMLMetadata();
        service.convertMetadata(meta, reader.getMetadataStore());
    }

    public String toString(int series) {
        Length xy = getXYSpatialRes(series);
        Length z = getZSpatialRes(series);
        return String.format("%s\nXY Spatial Res (%s): %f\nZ Spatial Res (%s): %f\n", id, xy.unit().getSymbol(), xy.value().floatValue(), z.unit().getSymbol(), z.value().floatValue());
    }
    
    public ImagePlus getImg(int series, int channel) throws FormatException, IOException {
        io.setSeriesOn(series, true);
        io.setCBegin(series, channel);
        io.setCEnd(series, channel);
        return BF.openImagePlus(io)[0];
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

    public Length getXYSpatialRes(int series) {
        return meta.getPixelsPhysicalSizeX(series);
    }

    public Length getZSpatialRes(int series) {
        return meta.getPixelsPhysicalSizeZ(series);
    }

}
