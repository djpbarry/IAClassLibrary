/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Math.Optimisation;

import Math.Correlation;
import static Math.Optimisation.Fitter.IterFactor;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.awt.Rectangle;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class PlateFitter extends Fitter {

    private Plate plateTemplate;
    private ImageProcessor image;

    public static void main(String[] args) {
        ImagePlus imp = IJ.openImage();
        PlateFitter fitter = new PlateFitter(imp.getProcessor(), 2, 3, 86, 30, 20, 10);
        fitter.doFit();
        double[] p = fitter.getParams();
        imp.setOverlay(fitter.getPlateTemplate().drawOverlay(p[0], p[1], p[2]));
        IJ.saveAs(imp, "TIF", "D:\\OneDrive - The Francis Crick Institute\\Working Data\\Sahai\\Karin\\overlay");
        System.out.println(String.format("X: %f, Y: %f, Theta: %f, Corr: %f", p[0], p[1], p[2], p[3]));
        System.exit(0);
    }

    public PlateFitter(ImageProcessor image, int rows, int cols, int wellRad, double xBuff, double yBuff, double interWellSpacing) {
        super();
        this.image = image;
        this.plateTemplate = new Plate(rows, cols, wellRad, xBuff, yBuff, interWellSpacing);
        numParams = 3;
    }

    public double evaluate(double[] params, double... x) {
        ImageProcessor template = plateTemplate.drawPlate(params[2]);
        Roi r = plateTemplate.getCropRoi();
        Rectangle bounds = r.getBounds();
        r.setLocation(params[0] - bounds.width / 2.0, params[1] - bounds.height / 2.0);
        image.setRoi(plateTemplate.getCropRoi());
        double[] coeffs = Correlation.imageCorrelation(image.crop(), template, Correlation.PEARSONS);
        if (coeffs == null) {
//            System.out.println(String.format("X: %f, Y: %f, Theta: %f, Corr: %f", params[0], params[1], params[2], -calcAreaOutside(image, plateTemplate.getCropRoi())));
            return -calcAreaOutside(image, plateTemplate.getCropRoi());
        } else {
//            System.out.println(String.format("X: %f, Y: %f, Theta: %f, Corr: %f", params[0], params[1], params[2], coeffs[0]));
            return coeffs[0];
        }
    }

    double calcAreaOutside(ImageProcessor image, Roi r) {
        Rectangle bounds = r.getBounds();
        Rectangle intersection = bounds.intersection(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        return bounds.width * bounds.height - intersection.width * intersection.height;
    }

    boolean initialize() {
        numVertices = numParams + 1; // need 1 more vertice than parametres,
        simp = new double[numVertices][numVertices];
        next = new double[numVertices];
        simp[0][0] = image.getWidth() / 2.0; // c
        simp[0][1] = image.getHeight() / 2.0; // a
        simp[0][2] = 180.0;
        maxIter = IterFactor * numParams * numParams; // Where does this estimate come from?
        restarts = defaultRestarts;
        nRestarts = 0;
        return true;
    }

    boolean sumResiduals(double[] x) {
        if (x == null) {
            return false;
        }
        double e = evaluate(x, null) - 1.0;
        x[numParams] = e * e;
        return true;
    }

    public Plate getPlateTemplate() {
        return plateTemplate;
    }

    boolean newVertex() {
        if (next == null) {
            return false;
        }
        System.arraycopy(next, 0, simp[worst], 0, numVertices);
        if (simp[worst][2] < 90.0) {
            simp[worst][2] = 90.0;
        } else if (simp[worst][2] > 270.0) {
            simp[worst][2] = 270.0;
        }
        return true;
    }

}
