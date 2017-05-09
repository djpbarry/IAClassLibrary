package Particle;

/**
 * Representation of a 2D IsoGaussian curve.
 *
 * @author David J Barry
 * @version 1.0, JAN 2011
 */
public class IsoGaussian extends Particle {

    protected double xSigma, ySigma, fit;

    public IsoGaussian(int t, IsoGaussian particle) {
        this(t, particle.getX(), particle.getY(), particle.getMagnitude(), particle.getXSigma(), particle.getYSigma(), particle.getFit(), null, -1);
    }

    public IsoGaussian() {
        this(0.0, 0.0, 0.0, 0.0);
    }

    public IsoGaussian(double x0, double y0, double a, double sig) {
        this(x0, y0, a, sig, sig, 0.0);
    }

    public IsoGaussian(double x0, double y0, double a, double xsig, double ysig, double fit) {
        this(0, x0, y0, a, xsig, ysig, fit, null, -1);
    }

    public IsoGaussian(int t, double x0, double y0, double a, double xsig, double ysig, double fit, Particle link, int iD) {
        super(t, link, iD, a);
        this.x = x0;
        this.y = y0;
        this.xSigma = xsig;
        this.ySigma = ysig;
        this.fit = fit;
    }

    public double getXSigma() {
        return xSigma;
    }

    public double getYSigma() {
        return ySigma;
    }

    public double getFit() {
        return fit;
    }

    public double evaluate(double x, double y) {
        double result = magnitude * Math.exp(-(((x - this.x) * (x - this.x))
                + ((y - this.y) * (y - this.y))) / (2 * xSigma * xSigma));
        return result;
    }

    public IsoGaussian makeCopy() {
        return new IsoGaussian(t, x, y, magnitude, xSigma, ySigma, fit, link, iD);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IsoGaussian other = (IsoGaussian) obj;
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
            return false;
        }
        if (Double.doubleToLongBits(this.magnitude) != Double.doubleToLongBits(other.magnitude)) {
            return false;
        }
        if (Double.doubleToLongBits(this.xSigma) != Double.doubleToLongBits(other.xSigma)) {
            return false;
        }
        if (Double.doubleToLongBits(this.ySigma) != Double.doubleToLongBits(other.ySigma)) {
            return false;
        }
        if (Double.doubleToLongBits(this.fit) != Double.doubleToLongBits(other.fit)) {
            return false;
        }
        return true;
    }
}
