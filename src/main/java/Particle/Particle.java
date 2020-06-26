package Particle;

import IAClasses.Region;
import fiji.plugin.trackmate.Spot;
import ij.process.ImageProcessor;

/**
 * Represents a detected particle in an individual image or frame.
 *
 * @author David J Barry
 * @version 2.0, FEB 2011
 */
public class Particle extends Spot {

    protected int iD;
    protected int t;
    protected double x, y, magnitude;
    protected Particle link;
    protected Particle colocalisedParticle;
    protected Region region;
    public static String COLOCALISED = "Colocalised";

    public Particle() {
        this(0, 0.0, 0.0, 0.0);
    }

    public Particle(int t, double x, double y, double magnitude) {
        this(t, x, y, magnitude, null, null, -1, null, Double.NaN, Double.NaN);
    }

    public Particle(int t, double x, double y, double magnitude, Particle newLink, Particle cP, int iD, Region region, double radius, double quality) {
        super(x, y, 0, radius, quality);
        this.t = t;
        this.x = x;
        this.y = y;
        this.iD = iD;
        this.magnitude = magnitude;
        this.link = newLink;
        this.colocalisedParticle = cP;
        this.region = region;
    }

    /**
     * Returns the particle's mean x-position, calculated as the average of the
     * x-positions in the red and green channels.
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the particle's mean y-position, calculated as the average of the
     * y-positions in the red and green channels.
     */
    public double getY() {
        return y;
    }

    /**
     * This particle's z-position within an image stack.
     */
    public int getFrameNumber() {
        return t;
    }

    /**
     * Returns the <code>Particle</code> to which this particle is linked.
     */
    public Particle getLink() {
        return link;
    }

    public void setLink(Particle link) {
        this.link = link;
    }

    public int getiD() {
        return iD;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public Particle makeCopy() {
        return new Particle(t, x, y, magnitude,
                link != null ? link.makeCopy() : null,
                colocalisedParticle != null ? colocalisedParticle.makeCopy() : null,
                iD, region, this.getFeature(Spot.RADIUS), this.getFeature(Spot.QUALITY));
    }

    public Particle getColocalisedParticle() {
        return colocalisedParticle;
    }

    public void setColocalisedParticle(Particle colocalisedParticle) {
        this.colocalisedParticle = colocalisedParticle;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void refineCentroid(ImageProcessor image, int blobRadius, double res) {
        int x0 = (int) Math.round(this.x / res) - blobRadius;
        int y0 = (int) Math.round(this.y / res) - blobRadius;
        int width = blobRadius * 2;
        double sumX = 0.0, sumY = 0.0, sum = 0.0;
        for (int yc = y0; yc <= y0 + width; yc++) {
            for (int xc = x0; xc <= x0 + width; xc++) {
                double p = image.getPixelValue(xc, yc);
                sumX += p * xc;
                sumY += p * yc;
                sum += p;
            }
        }
        this.x = sumX * res / sum;
        this.y = sumY * res / sum;
    }

}
