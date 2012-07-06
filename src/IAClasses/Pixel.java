package IAClasses;

/**
 *
 * @author barry05
 */
public class Pixel {

    private int x, y, associations;
    private double z;
    private double precX, precY;
    private boolean fixed;

    public Pixel(int x, int y, double z, int associations) {
        this.precX = Double.NaN;
        this.precY = Double.NaN;
        this.x = x;
        this.y = y;
        this.z = z;
        fixed = false;
        this.associations = associations;
    }

    public Pixel(double x, double y, double z, int associations) {
        this.precX = x;
        this.precY = y;
        this.x = (int) Math.round(x);
        this.y = (int) Math.round(y);
        this.z = z;
        fixed = false;
        this.associations = associations;
    }
    
    public Pixel(double x, double y, double z) {
        this.precX = x;
        this.precY = y;
        this.x = (int) Math.round(x);
        this.y = (int) Math.round(y);
        this.z = z;
        fixed = false;
        this.associations = 0;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getAssociations() {
        return associations;
    }

    public void decAssociations() {
        associations--;
    }

    public double getPrecX() {
        return precX;
    }

    public double getPrecY() {
        return precY;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }
    
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pixel other = (Pixel) obj;
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        if (this.associations != other.associations) {
            return false;
        }
        if (Double.doubleToLongBits(this.z) != Double.doubleToLongBits(other.z)) {
            return false;
        }
        if (Double.doubleToLongBits(this.precX) != Double.doubleToLongBits(other.precX)) {
            return false;
        }
        if (Double.doubleToLongBits(this.precY) != Double.doubleToLongBits(other.precY)) {
            return false;
        }
        if (this.fixed != other.fixed) {
            return false;
        }
        return true;
    }
}
