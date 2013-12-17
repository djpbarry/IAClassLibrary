/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IAClasses;

/**
 *
 * @author barry05
 */
public class PSF {

    private final double spatialRes, //Spatial resolution in nm per pixel
            lambda, //Wavelength of light
            numAp = 1.4; //Numerical aperture of system
    private double sigEst;

    public PSF(double spatialRes, double lambda) {
        this.spatialRes = spatialRes;
        this.lambda = lambda;
    }

    public final void calcPSF() {
        double airyRad = 1.22 * lambda / (2.0 * numAp); //Airy radius
        sigEst = airyRad / (2.0 * spatialRes * 1000.0);
    }

    public double getSigEst() {
        return sigEst;
    }

}
