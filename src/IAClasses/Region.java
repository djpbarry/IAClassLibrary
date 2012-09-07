package IAClasses;

import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.ByteProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author barry05
 */
public class Region {

    private ArrayList<Pixel> pixels = new ArrayList<Pixel>();
    private ArrayList<Pixel> seedPix = new ArrayList<Pixel>();
    private ArrayList<Pixel> borderPix = new ArrayList<Pixel>();
    private ArrayList<Pixel> expandedBorder = new ArrayList<Pixel>();
    private ArrayList<Pixel> centroids = new ArrayList<Pixel>();
    private double min = Double.MAX_VALUE, max = Double.MIN_VALUE, mean, seedMean, sigma;
    private double mfD[];
    private int index;
    private boolean edge, active;
    private Rectangle bounds;
    private int[] histogram = new int[256];

    public Region() {
    }

    public Region(int index) {
        this.index = index;
        this.bounds = null;
        edge = false;
        active = true;
        seedMean = mean = 0.0;
    }

    public void addPoint(Pixel point) {
        pixels.add(point);
    }

    public void addBorderPoint(Pixel point) {
        borderPix.add(point);
    }

    public void calcStats(ImageProcessor refImage) {
        if (refImage == null) {
            return;
        }
        Arrays.fill(histogram, 0);
        int size = pixels.size();
        int bordersize = borderPix.size();
        int i;
        double xsum = 0.0, ysum = 0.0, valSum = 0.0, varSum = 0.0, pix;
        Pixel current;
        if (size > 0) {
            for (i = 0; i < size; i++) {
                current = (Pixel) pixels.get(i);
                xsum += current.getX();
                ysum += current.getY();
                pix = refImage.getPixelValue(current.getX(), current.getY());
                if (pix < min) {
                    min = pix;
                } else if (pix > max) {
                    max = pix;
                }
                valSum += pix;
                int bin = (int) Math.floor(pix * 255.0);
                histogram[bin]++;
            }
            double precX = xsum / (pixels.size());
            double precY = ysum / (pixels.size());
            int x = (int) Math.round(precX);
            int y = (int) Math.round(precY);
            centroids.add(new Pixel(precX, precY, refImage.getPixelValue(x, y), 2));
            mean = valSum / (size);
            for (i = 0; i < size; i++) {
                current = (Pixel) pixels.get(i);
                varSum += Math.pow(mean - refImage.getPixelValue(current.getX(), current.getY()), 2);
            }
            sigma = Math.sqrt(varSum) / size;
        } else if (bordersize > 0) {
            for (i = 0; i < bordersize; i++) {
                current = (Pixel) borderPix.get(i);
                xsum += current.getX();
                ysum += current.getY();
            }
            double precX = xsum / (borderPix.size());
            double precY = ysum / (borderPix.size());
            int x = (int) Math.round(precX);
            int y = (int) Math.round(precY);
            centroids.add(new Pixel(precX, precY, refImage.getPixelValue(x, y), 2));
        }
        return;
    }

    public double getMean() {
        return mean;
    }

    public double getSigma() {
        return sigma;
    }

