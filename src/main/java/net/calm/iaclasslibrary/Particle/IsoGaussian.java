package net.calm.iaclasslibrary.Particle;

/**
 * Representation of a 2D IsoGaussian curve.
 *
 * @author David J Barry
 * @version 1.0, JAN 2011
 */
public class IsoGaussian extends Particle {

    protected double xSigma, ySigma, fit;

    public IsoGaussian(int t, IsoGaussian particle) {
        this(t, particle.getX(), particle.getY(), particle.getMagnitude(), particle.getXSigma(), particle.getYSigma(), particle.getFit(), particle.getLink(), -1, particle.getColocalisedParticle());
    }

    public IsoGaussian() {
        this(0.0, 0.0, 0.0, 0.0);
    }

    public IsoGaussian(double x0, double y0, double a, double sig) {
        this(x0, y0, a, sig, sig, 0.0);
    }

    public IsoGaussian(double x0, double y0, double a, double xsig, double ysig, double fit) {
        this(0, x0, y0, a, xsig, ysig, fit, null, -1, null);
    }

    public IsoGaussian(int t, double x0, double y0, double a, double xsig, double ysig, double fit, Particle link, int iD, Particle colocalisedParticle) {
        super(t, x0, y0, a, link, colocalisedParticle, iD, null, xsig, fit);
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
        return new IsoGaussian(t, x, y, magnitude, xSigma, ySigma, fit,
                link != null ? link.makeCopy() : null, iD,
                colocalisedParticle != null ? colocalisedParticle.makeCopy() : null);
    }
}
