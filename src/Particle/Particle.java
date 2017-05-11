package Particle;

/**
 * Represents a detected particle in an individual image or frame.
 *
 * @author David J Barry
 * @version 2.0, FEB 2011
 */
public class Particle {

    protected int iD;
    protected int t;
    protected double x, y, magnitude;
    protected Particle link;
    protected Particle colocalisedParticle;

    public Particle() {
        this(0, 0.0, 0.0, 0.0);
    }

    public Particle(int t, double x, double y, double magnitude) {
        this(t, x, y, magnitude, null, null, -1);
    }

    public Particle(int t, double x, double y, double magnitude, Particle newLink, Particle cP, int iD) {
        this.t = t;
        this.x = x;
        this.y = y;
        this.iD = iD;
        this.magnitude = magnitude;
        this.link = newLink;
        this.colocalisedParticle = cP;
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
    public int getTimePoint() {
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
                iD);
    }

    public Particle getColocalisedParticle() {
        return colocalisedParticle;
    }

    public void setColocalisedParticle(Particle colocalisedParticle) {
        this.colocalisedParticle = colocalisedParticle;
    }

}
