package IAClasses;

import java.awt.geom.Point2D;
import java.util.Arrays;

/**
 *
 * @author barry05
 */
public class Pixel2 extends Point2D.Double {

    protected int iD;
    private double z;
    private int[] associations;

    public Pixel2(double x, double y, double z, int iD) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.iD = iD;
    }

    public Pixel2(double x, double y, double z) {
        this(x, y, z, -1);
    }

    public Pixel2(double x, double y) {
        this(x, y, 0.0, -1);
    }

    public int getRoundedX() {
        return (int) Math.round(x);
    }

    public int getRoundedY() {
        return (int) Math.round(y);
    }

    public int getRoundedZ() {
        return (int) Math.round(z);
    }

    public double getZ() {
        return z;
    }

    public void removeAssociation(int index) {
        int location = Arrays.binarySearch(associations, index);
        int L = associations.length;
        int[] newAssociations = new int[L - 1];
        System.arraycopy(associations, 0, newAssociations, 0, location);
        System.arraycopy(associations, location + 1, newAssociations, location, L - location);
        this.associations = newAssociations;
    }

    public void addAssociation(int index) {
        int L = associations.length;
        int[] newAssociations = new int[L + 1];
        System.arraycopy(associations, 0, newAssociations, 0, L);
        newAssociations[L] = index;
        this.associations = newAssociations;
    }

    public int[] getAssociations() {
        return associations;
    }

    public int getiD() {
        return iD;
    }

    public void setiD(int iD) {
        this.iD = iD;
    }

}
