package net.calm.iaclasslibrary.IO.BioFormats;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.Importer;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import ome.units.quantity.Length;
import ome.xml.meta.IMetadata;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;

public class LocationAgnosticBioFormatsImg extends BioFormatsImg {
    private final Importer importer = new Importer(null);
    private final IMetadata metadata;

    public LocationAgnosticBioFormatsImg(String options) {
        super(null);
        ImportProcess temp = null;
        try {
            if (options != null) {
                this.io = importer.parseOptions(options);
                temp = new ImportProcess(this.io);
                temp.execute();
                setId(io.getId());
            }
        } catch (IOException | FormatException e) {
            GenUtils.logError(e, "Could not open " + options);
        }
        if (temp != null) this.metadata = temp.getOMEMetadata();
        else this.metadata = null;
    }

    public void loadPixelData(int series, int cBegin, int cEnd, String dimOrder) {
        io.clearSeries();
        io.setSeriesOn(series, true);
        io.setCBegin(series, cBegin);
        io.setCEnd(series, cEnd);
        if (series >= getSeriesCount() || cBegin >= getSizeC(series) || cEnd > getSizeC(series)) {
            img = null;
            return;
        }
        try {
            ImportProcess process = new ImportProcess(io);
            process.execute();
            DisplayHandler displayHandler = new DisplayHandler(process);
            ImagePlusReader reader = new ImagePlusReader(process);
            ImagePlus[] imps = importer.readPixels(reader, io, displayHandler);
            this.img = imps[0];
            this.img.setTitle(String.format("%s-S%d_C%d", reformatFileName(FilenameUtils.getName(getId())), series, cBegin));
        } catch (Exception e) {
            GenUtils.logError(e, "There seems to be a problem opening that image!");
        }
    }

    public void setId(String id) throws IOException, FormatException {
        this.id = id;
        this.io.setId(id);
    }

    public int getSeriesCount() {
        if (metadata == null) return -1;
        return metadata.getImageCount();
    }

    public int getSizeC(int series) {
        if (metadata == null) return -1;
        return metadata.getChannelCount(series);
    }

    public int getSizeX(int series) {
        if (metadata == null) return -1;
        return metadata.getPixelsSizeX(series).getValue();
    }

    public int getSizeY(int series) {
        if (metadata == null) return -1;
        return metadata.getPixelsSizeY(series).getValue();
    }

    public int getSizeZ(int series) {
        if (metadata == null) return -1;
        return metadata.getPixelsSizeZ(series).getValue();
    }

    public Length getXYSpatialRes(int series) {
        if (metadata == null || series >= getSeriesCount()) {
            return null;
        }
        return metadata.getPixelsPhysicalSizeX(series);
    }

    public Length getZSpatialRes(int series) {
        if (metadata == null || series >= getSeriesCount()) {
            return null;
        }
        Length zRes = metadata.getPixelsPhysicalSizeZ(series);
        if (zRes == null && getSizeZ(series) > 1) {
            zRes = new Length(1.0, getXYSpatialRes(series).unit());
        }
        return zRes;
    }

}
