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

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class Plate {

    private Roi outline;
    private int nWellRows, nWellCols;
    private double wellRadius;
    private Roi cropRoi;

//    public static void main(String[] args) {
//        Plate p = new Plate(2, 3, 100);
//        (new ImagePlus("", p.drawPlate(10))).show();
//        System.exit(0);
//    }
    public Plate(int nWellRows, int nWellCols, double wellRadius) {
        this.nWellRows = nWellRows;
        this.nWellCols = nWellCols;
        this.wellRadius = wellRadius;
        this.outline = new Roi(0, 0, nWellCols * wellRadius * 2.0, nWellRows * wellRadius * 2.0);
    }

    ImageProcessor drawPlate(double angle) {
        Rectangle bounds = outline.getBounds();
        double x = bounds.width / 2.0;
        double y = bounds.height / 2.0;
        Roi plate = RoiRotator.rotate(new Roi(0, 0, bounds.width, bounds.height), angle, x, y);
        Rectangle bounds2 = plate.getBounds();
        cropRoi = plate;
        ImageProcessor image = constructImage(bounds2);
        plate.setLocation(0, 0);
        image.draw(plate);
        x = image.getWidth() / 2.0;
        y = image.getHeight() / 2.0;
        for (int j = 1; j <= nWellRows * 2; j += 2) {
            for (int i = 1; i <= nWellCols * 2; i += 2) {
                double x0 = x - bounds.width / 2.0 + (double) i * wellRadius;
                double y0 = y - bounds.height / 2.0 + (double) j * wellRadius;
                OvalRoi well = new OvalRoi(x0 - wellRadius, y0 - wellRadius, 2 * wellRadius, 2 * wellRadius);
                image.draw(RoiRotator.rotate(well, angle, x, y));
            }
        }
        return image;
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

    Overlay drawOverlay(double x, double y, double angle) {
        Overlay overlay = new Overlay();
        Rectangle bounds = outline.getBounds();
        double xc = bounds.width / 2.0;
        double yc = bounds.height / 2.0;
        Roi plate = RoiRotator.rotate(new Roi(0, 0, bounds.width, bounds.height), angle, xc, yc);
        double xShift = (x - plate.getBounds().width / 2.0) - plate.getBounds().x ;
        double yShift = (y - plate.getBounds().height / 2.0) - plate.getBounds().y ;
        plate.setLocation(plate.getBounds().x + xShift, plate.getBounds().y + yShift);
        overlay.add(plate);
        for (int j = 1; j <= nWellRows * 2; j += 2) {
            for (int i = 1; i <= nWellCols * 2; i += 2) {
                double x0 = xc - bounds.width / 2.0 + (double) i * wellRadius;
                double y0 = yc - bounds.height / 2.0 + (double) j * wellRadius;
                OvalRoi well = new OvalRoi(x0 - wellRadius, y0 - wellRadius, 2 * wellRadius, 2 * wellRadius);
                Roi well2 = RoiRotator.rotate(well, angle, xc, yc);
                well2.setLocation(well2.getBounds().x + xShift, well2.getBounds().y + yShift);
                overlay.add(well2);
            }
        }
        return overlay;
    }

}
