/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package IAClasses;

/**
 *
 * @author barry05
 */
public class BoundaryPixel extends Pixel implements Comparable {

    private int pos, time;

    public BoundaryPixel(int x, int y, int pos, int iD, int t) {
        super(x, y);
        this.pos = pos;
        this.iD = iD;
        this.time = t;
    }
    
    public BoundaryPixel(double x, double y, int pos, int iD, int t) {
        super(x, y);
        this.pos = pos;
        this.iD = iD;
        this.time = t;
    }

    public int getPos() {
        return pos;
    }

    public int getTime() {
        return time;
    }

    public int compareTo(Object obj) {
        if (obj == null) {
            return -Integer.MAX_VALUE;
        }
        if (getClass() != obj.getClass()) {
            return -Integer.MAX_VALUE;
        }
        final BoundaryPixel other = (BoundaryPixel) obj;
        return this.pos - other.pos;
    }
}
