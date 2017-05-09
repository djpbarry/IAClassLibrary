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
        t = 0;
        x = 0.0;
        y = 0.0;
        iD = -1;
        link = null;
    }

    /**
     * Creates a new Particle
     *
     * @param time position within an image stack
     * @param newLink the last <code>Particle</code> in the current
     * <code>ParticleTrajectory</code> to which this particle should be linked.
     * Set to <code>null</code> if this is the first particle in a new
     * trajectory.
     */
    public Particle(int time, Particle newLink, int iD, double magnitude) {
        this.iD = iD;
        this.t = time;
        this.link = newLink;
        this.magnitude = magnitude;
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
        return new Particle(t, link, iD, magnitude);
    }

    public Particle getColocalisedParticle() {
        return colocalisedParticle;
    }

    public void setColocalisedParticle(Particle colocalisedParticle) {
        this.colocalisedParticle = colocalisedParticle;
    }
    
    
}
