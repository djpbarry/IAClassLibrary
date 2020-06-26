package IAClasses;

import java.awt.geom.Point2D;

/**
 *
 * @author barry05
 */
public class Pixel extends Point2D.Double {

//    protected int x, y, iD;
    protected int iD;
    private double z;
//    private double precX, precY;
    private int associations = 0;

//    public Pixel(int x, int y, double z) {
////        this.precX = Double.NaN;
////        this.precY = Double.NaN;
//        this.x = x;
//        this.y = y;
//        this.z = z;
//    }
    public Pixel(double x, double y, double z, int iD) {
//        this.precX = Double.NaN;
//        this.precY = Double.NaN;
        this.x = x;
        this.y = y;
        this.z = z;
        this.iD = iD;
    }

    public Pixel(double x, double y, double z) {
//        this.precX = x;
//        this.precY = y;
//        this.x = x;
//        this.y = y;
//        this.z = z;
        this(x, y, z, 0);
    }

//    public Pixel(int x, int y) {
////        this.precX = x;
////        this.precY = y;
////        this.x = x;
////        this.y = y;
////        this.z = 0.0;
//    }
    public Pixel(double x, double y) {
//        this.precX = x;
//        this.precY = y;
//        this.x = (int) Math.round(x);
//        this.y = (int) Math.round(y);
//        this.z = 0.0;
        this(x, y, 0.0, 0);
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

//    public double getX() {
//        return precX;
//    }
//
//    public double getY() {
//        return precY;
//    }
    public int getID() {
        return iD;
    }

    public void decAssociations() {
        associations--;
    }

    public int getAssociations() {
        return associations;
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        final Pixel other = (Pixel) obj;
//        if (this.x != other.x) {
//            return false;
//        }
//        if (this.y != other.y) {
//            return false;
//        }
//        if (this.iD != other.iD) {
//            return false;
//        }
//        if (Double.doubleToLongBits(this.z) != Double.doubleToLongBits(other.z)) {
//            return false;
//        }
//        if (Double.doubleToLongBits(this.precX) != Double.doubleToLongBits(other.precX)) {
//            return false;
//        }
//        if (Double.doubleToLongBits(this.precY) != Double.doubleToLongBits(other.precY)) {
//            return false;
//        }
//        return true;
//    }

    public void setNeighbouringRegionIndex(int iD) {
        this.iD = iD;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
