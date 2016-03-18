/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IAClasses;

import ij.ImageStack;
import java.awt.Rectangle;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class Region3D extends Region {

    private ImageStack maskStack;
    private Rectangle[] bounds;
    private int imageDepth;

    public Region3D() {
        super();
    }

    public Region3D(ImageStack maskStack, short[] centre) {
        super();
        this.active = true;
        this.imageWidth = maskStack.getWidth();
        this.imageHeight = maskStack.getHeight();
        this.imageDepth = maskStack.getSize();
        if (centre != null) {
            this.centres.add(new float[]{centre[2], centre[0], centre[1]});
        }
        this.maskStack = maskStack;
        this.newBounds(centre);
        this.addBorderPoint(centre, maskStack.getProcessor(centre[2]));
    }

    @Override
    final void newBounds(short[] point) {
        if (point != null) {
            bounds[point[2]] = new Rectangle(point[0], point[1], 1, 1);
        }
    }

    void updateBounds(short[] pixel) {
        if (pixel == null) {
            return;
        }
        int x = pixel[0];
        int y = pixel[1];
        int z = pixel[2];
        if (bounds[z] == null) {
            bounds[z] = new Rectangle(x - 1, y - 1, 3, 3);
            return;
        }
        if (x < bounds[z].x) {
            bounds[z].x = x - 1;
        } else if (x > bounds[z].x + bounds[z].width) {
            bounds[z].width = x + 1 - bounds[z].x;
        }
        if (y < bounds[z].y) {
            bounds[z].y = y - 1;
        } else if (y > bounds[z].y + bounds[z].height) {
            bounds[z].height = y + 1 - bounds[z].y;
        }
    }    
}
