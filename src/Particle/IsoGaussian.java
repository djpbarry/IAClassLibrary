package Particle;

/**
 * Representation of a 2D IsoGaussian curve.
 *
 * @author David J Barry
 * @version 1.0, JAN 2011
 */
public class IsoGaussian {

    protected double x0, y0, magnitude, xSigma, ySigma, fit;

    public IsoGaussian() {
    }
    
        public IsoGaussian(double x0, double y0, double a, double sig) {
        this(x0, y0, a, sig, sig, 0.0);
    }

    public IsoGaussian(double x0, double y0, double a, double xsig, double ysig, double fit) {
        this.x0 = x0;
        this.y0 = y0;
        this.magnitude = a;
        this.xSigma = xsig;
        this.ySigma = ysig;
        this.fit = fit;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public double getXSigma() {
        return xSigma;
    }

    public double getYSigma() {
        return ySigma;
    }

    public double getX() {
        return x0;
    }

    public double getY() {
        return y0;
    }

    public double getFit() {
        return fit;
    }

    public double evaluate(double x, double y) {
        double result = magnitude * Math.exp(-(((x - this.x0) * (x - this.x0))
                + ((y - this.y0) * (y - this.y0))) / (2 * xSigma * xSigma));
        return result;
    }

    public Object clone() {
        return new IsoGaussian(x0, y0, magnitude, xSigma, ySigma, fit);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IsoGaussian other = (IsoGaussian) obj;
        if (Double.doubleToLongBits(this.x0) != Double.doubleToLongBits(other.x0)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y0) != Double.doubleToLongBits(other.y0)) {
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
