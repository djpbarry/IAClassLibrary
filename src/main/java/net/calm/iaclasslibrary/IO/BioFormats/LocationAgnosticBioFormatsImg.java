package net.calm.iaclasslibrary.IO.BioFormats;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.DisplayHandler;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.Importer;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;

public class LocationAgnosticBioFormatsImg extends BioFormatsImg {
    private final Importer importer = new Importer(null);

    public LocationAgnosticBioFormatsImg(String options) {
        super(null);
        try {
            if (options != null) {
                this.io = importer.parseOptions(options);
                setId(io.getId());
            }
        } catch (IOException | FormatException e) {
            GenUtils.logError(e, "Could not open " + options);
        }
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
}
