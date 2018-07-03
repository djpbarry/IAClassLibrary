/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Math.Optimisation;

import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.RoiRotator;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.LinkedList;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class Plate {

    public final static String PLATE_COMPONENT = "Plate Component";
    public final static String WELL = "Well";
    public final static String SHRUNK_WELL = "Shrunk Well";
    public final static String OUTLINE = "Outline";
    private Roi outline;
    private int nWellRows, nWellCols;
    private double wellRadius;
    private double xBuff;
    private double yBuff;
    private double interWellSpacing;
    private double shrinkFactor;
    private Roi cropRoi;

//    public static void main(String[] args) {
//        Plate p = new Plate(2, 3, 100);
//        (new ImagePlus("", p.drawPlate(10))).show();
//        System.exit(0);
//    }
    public Plate(int nWellRows, int nWellCols, double wellRadius, double xBuff, double yBuff, double interWellSpacing, double shrinkFactor) {
        this.nWellRows = nWellRows;
        this.nWellCols = nWellCols;
        this.wellRadius = wellRadius;
        this.xBuff = xBuff;
        this.yBuff = yBuff;
        this.interWellSpacing = interWellSpacing;
        this.shrinkFactor = shrinkFactor;
        this.outline = new Roi(0, 0,
                nWellCols * wellRadius * 2.0 + 2.0 * xBuff + (nWellCols - 1) * interWellSpacing,
                nWellRows * wellRadius * 2.0 + 2.0 * yBuff + (nWellRows - 1) * interWellSpacing);
    }

    ImageProcessor drawPlate(double angle) {
        Rectangle bounds = outline.getBounds();
        Roi plate = getPlateOutline(angle);
        Rectangle bounds2 = plate.getBounds();
        cropRoi = plate;
        ImageProcessor image = constructImage(bounds2);
        plate.setLocation(0, 0);
        image.draw(plate);
        double x = image.getWidth() / 2.0;
        double y = image.getHeight() / 2.0;
        for (int j = 1; j <= nWellRows * 2; j += 2) {
            for (int i = 1; i <= nWellCols * 2; i += 2) {
                image.draw(RoiRotator.rotate(
                        constructWell(x, y, bounds.width, bounds.height, i, j),
                        angle, x, y));
            }
        }
        return image;
    }

    public Overlay drawOverlay(double x, double y, double angle) {
        Overlay overlay = new Overlay();
        LinkedList<Roi> rois = drawRoi(x, y, angle);
        for (Roi r : rois) {
            overlay.add(r);
        }
        return overlay;
    }

    public LinkedList<Roi> drawRoi(double x, double y, double angle) {
        LinkedList<Roi> rois = new LinkedList();
        Rectangle bounds = outline.getBounds();
        double xc = bounds.width / 2.0;
        double yc = bounds.height / 2.0;
        Roi plate = getPlateOutline(angle);
        double xShift = (x - plate.getBounds().width / 2.0) - plate.getBounds().x;
        double yShift = (y - plate.getBounds().height / 2.0) - plate.getBounds().y;
        plate.setLocation(plate.getBounds().x + xShift, plate.getBounds().y + yShift);
        plate.setProperty(PLATE_COMPONENT, OUTLINE);
        rois.add(plate);
        for (int j = 1; j <= nWellRows * 2; j += 2) {
            for (int i = 1; i <= nWellCols * 2; i += 2) {
                OvalRoi well = constructWell(xc, yc, bounds.width, bounds.height, i, j);
                Rectangle wellBounds = well.getBounds();
                double shrunkWellX = wellBounds.x + ((1.0 - shrinkFactor) / 2.0) * wellBounds.width;
                double shrunkWellY = wellBounds.y + ((1.0 - shrinkFactor) / 2.0) * wellBounds.height;
                OvalRoi shrunkWell = new OvalRoi(shrunkWellX, shrunkWellY, wellBounds.width * shrinkFactor, wellBounds.height * shrinkFactor);
                Roi rotatedWell = RoiRotator.rotate(well, angle, xc, yc);
                rotatedWell.setLocation(rotatedWell.getBounds().x + xShift, rotatedWell.getBounds().y + yShift);
                rotatedWell.setProperty(PLATE_COMPONENT, WELL);
                rois.add(rotatedWell);
                Roi rotatedShrunkWell = RoiRotator.rotate(shrunkWell, angle, xc, yc);
                rotatedShrunkWell.setLocation(rotatedShrunkWell.getBounds().x + xShift, rotatedShrunkWell.getBounds().y + yShift);
                rotatedShrunkWell.setProperty(PLATE_COMPONENT, SHRUNK_WELL);
                rois.add(rotatedShrunkWell);
            }
        }
        return rois;
    }

    OvalRoi constructWell(double x, double y, int width, int height, int i, int j) {
        double x0 = x - width / 2.0 + (double) i * wellRadius + xBuff + ((i - 1) / 2) * interWellSpacing;
        double y0 = y - height / 2.0 + (double) j * wellRadius + yBuff + ((j - 1) / 2) * interWellSpacing;
        return new OvalRoi(x0 - wellRadius, y0 - wellRadius, 2 * wellRadius, 2 * wellRadius);
    }

    Roi getPlateOutline(double angle) {
        Rectangle bounds = outline.getBounds();
        double x = bounds.width / 2.0;
        double y = bounds.height / 2.0;
        return RoiRotator.rotate(new Roi(0, 0, bounds.width, bounds.height), angle, x, y);
    }

    ImageProcessor constructImage(Rectangle roi) {
        ByteProcessor bp = new ByteProcessor(roi.width, roi.height);
        bp.setValue(255);
        bp.fill();
        bp.setValue(0);
        bp.setLineWidth(2);
        return bp;
    }

    public Roi getCropRoi() {
        return cropRoi;
    }
}