    public int getIndex() {
        return index;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public ArrayList<Pixel> getPixels() {
        return pixels;
    }

    public int borderContains(int x, int y) {
        int i;
        for (i = 0; i < borderPix.size(); i++) {
            Pixel p = (Pixel) borderPix.get(i);
            if ((p.getX() == x) && (p.getY() == y)) {
                return i;
            }
        }
        return -1;
    }

    public int contains(int x, int y) {
        int i;
        for (i = 0; i < pixels.size(); i++) {
            Pixel p = (Pixel) pixels.get(i);
            if ((p.getX() == x) && (p.getY() == y)) {
                return i;
            }
        }
        return -1;
    }

    public ArrayList<Pixel> getBorderPix() {
        return borderPix;
    }

    public void removeBorderPoint(int index) {
        borderPix.remove(index);
    }

    public boolean expandBorder() {
        if (expandedBorder.size() < 1) {
            return false;
        } else {
            borderPix = expandedBorder;
            expandedBorder = new ArrayList();
            return true;
        }
    }

    public void addExpandedBorderPix(Pixel p) {
        expandedBorder.add(p);
        return;
    }

    public ArrayList<Pixel> getExpandedBorder() {
        return expandedBorder;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Region other = (Region) obj;
        if (Double.doubleToLongBits(this.min) != Double.doubleToLongBits(other.min)) {
            return false;
        }
        if (Double.doubleToLongBits(this.max) != Double.doubleToLongBits(other.max)) {
            return false;
        }
        if (Double.doubleToLongBits(this.mean) != Double.doubleToLongBits(other.mean)) {
            return false;
        }
        if (Double.doubleToLongBits(this.sigma) != Double.doubleToLongBits(other.sigma)) {
            return false;
        }
        if (this.index != other.index) {
            return false;
        }
        return true;
    }

    public boolean isEdge() {
        return edge;
    }

    public void setEdge(boolean edge) {
        this.edge = edge;
    }

    public ArrayList<Pixel> getSeedPix() {
        return seedPix;
    }

    public void setSeedPix() {
        seedMean = mean;
        mean = 0.0;
        seedPix = (ArrayList) pixels.clone();
        pixels.clear();
    }

    public Rectangle getBounds() {
        Pixel pixel = (Pixel) borderPix.get(0);
        int x = pixel.getX();
        int y = pixel.getY();
        bounds = new Rectangle(x - 1, y - 1, 3, 3);
        for (int i = 1; i < borderPix.size(); i++) {
            pixel = (Pixel) borderPix.get(i);
            x = pixel.getX();
            y = pixel.getY();
            if (x < bounds.x) {
                bounds = new Rectangle(x - 1, bounds.y, bounds.width + bounds.x - x + 1, bounds.height);
            } else if (x > bounds.x + bounds.width) {
                bounds = new Rectangle(bounds.x, bounds.y, x + 1 - bounds.x, bounds.height);
            }
            if (y < bounds.y) {
                bounds = new Rectangle(bounds.x, y - 1, bounds.width, bounds.height + bounds.y - y + 1);
            } else if (y > bounds.y + bounds.height) {
                bounds = new Rectangle(bounds.x, bounds.y, bounds.width, y + 1 - bounds.y);
            }
        }
        return bounds;
    }

    public double[][] getDataArray() {
        double data[][] = new double[bounds.width + 1][bounds.height + 1];
        for (int i = 0; i < bounds.width; i++) {
            Arrays.fill(data[i], Double.NaN);
        }
        for (int i = 0; i < pixels.size(); i++) {
            Pixel pix = (Pixel) pixels.get(i);
            int x = pix.getX() - bounds.x;
            int y = pix.getY() - bounds.y;
            data[x][y] = pix.getZ();
        }
        return data;
    }

    public int[] getHistogram() {
        return histogram;
    }

    public double[] getMfD() {
        return mfD;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ArrayList<Pixel> getCentroids() {
        return centroids;
    }

    public double getSeedMean() {
        return seedMean;
    }

    public ImageProcessor getMask() {
        Rectangle roi = getBounds();
        int w = roi.width, h = roi.height;
        return getMask(w, h);
    }

    public ImageProcessor getMask(int width, int height) {
        ImageProcessor mask = new ByteProcessor(width, height);
        mask.setColor(255);
        mask.fill();
        mask.setColor(0);
        int m = borderPix.size();
        for (int i = 0; i < m; i++) {
            Pixel current = (Pixel) borderPix.get(i);
            mask.drawPixel(current.getX(), current.getY());
        }
        FloodFiller ff = new FloodFiller(mask);
        ff.fill8(width / 2, height / 2);
        return mask;
    }

    public Pixel[] getBoundarySig(double xc, double yc) {
        Wand wand = new Wand(getMask());
        wand.autoOutline((int) Math.round(xc - bounds.x), (int) Math.round(yc - bounds.y), 0.0,
                Wand.EIGHT_CONNECTED);
        int n = wand.npoints;
        return DSPProcessor.getDistanceSignal(n, xc,
                yc, wand.xpoints, wand.ypoints, 1.0);
    }

    public Pixel[] buildVelMapCol(double xc, double yc, ImageStack stack, int frame, double timeRes, double spatialRes) {
        ImageProcessor ip = stack.getProcessor(frame);
        ImageProcessor edges = ip.duplicate();
        edges.findEdges();
        Wand wand = new Wand(getMask(ip.getWidth(), ip.getHeight()));
        wand.autoOutline((int) Math.round(xc), (int) Math.round(yc), 0.0,
                Wand.EIGHT_CONNECTED);
        int n = wand.npoints;
        int[] xpoints = wand.xpoints;
        int[] ypoints = wand.ypoints;
        Pixel points[] = DSPProcessor.interpolatePoints(n, xpoints, ypoints);
        double t1 = 0, t2 = 0;
        if (frame > 1 && frame < stack.getSize()) {
            t1 = stack.getProcessor(frame + 1).getAutoThreshold();
            t2 = stack.getProcessor(frame - 1).getAutoThreshold();
        }
        for (int i = 0; i < points.length; i++) {
            int x = points[i].getX();
            int y = points[i].getY();
            double g = 0.0;
            if (frame > 1 && frame < stack.getSize()) {
                g = stack.getProcessor(frame + 1).getPixelValue(x, y) - t1
                        - stack.getProcessor(frame - 1).getPixelValue(x, y) + t2;
            }
            double delta = edges.getPixelValue(x, y);
            double z;
            if (delta == 0.0) {
                z = 0.0;
            } else {
                z = g / delta;
            }
            points[i] = new Pixel(x, y, z * spatialRes * timeRes, 1);
        }
        return points;
    }

    public Pixel[] buildStandMapCol(double xc, double yc, ImageStack stack, int frame) {
        ImageProcessor ip = stack.getProcessor(frame);
        Wand wand = new Wand(getMask(ip.getWidth(), ip.getHeight()));
        wand.autoOutline((int) Math.round(xc), (int) Math.round(yc), 0.0,
                Wand.EIGHT_CONNECTED);
        int n = wand.npoints;
        int[] xpoints = wand.xpoints;
        int[] ypoints = wand.ypoints;
        Pixel points[] = DSPProcessor.interpolatePoints(n, xpoints, ypoints);
        for (int i = 0; i < points.length; i++) {
            int x = points[i].getX();
            int y = points[i].getY();
            points[i] = new Pixel(x, y, stack.getProcessor(frame).getPixelValue(x, y), 1);
        }
        return points;
    }

    public Roi getRoi() {
        ImageProcessor mask = getMask();
        Wand wand = new Wand(mask);
        wand.autoOutline((int) Math.round(mask.getWidth() / 2), (int) Math.round(mask.getHeight() / 2), 0.0,
                Wand.EIGHT_CONNECTED);
        PolygonRoi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON);
        roi.setLocation(bounds.x + 1, bounds.y + 1);

        return roi;
    }

    public void clearPixels() {
        pixels.clear();
    }

    public void clearBorderPix() {
        borderPix.clear();
    }
}
