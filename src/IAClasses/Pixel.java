package IAClasses;

/**
 *
 * @author barry05
 */
public class Pixel {

    protected int x, y, iD;
    private double z;
    private double precX, precY;

    public Pixel(int x, int y, double z) {
        this.precX = Double.NaN;
        this.precY = Double.NaN;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Pixel(double x, double y, double z) {
        this.precX = x;
        this.precY = y;
        this.x = (int) Math.round(x);
        this.y = (int) Math.round(y);
        this.z = z;
    }

    public Pixel(int x, int y) {
        this.precX = x;
        this.precY = y;
        this.x = x;
        this.y = y;
        this.z = 0.0;
    }

    public Pixel(double x, double y) {
        this.precX = x;
        this.precY = y;
        this.x = (int) Math.round(x);
        this.y = (int) Math.round(y);
        this.z = 0.0;
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

    public double getPrecX() {
        return precX;
    }

    public double getPrecY() {
        return precY;
    }

    public int getID() {
        return iD;
    }

    @Override
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
        if (this.iD != other.iD) {
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
        return true;
    }
}
