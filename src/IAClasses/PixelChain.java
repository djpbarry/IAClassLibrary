/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package IAClasses;

/**
 *
 * @author barry05
 */
public class PixelChain {

    private Pixel end;
    private int size;

    public PixelChain() {
        end = null;
        size = 0;
    }

//    public boolean addPixel(int x, int y, int z, int iD) {
//        end = new Pixel(x, y, z, iD, end);
//        if (end == null) {
//            return false;
//        }
//        size++;
//        return true;
//    }
    
    public boolean addPixel(Pixel newPixel) {
        end = newPixel;
        if (end == null) {
            return false;
        }
        size++;
        return true;
    }

    public boolean contains(Pixel pixel) {
        Pixel current = end;
        while (current != null) {
            if (current.equals(pixel)) {
                return true;
            }
            current = current.getLink();
        }
        return false;
    }

    public Pixel getEnd() {
        return end;
    }

    public int getSize() {
        return size;
    }
}
