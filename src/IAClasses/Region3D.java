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

    private byte[][][] maskStack;
    private int imageDepth;
    private int[] maxLengths;

    public Region3D() {
        super();
    }

    public Region3D(ImageStack mask, short[] centre) {
        super();
        initialise(centre, getMaskArray(mask));
    }

    public Region3D(byte[][][] maskStack, short[] centre) {
        super();
        initialise(centre, maskStack);
    }

    final void initialise(short[] centre, byte[][][] maskStack) {
        this.active = true;
        this.imageWidth = maskStack[0].length;
        this.imageHeight = maskStack[0][0].length;
        this.imageDepth = maskStack.length;
        if (centre != null) {
            this.centres.add(new float[]{centre[0], centre[1], centre[2]});
        }
        this.maskStack = maskStack;
        this.newBounds(centre);
        updateBoundary(this.imageWidth, this.imageHeight, this.maskStack, centre);
    }

    final void updateBoundary(int imageWidth, int imageHeight, byte[][][] maskStack, short[] centre) {
        short[][] bp = getOrderedBoundary(imageWidth, imageHeight, centre);
        if (bp != null) {
            for (int i = 0; i < bp.length; i++) {
                addBorderPoint(bp[i], maskStack);
            }
        }
    }

    public void addBorderPoint(short[] point, byte[][][] mask) {
        borderPix.add(point);
        updateBounds(point);
        mask[point[2]][point[0]][point[1]] = (byte) (FOREGROUND & 0xFF);
    }

    public byte[][][] getMaskStack() {
        if (maskStack == null) {
            drawMask(imageWidth, imageHeight);
        } else if (maskStack[0].length != imageWidth || maskStack[0][0].length != imageHeight) {
            return constructFullSizeMaskStack(imageWidth, imageHeight);
        }
        return duplicateMask();
    }

    public final byte[][][] getMaskArray(ImageStack stack) {
        int width = stack.getWidth();
        int height = stack.getHeight();
        int depth = stack.getSize();
        byte[][][] output = new byte[depth][width][height];
        for (int z = 0; z < depth; z++) {
            ImageProcessor slice = stack.getProcessor(z + 1);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    output[z][x][y] = (byte) (slice.getPixel(x, y) & 0xFF);
                }
            }
        }
        return output;
    }

    public ImageStack getMaskImage(byte[][][] mask) {
        if (mask == null) {
            mask = getMaskStack();
        }
        int width = mask[0].length;
        int height = mask[0][0].length;
        ImageStack output = new ImageStack(width, height);
        for (int z = 0; z < imageDepth; z++) {
            ByteProcessor slice = new ByteProcessor(width, height);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    slice.putPixel(x, y, mask[z][x][y] & 0x000000FF);
                }
            }
            output.addSlice(slice);
        }
        return output;
    }

    public byte[][][] duplicateMask() {
        int d = maskStack.length;
        int w = maskStack[0].length;
        int h = maskStack[0][0].length;
        byte[][][] copy = new byte[d][w][h];
        for (int z = 0; z < d; z++) {
            for (int x = 0; x < w; x++) {
                System.arraycopy(maskStack[z][x], 0, copy[z][x], 0, h);
            }
        }
        return copy;
    }

    void drawMask(int width, int height) {
        byte[][][] mask = new byte[imageDepth][width][height];
        for (int z = 0; z < imageDepth; z++) {
            for (int x = 0; x < width; x++) {
                Arrays.fill(mask[z][x], (byte) (BACKGROUND & 0xFF));
            }
        }
        int m = borderPix.size();
        for (int i = 0; i < m; i++) {
            short[] current = borderPix.get(i);
            mask[current[2]][current[0]][current[1]] = (byte) (FOREGROUND & 0xFF);
        }
        for (int i = 0; i < imageDepth; i++) {
            ByteProcessor slice = new ByteProcessor(width, height);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    slice.putPixel(x, y, mask[i][x][y]);
                }
            }
            fill(slice, FOREGROUND, BACKGROUND);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    mask[i][x][y] = (byte) (slice.getPixel(x, y) & 0xFF);
                }
            }
        }
        this.maskStack = mask;
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
                    if (pix[x + offset] != (byte) (BACKGROUND & 0xFF)) {
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
        if (input != null) {
            ImageStack stack = new ImageStack(input.getWidth(), input.getHeight());
            stack.addSlice(input);
            return findSeed3D(stack);
        } else {
            return findSeed3D(null);
        }
    }

    public short[] findSeed3D(ImageStack stack) {
        int bx = 0, by = 0;
        if (stack == null) {
            stack = getMaskImage(cropMask());
            bx = bounds.x;
            by = bounds.y;
        }
        ImageFloat edm = EDT.run(new ImageByte(stack), FOREGROUND, true, 0);
//        IJ.saveAs(edm.getImagePlus(), "TIF", "c:/users/barry05/adapt_debug/edm.tif");
        ArrayList<int[]> max = Utils.findLocalMaxima(1, edm.getImageStack(), 0.9 * edm.getMax(), false, false);
//        sp.invert();
        if (!(max.isEmpty())) {
            return new short[]{(short) Math.round(max.get(0)[0] + bx),
                (short) Math.round(max.get(0)[1] + by),
                (short) Math.round(max.get(0)[2])};
        } else {
            return null;
        }
    }

    @Override
    public void addExpandedBorderPix(short[] p) {
        expandedBorder.add(p);
        updateBounds(p);
        maskStack[p[2]][p[0]][p[1]] = FOREGROUND & 0xFF;
    }

    public short[][] getOrderedBoundary() {
        float[] centre = centres.get(centres.size() - 1);
        short[] scentre = new short[]{(short) Math.round(centre[0]),
            (short) Math.round(centre[1]),
            (short) Math.round(centre[2])};
        return getOrderedBoundary(imageWidth, imageHeight, scentre);
    }

    public short[][] getOrderedBoundary(int width, int height, short[] centre) {
        ImageProcessor maskSlice = getMaskImage(null).getProcessor(centre[2] + 1);
        if (centre == null || maskSlice.getPixel(centre[0], centre[1]) != FOREGROUND) {
            short[] seed = findSeed(null);
            if (seed == null) {
                return null;
            } else {
                centre = seed;
            }
        }
        short[][] boundary = getOrderedBoundary(width, height, maskSlice, centre);
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

//    public Rectangle getBounds(int zIndex) {
//        return bounds[zIndex];
//    }
    public PolygonRoi getPolygonRoi(int zIndex) {
//        (new ImagePlus("",slice)).show();
        return getPolygonRoi(getBounds(), getMaskImage(null).getProcessor(zIndex + 1));
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

    public void setFinalMask() {
        maskStack = cropMask();
    }

    public byte[][][] cropMask() {
        if (maskStack[0].length < imageWidth || maskStack[0][0].length < imageHeight) {
            return maskStack;
        }
        byte[][][] finalMask = new byte[imageDepth][bounds.width][bounds.height];
        for (int z = 0; z < imageDepth; z++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                System.arraycopy(maskStack[z][x], bounds.y, finalMask[z][x - bounds.x], 0, bounds.height);
            }
        }
        return finalMask;
    }

    byte[][][] constructFullSizeMaskStack(int width, int height) {
        byte[][][] fullSizeMask = new byte[imageDepth][imageWidth][imageHeight];
        for (int z = 0; z < imageDepth; z++) {
            for (int x = 0; x < imageWidth; x++) {
                Arrays.fill(fullSizeMask[z][x], (byte) (BACKGROUND & 0xFF));
            }
        }
        for (int z = 0; z < imageDepth; z++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                System.arraycopy(maskStack[z][x - bounds.x], 0, fullSizeMask[z][x], bounds.y, bounds.height);
            }
        }
        return fullSizeMask;
    }
}
