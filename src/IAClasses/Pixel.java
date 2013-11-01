package IAClasses;

/**
 *
 * @author barry05
 */
public class Pixel {

    protected int x, y, iD, associations;
    private double z;
    private double precX, precY;
    private boolean fixed;
    private Pixel link;

    public Pixel(int x, int y, double z, int associations) {
        this.precX = Double.NaN;
        this.precY = Double.NaN;
        this.x = x;
        this.y = y;
        this.z = z;
        fixed = false;
        this.associations = associations;
        this.link = null;
    }

    public Pixel(double x, double y, double z, int associations) {
        this.precX = x;
        this.precY = y;
        this.x = (int) Math.round(x);
        this.y = (int) Math.round(y);
        this.z = z;
        fixed = false;
        this.associations = associations;
        this.link = null;
    }

    public Pixel(double x, double y, double z) {
        this.precX = x;
        this.precY = y;
        this.x = (int) Math.round(x);
        this.y = (int) Math.round(y);
        this.z = z;
        fixed = false;
        this.associations = 0;
        this.link = null;
    }
    
    public Pixel(int x, int y) {
        this.precX = x;
        this.precY = y;
        this.x = x;
        this.y = y;
        this.z = 0.0;
        fixed = false;
        this.associations = 0;
        this.link = null;
    }
    
    public Pixel(double x, double y, double z, int iD, Pixel newLink) {
        this.precX = x;
        this.precY = y;
        this.x = (int) Math.round(x);
        this.y = (int) Math.round(y);
        this.z = z;
        fixed = false;
        this.associations = 0;
        this.link = newLink;
        this.iD = iD;
    }

    public Pixel getLink(){
        return link;
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

    public void setAssociations(int associations) {
        this.associations = associations;
    }

    public void setPrecX(double precX) {
        this.precX = precX;
    }

    public void setPrecY(double precY) {
        this.precY = precY;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
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
        return true;
    }    
}
