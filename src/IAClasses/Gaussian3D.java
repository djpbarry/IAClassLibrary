/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package IAClasses;

import Particle.IsoGaussian;

/**
 *
 * @author barry05
 */
public class Gaussian3D extends IsoGaussian {

    private double z0, zSigma;

    public Gaussian3D(double x0, double y0, double z0, double mag, double xsig,
            double ysig, double zsig, double fit) {
        super();
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.magnitude = mag;
        this.xSigma = xsig;
        this.ySigma = ysig;
        this.zSigma = zsig;
        this.fit = fit;
    }

    public double evaluate(double x, double y, double z) {
        double result = magnitude * Math.exp(-(Math.pow(x - this.x0, 2.0) / (2 * xSigma * xSigma)
                + Math.pow(y - this.y0, 2.0) / (2 * ySigma * ySigma)
                + Math.pow(z - this.z0, 2.0) / (2 * zSigma * zSigma)));
        return result;
    }

    public double getZ0() {
        return z0;
    }

    public double getzSigma() {
        return zSigma;
    }
    
    
}
