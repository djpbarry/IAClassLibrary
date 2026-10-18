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
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FileInfo;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.quantity.Time;
import ome.units.unit.Unit;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;

/**
 * Primary Bio-Formats image loader. Wraps a {@code loci.formats.ImageReader}
 * plus {@code ImporterOptions}/{@code IMetadata} and loads pixel data for a
 * series and channel range into an ImageJ {@link ImagePlus}. See
 * {@link LocationAgnosticBioFormatsImg} for the location-agnostic alternative.
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BioFormatsImg implements AutoCloseable {

    protected ImporterOptions io;
    private final ImageReader reader;
    private IMetadata meta;
    protected String id;
    protected ImagePlus img = new ImagePlus();
    private ImagePlus processedImage;
    private boolean validID;
    public static final char SERIES_SEP = '-';
    public static final char LABEL_SEP = '_';
    public static final char REPLACEMENT_SEP = '.';

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
        return String.format("%s\nXY Spatial Res (%s): %f\nZ Spatial Res (%s): %f\n",
                id, lengthSymbol(xy), lengthValue(xy), lengthSymbol(z), lengthValue(z));
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
        return String.valueOf(meta.getPixelsDimensionOrder(series).getValue());
    }

    public int getPixelType(int series) {
        reader.setSeries(series);
        return reader.getPixelType();
    }

    public int getSizeC(int series) {
        return meta.getPixelsSizeC(series).getValue();
    }

    public int getSizeZ(int series) {
        return meta.getPixelsSizeZ(series).getValue();
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
            Length xy = getXYSpatialRes(series);
            Unit<Length> unit = xy != null ? xy.unit() : null;
            zRes = unit != null ? new Length(1.0, unit) : null;
        }
        return zRes;
    }

    /**
     * Returns the internal image directly, not a copy. Callers share a mutable
     * reference to the loaded image.
     *
     * @return the loaded {@link ImagePlus}
     */
    public ImagePlus getLoadedImage() {
        return img;
    }

    /**
     * Intentionally a no-op. Clearing the image data caused GUI problems, so
     * the loaded image is deliberately retained in memory.
     */
    public void clearImageData() {
    }

    /**
     * Closes the underlying Bio-Formats reader. Safe to call after loading is
     * complete; subsequent accessor calls will fail or return defaults.
     *
     * @throws IOException if the reader cannot be closed
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) throws IOException, FormatException {
        this.id = id;
        this.io.setId(id);
        this.reader.setId(id);
        this.validID = true;
    }

    public boolean checkID(String id) {
        return this.reader.isThisType(id);
    }

    public void loadPixelData(int series) {
        loadPixelData(series, 0, getSizeC(series) - 1, getDimOrder(series));
    }

    public void loadPixelData(int series, int cBegin, int cEnd, String dimOrder) {
        loadPixelData(series, cBegin, cEnd, 0, getSizeZ(series) - 1, 0, getSizeT(series) - 1, dimOrder);
    }

    public void loadPixelData(int series, int cBegin, int cEnd, int zBegin, int zEnd, int tBegin, int tEnd, String dimOrder) {
        if (series >= getSeriesCount() || cBegin < 0 || cEnd >= getSizeC(series) || cBegin > cEnd) {
            img = null;
            return;
        }
        try {
            reader.setSeries(series);
            if (reader.getRGBChannelCount() > 1) {
                cEnd = 1;
            }
            ImporterOptions options = new ImporterOptions();
            options.setId(id);
            options.clearSeries();
            options.setSeriesOn(series, true);
            options.setSpecifyRanges(true);
            options.setCBegin(series, cBegin);
            options.setCEnd(series, cEnd);
            options.setCStep(series, 1);
            options.setZBegin(series, zBegin);
            options.setZEnd(series, zEnd);
            options.setZStep(series, 1);
            options.setTBegin(series, tBegin);
            options.setTEnd(series, tEnd);
            options.setTStep(series, 1);
            if (dimOrder != null) {
                options.setInputOrder(series, dimOrder);
            }
            ImagePlus stackImp = BF.openImagePlus(options)[0];
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
                    lengthValue(getXYSpatialRes(s)),
                    lengthValue(getZSpatialRes(s)));
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
        return meta.getPixelsSizeX(series).getValue();
    }

    public int getSizeY(int series) {
        return meta.getPixelsSizeY(series).getValue();
    }

    public int getSizeT(int series) {
        return meta.getPixelsSizeT(series).getValue();
    }

    public String getSeriesName(int series) {
        return meta.getImageName(series);
    }

    public double[] getCalibration(int series) {
        return new double[]{lengthValue(getXYSpatialRes(series)),
                lengthValue(getXYSpatialRes(series)),
                lengthValue(getZSpatialRes(series))};
    }

    private static double lengthValue(Length length) {
        return (length != null && length.value() != null) ? length.value().doubleValue() : Double.NaN;
    }

    private static String lengthSymbol(Length length) {
        return (length != null && length.unit() != null) ? length.unit().getSymbol() : "microns";
    }

    /**
     * Returns the time increment between consecutive timepoints for the given
     * series, or {@code null} if the file does not carry time metadata.
     *
     * @param series the series index
     * @return the {@link Time} increment, or {@code null}
     */
    public Time getTimeIncrement(int series) {
        if (series >= getSeriesCount()) {
            return null;
        }
        return meta.getPixelsTimeIncrement(series);
    }

    /**
     * Returns the time increment in seconds, or {@link Double#NaN} when the
     * time metadata is absent or not convertible to seconds.
     *
     * @param series the series index
     * @return the time increment in seconds, or {@link Double#NaN}
     */
    public double getTimeIncrementSeconds(int series) {
        Time t = getTimeIncrement(series);
        if (t == null) {
            return Double.NaN;
        }
        Number seconds = t.value(UNITS.SECOND);
        return seconds != null ? seconds.doubleValue() : Double.NaN;
    }

    /**
     * Returns the time calibration (increment between timepoints) in seconds,
     * or {@link Double#NaN} when unavailable. Kept separate from
     * {@link #getCalibration(int)} (which returns {@code [x, y, z]} only) so the
     * spatial calibration's array shape is unchanged.
     *
     * @param series the series index
     * @return the time increment in seconds, or {@link Double#NaN}
     */
    public double getTimeCalibration(int series) {
        return getTimeIncrementSeconds(series);
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
        return lengthSymbol(getXYSpatialRes(series));
    }
}
