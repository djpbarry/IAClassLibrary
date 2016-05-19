/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IAClasses;

import static IAClasses.Region.BACKGROUND;
import static IAClasses.Region.FOREGROUND;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.distanceMap3d.EDT;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class Region3D extends Region {

    private ImageStack maskStack;
    private Rectangle[] bounds;
    private int imageDepth;
    private Object[] maskPix;
    private int[] maxLengths;

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
            this.centres.add(new float[]{centre[0], centre[1], centre[2]});
        }
        this.maskStack = maskStack;
        this.maskPix = this.maskStack.getImageArray();
        this.bounds = new Rectangle[imageDepth];
        this.newBounds(centre);
//        this.addBorderPoint(centre, maskStack.getProcessor(centre[2] + 1));
        updateBoundary(this.imageWidth, this.imageHeight, this.maskStack, centre);
    }

    final void updateBoundary(int imageWidth, int imageHeight, ImageStack maskStack, short[] centre) {
        short[][] bp = getOrderedBoundary(imageWidth, imageHeight, maskStack, centre);
        if (bp != null) {
            for (int i = 0; i < bp.length; i++) {
                addBorderPoint(bp[i], maskStack);
            }
        }
    }

    public void addBorderPoint(short[] point, ImageStack mask) {
        borderPix.add(point);
        updateBounds(point);
        mask.getProcessor(point[2] + 1).drawPixel(point[0], point[1]);
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

    public short[] findSeed(ImageProcessor input) {
        ImageStack stack = new ImageStack(input.getWidth(), input.getHeight());
        stack.addSlice(input);
        return findSeed(stack);
    }

    public short[] findSeed(ImageStack input) {
//        if (input.getSize() < 2) {
//            short[] seed = findSeed(input.getProcessor(1));
//            return new short[]{seed[0], seed[1], 0};
//        }
//        int bx = 0, by = 0;
//        if (bounds != null) {
//            bounds = checkBounds(bounds);
//            input.setRoi(bounds);
//            bx = bounds.x;
//            by = bounds.y;
//        }
//        ImageProcessor mask = input.crop();
//        mask.invert();
//        StackProcessor sp = new StackProcessor(input);
//        sp.invert();
        ImageFloat edm = EDT.run(new ImageByte(input), FOREGROUND, true, 0);
//        IJ.saveAs(edm.getImagePlus(), "TIF", "c:/users/barry05/adapt_debug/edm.tif");
        ArrayList<int[]> max = Utils.findLocalMaxima(1, edm.getImageStack(), 0.9 * edm.getMax(), false, false);
//        sp.invert();
        if (!(max.isEmpty())) {
            return new short[]{(short) Math.round(max.get(0)[0]), (short) Math.round(max.get(0)[1]),
                (short) Math.round(max.get(0)[2])};
        } else {
            return null;
        }
    }

    @Override
    public void addExpandedBorderPix(short[] p) {
        expandedBorder.add(p);
        updateBounds(p);
        ((byte[]) maskPix[p[2]])[p[0] + p[1] * imageWidth] = FOREGROUND;
    }

    public short[][] getOrderedBoundary() {
        float[] centre = centres.get(centres.size() - 1);
        short[] scentre = new short[]{(short) Math.round(centre[0]),
            (short) Math.round(centre[1]),
            (short) Math.round(centre[2])};
        return getOrderedBoundary(imageWidth, imageHeight, maskStack, scentre);
    }

    public short[][] getOrderedBoundary(int width, int height, ImageStack mask, short[] centre) {
        if (centre == null || mask.getProcessor(centre[2] + 1).getPixel(centre[0], centre[1]) != FOREGROUND) {
            short[] seed = findSeed(mask);
            if (seed == null) {
                return null;
            } else {
                centre = seed;
            }
        }
        short[][] boundary = getOrderedBoundary(width, height, mask.getProcessor(centre[2] + 1), centre);
        int l = boundary.length;
        short[][] output = new short[l][];
        for (int j = 0; j < l; j++) {
            output[j] = new short[]{boundary[j][0], boundary[j][1], centre[2]};
        }
        return output;
    }

    public int getImageDepth() {
        return imageDepth;
    }

    public int getMaxLength() {
        if (maxLengths == null) {
            sumLengths();
        }
        int max = 0;
        for (int i = 0; i < imageDepth; i++) {
            if (maxLengths[i] > max) {
                max = maxLengths[i];
            }
        }
        return max;
    }

    private void sumLengths() {
        maxLengths = new int[imageDepth];
        short[][] boundary = getOrderedBoundary();
        int l = boundary.length;
        short[][] output = new short[l][];
        Arrays.fill(maxLengths, 0, maxLengths.length - 1, 0);
        for (int j = 0; j < l; j++) {
            output[j] = new short[]{boundary[j][0], boundary[j][1], boundary[j][2]};
            maxLengths[boundary[j][2]]++;
        }
    }

    public Rectangle getBounds(int zIndex) {
        return bounds[zIndex];
    }

    public PolygonRoi getPolygonRoi(int zIndex) {
        return getPolygonRoi(getBounds(zIndex), getMaskStack().getProcessor(zIndex + 1));
    }

    public ImageStack buildVelImageStack(ImagePlus input, int frame, double timeRes, double spatialRes, int[] thresholds) {
        int width = input.getWidth();
        int height = input.getHeight();
        int nSlices = input.getNSlices();
        int nFrames = input.getNFrames();
        ImageStack inputStack = input.getImageStack();
        ImageStack output = new ImageStack(width, height);
        for (int i = 1; i <= nSlices; i++) {
            ImageStack subStack = new ImageStack(width, height);
            for (int j = 0; j < nFrames; j++) {
                subStack.addSlice(inputStack.getProcessor(j * nSlices + i));
            }
            output.addSlice(buildVelImage(subStack, frame + 1, timeRes, spatialRes, thresholds));
        }
        return output;
    }
}
