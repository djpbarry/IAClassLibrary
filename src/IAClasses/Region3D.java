/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IAClasses;

import static IAClasses.Region.BACKGROUND;
import static IAClasses.Region.FOREGROUND;
import ij.ImageStack;
import ij.process.ByteProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.distanceMap3d.EDT;
import mcib3d.image3d.processing.MaximaFinder;

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
        this.bounds = new Rectangle[imageDepth];
        this.newBounds(centre);
        this.addBorderPoint(centre, maskStack.getProcessor(centre[2] + 1));
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

    public ImageStack getMaskStack() {
        if (maskStack == null) {
            drawMask(imageWidth, imageHeight);
        }
        return maskStack.duplicate();
    }

    void drawMask(int width, int height) {
        ImageStack maskStack = new ImageStack(width, height);
        for (int i = 0; i < imageDepth; i++) {
            ByteProcessor mask = new ByteProcessor(width, height);
            mask.setColor(BACKGROUND);
            mask.fill();
            mask.setColor(FOREGROUND);
            maskStack.addSlice(mask);
        }
        int m = borderPix.size();
        for (int i = 0; i < m; i++) {
            short[] current = borderPix.get(i);
            maskStack.getProcessor(current[2] + 1).drawPixel(current[0], current[1]);
        }
        for (int i = 1; i <= imageDepth; i++) {
            fill(maskStack.getProcessor(i), FOREGROUND, BACKGROUND);
        }
        this.maskStack = maskStack;
    }

    public void calcCentroid(ImageStack mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        int depth = mask.getSize();
        int count = 0;
        float xsum = 0.0f, ysum = 0.0f, zsum = 0.0f;
        for (int z = 0; z < depth; z++) {
            byte[] pix = (byte[]) mask.getProcessor(z + 1).getPixels();
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    if (pix[x + offset] != (byte) BACKGROUND) {
                        xsum += x;
                        ysum += y;
                        zsum += z;
                        count++;
                    }
                }
            }
        }
        centres.add(new float[]{xsum / count, ysum / count, zsum / count});
    }

    public short[] findSeed(ImageStack input) {
//        int bx = 0, by = 0;
//        if (bounds != null) {
//            bounds = checkBounds(bounds);
//            input.setRoi(bounds);
//            bx = bounds.x;
//            by = bounds.y;
//        }
//        ImageProcessor mask = input.crop();
//        mask.invert();
        ImageFloat edm = EDT.run(new ImageByte(input), BACKGROUND, false, 0);
        MaximaFinder ma = new MaximaFinder(edm, (float) (0.9 * edm.getMax()));
        ArrayList<Voxel3D> maxima = ma.getListPeaks();
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/edm.png");
        if (!(maxima.isEmpty())) {
            return new short[]{(short) Math.round(maxima.get(0).x), (short) Math.round(maxima.get(0).y)};
        } else {
            return null;
        }
    }

    }
