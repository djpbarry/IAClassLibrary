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

    public Pixel2(int iD, double x, double y, double z, int... associations) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.iD = iD;
        this.associations = associations;
        sortAssociations();
    }

    public Pixel2(int iD, double x, double y, int... associations) {
        this(iD, x, y, 0.0, associations);
    }

    public Pixel2(int iD, double x, double y) {
        this(iD, x, y, null);
    }

    public Pixel2(double x, double y) {
        this(-1, x, y);
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

    public boolean removeAssociationBySearch(int searchTerm) {
        if (associations == null) {
            return false;
        }
        int index = Arrays.binarySearch(associations, searchTerm);
        if (index < 0) {
            return false;
        }
        if (associations.length > 1) {
            removeAssociationByIndex(index);
        } else {
            associations = null;
        }
        return true;
    }

    public void removeAssociationByIndex(int index) {
        int L = associations.length;
        int[] newAssociations = new int[L - 1];
        System.arraycopy(associations, 0, newAssociations, 0, index);
        System.arraycopy(associations, index + 1, newAssociations, index, L - index - 1);
        this.associations = newAssociations;
    }

    public void addAssociation(int index) {
        if (associations != null) {
            if (Arrays.binarySearch(associations, index) >= 0) {
                return;
            }
            int L = associations.length;
            int[] newAssociations = new int[L + 1];
            System.arraycopy(associations, 0, newAssociations, 0, L);
            newAssociations[L] = index;
            this.associations = newAssociations;
        } else {
            associations = new int[]{index};
        }
        sortAssociations();
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

    final void sortAssociations() {
        if (associations != null) {
            Arrays.sort(associations);
        }
    }

}
