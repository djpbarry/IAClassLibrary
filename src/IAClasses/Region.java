package IAClasses;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.Straightener;
import ij.process.ByteProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author barry05
 */
public class Region {

//    private ArrayList<Pixel> pixels = new ArrayList<Pixel>();
    private ArrayList<Pixel> seedPix = new ArrayList<Pixel>();
//    private ArrayList<ArrayList> pixMem = new ArrayList<ArrayList>();
//    private ArrayList<LinkedList> borderPixMem = new ArrayList<LinkedList>();
    private LinkedList<Pixel> borderPix = new LinkedList<Pixel>();
    private LinkedList<Pixel> expandedBorder = new LinkedList<Pixel>();
//    private ArrayList<Pixel> centroids = new ArrayList<Pixel>();
//    private ArrayList<Pixel> geoMedians = new ArrayList<Pixel>();
    private ArrayList<Pixel> centres = new ArrayList<Pixel>();
    private double min = Double.MAX_VALUE, max = Double.MIN_VALUE, mean, seedMean, sigma;
    private double mfD[];
    private boolean edge, active;
    private Rectangle bounds;
    private int[] histogram = new int[256];
//    private int initX, initY;
    public final static int FOREGROUND = 0, BACKGROUND = 255;
//    private final int memSize = 10;

    public Region(ImageProcessor mask, Pixel centre) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        if (centre != null) {
            this.centres.add(centre);
        }
        Pixel[] bp = this.getOrderedBoundary(width, height, mask);
        if (bp != null) {
            for (int i = 0; i < bp.length; i++) {
                this.addBorderPoint(bp[i]);
            }
        }
//        mask.erode();
//        for (int i = 0; i < width; i++) {
//            for (int j = 0; j < height; j++) {
//                if (mask.getPixel(i, j) == StaticConstants.FOREGROUND) {
//                    this.addPoint(new Pixel(i, j, index));
//                }
//            }
//        }
    }

    public Region() {
        this.bounds = null;
        edge = false;
        active = true;
        seedMean = mean = 0.0;
    }

//    public void addPoint(Pixel point) {
//        pixels.add(point);
//    }
    public void addBorderPoint(Pixel point) {
        borderPix.add(point);
    }

    public void calcStats(ImageProcessor refImage) {
        if (refImage == null) {
            return;
        }
        Arrays.fill(histogram, 0);
//        int size = pixels.size();
        ImageProcessor mask = getMask();
        int width = mask.getWidth();
        int height = mask.getHeight();
        int size = mask.getStatistics().histogram[FOREGROUND];
        double valSum = 0.0, varSum = 0.0, pix;
        if (size > 0) {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (mask.getPixel(i, j) == FOREGROUND) {
                        pix = refImage.getPixelValue(i, j);
                        if (pix < min) {
                            min = pix;
                        } else if (pix > max) {
                            max = pix;
                        }
                        valSum += pix;
                        int bin = (int) Math.floor(pix);
                        histogram[bin]++;
                    }
                }
            }
            mean = valSum / (size);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    if (mask.getPixel(i, j) == FOREGROUND) {
                        varSum += Math.pow(mean - refImage.getPixelValue(i, j), 2);
                    }
                }
            }
            sigma = Math.sqrt(varSum) / size;
        }
    }

    private boolean calcCentroid(LinkedList<Pixel> borderPix) {
        int bordersize = borderPix.size();
        double xsum = 0.0, ysum = 0.0;
        Pixel current;
        for (int i = 0; i < bordersize; i++) {
            current = (Pixel) borderPix.get(i);
            xsum += current.getX();
            ysum += current.getY();
        }
        PolygonRoi proi = getPolygonRoi();
        double x = xsum / (borderPix.size());
        double y = ysum / (borderPix.size());
        if (proi.contains((int) Math.round(x), (int) Math.round(y))) {
            centres.add(new Pixel(x, y, 0.0, 1));
            return true;
        } else {
            return false;
        }
    }

    private boolean calcCentroid(ImageProcessor mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        int count = 0;
        double xsum = 0.0, ysum = 0.0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (mask.getPixel(i, j) < Region.BACKGROUND) {
                    xsum += i;
                    ysum += j;
                    count++;
                }
            }
        }
        double x = xsum / count;
        double y = ysum / count;
        if (mask.getPixel((int) Math.round(x), (int) Math.round(y)) < Region.BACKGROUND) {
            centres.add(new Pixel(xsum / count, ysum / count, 0.0, 1));
            return true;
        } else {
            return false;
        }
    }

    private boolean calcGeoMedian(ImageProcessor mask) {
        ArrayList<Pixel> pixels = new ArrayList();
        int height = mask.getHeight();
        int width = mask.getWidth();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask.getPixel(x, y) < BACKGROUND) {
                    pixels.add(new Pixel(x, y, 0));
                }
            }
        }
        double minDist = Double.MAX_VALUE;
        int xm = -1;
        int ym = -1;
        int size = pixels.size();
        for (int p1 = 0; p1 < size; p1++) {
            double dist = 0.0;
            for (int p2 = 0; p2 < size; p2++) {
                Pixel pix1 = pixels.get(p1);
                Pixel pix2 = pixels.get(p2);
                dist += Math.abs(pix2.getX() - pix1.getX()) + Math.abs(pix2.getY() - pix1.getY());
            }
            if (dist < minDist) {
                minDist = dist;
                xm = pixels.get(p1).getX();
                ym = pixels.get(p1).getY();
            }
        }
        if (xm >= 0 && ym >= 0) {
            centres.add(new Pixel(xm, ym, 0.0, 1));
            return true;
        } else {
            return false;
        }
    }

    private boolean calcGeoMedian(LinkedList<Pixel> borderPix) {
        int bordersize = borderPix.size();
        if (bordersize < 3) {
            Pixel pix = borderPix.get(0);
            centres.add(new Pixel(pix.getX(), pix.getY(), 0.0, 1));
            return true;
        }
        double minDist = Double.MAX_VALUE;
        int xm = -1;
        int ym = -1;
        bounds = getBounds();
        PolygonRoi proi = getPolygonRoi();
        for (int y = bounds.y; y < bounds.height + bounds.y; y++) {
            for (int x = bounds.x; x < bounds.width + bounds.x; x++) {
                double dist = 0.0;
                for (int b = 0; b < bordersize; b++) {
                    Pixel pix = borderPix.get(b);
                    dist += Math.abs(pix.getX() - x) + Math.abs(pix.getY() - y);
                }
                if (dist < minDist && proi.contains(x, y)) {
                    minDist = dist;
                    xm = x;
                    ym = y;
                }
            }
        }
        if (xm >= 0 && ym >= 0) {
            centres.add(new Pixel(xm, ym, 0.0, 1));
            return true;
        } else {
            return false;
        }
    }

    PolygonRoi getPolygonRoi() {
        int bordersize = borderPix.size();
        int xpoints[] = new int[bordersize];
        int ypoints[] = new int[bordersize];
        for (int i = 0; i < bordersize; i++) {
            xpoints[i] = borderPix.get(i).getX();
            ypoints[i] = borderPix.get(i).getY();
        }
        return new PolygonRoi(xpoints, ypoints, bordersize, Roi.POLYGON);
    }

    public double getMean() {
        return mean;
    }

    public double getSigma() {
        return sigma;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

//    public ArrayList<Pixel> getPixels() {
//        return pixels;
//    }
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

//    public int contains(int x, int y) {
//        int i;
//        for (i = 0; i < pixels.size(); i++) {
//            Pixel p = (Pixel) pixels.get(i);
//            if ((p.getX() == x) && (p.getY() == y)) {
//                return i;
//            }
//        }
//        return -1;
//    }
    public LinkedList<Pixel> getBorderPix() {
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
            expandedBorder = new LinkedList();
            return true;
        }
    }

    public void addExpandedBorderPix(Pixel p) {
        expandedBorder.add(p);
    }

    public LinkedList<Pixel> getExpandedBorder() {
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

//    public void setSeedPix() {
//        seedMean = mean;
//        mean = 0.0;
////        seedPix = (ArrayList) pixels.clone();
////        pixels.clear();
//    }
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

    public double[][] getDataArray(ImageProcessor refImage) {
        double data[][] = new double[bounds.width + 1][bounds.height + 1];
        for (int i = 0; i < bounds.width; i++) {
            Arrays.fill(data[i], Double.NaN);
        }
        for (int i = 0; i < bounds.width; i++) {
            for (int j = 0; j < bounds.height; j++) {
                data[i][j] = refImage.getPixelValue(i, j);
            }
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
        mask.setColor(BACKGROUND);
        mask.fill();
        mask.setColor(FOREGROUND);
        int m = borderPix.size();
        Pixel centre = centres.get(centres.size() - 1);
        int xc = centre.getX();
        int yc = centre.getY();
        for (int i = 0; i < m; i++) {
            Pixel current = (Pixel) borderPix.get(i);
            mask.drawPixel(current.getX(), current.getY());
        }
        FloodFiller ff = new FloodFiller(mask);
        ff.fill8(xc, yc);
        return mask;
    }

    public static double[] calcCurvature(Pixel[] pix, int step) {
        int n = pix.length;
        double curvature[] = new double[n];
        if (n < step) {
            Arrays.fill(curvature, 0.0);
        } else {
            for (int j = 0; j < n; j++) {
                int i = j - step;
                int k = j + step;
                if (i < 0) {
                    i += n;
                }
                if (k >= n) {
                    k -= n;
                }
                double theta1 = Utils.arcTan(pix[j].getX() - pix[i].getX(), pix[j].getY() - pix[i].getY());
                double theta2 = Utils.arcTan(pix[k].getX() - pix[j].getX(), pix[k].getY() - pix[j].getY());
                if (theta1 >= 0 && theta1 < 90 && theta2 > 270) {
                    theta2 -= 360.0;
                }
                if (theta2 >= 0 && theta2 < 90 && theta1 > 270) {
                    theta1 -= 360.0;
                }
                double C = theta2 - theta1;
                curvature[j] = -C;
            }
        }
        return curvature;
    }

    public Pixel[] getBoundarySig(double xc, double yc) {
        Wand wand = getWand(getMask(), xc, yc);
        int n = wand.npoints;
        return DSPProcessor.getDistanceSignal(n, xc,
                yc, wand.xpoints, wand.ypoints, 1.0);
    }

    public Wand getWand(ImageProcessor mask, double xc, double yc) {
        Wand wand = new Wand(getMask());
        wand.autoOutline((int) Math.round(xc - bounds.x), (int) Math.round(yc - bounds.y), 0.0,
                Wand.EIGHT_CONNECTED);
        return wand;
    }

    public Pixel[] getOrderedBoundary(int width, int height, double xc, double yc) {
        return getOrderedBoundary(width, height, getMask(width, height));
    }

    public Pixel[] getOrderedBoundary(int width, int height, ImageProcessor mask) {
        if (centres.size() < 1) {
            calcCentre(mask);
        }
        ArrayList<Pixel> centres = getCentres();
        Pixel seed = centres.get(centres.size() - 1);
        Wand wand = new Wand(mask);
        wand.autoOutline(seed.getX(), seed.getY(), 0.0, Wand.EIGHT_CONNECTED);
        int n = wand.npoints;
        int[] xpoints = wand.xpoints;
        int[] ypoints = wand.ypoints;
        return DSPProcessor.interpolatePoints(n, xpoints, ypoints);
    }

    public Pixel[] buildVelMapCol(double xc, double yc, ImageStack stack, int frame, double timeRes, double spatialRes, int[] thresholds) {
        ImageProcessor ip = stack.getProcessor(frame);
        ImageProcessor ipm1 = null, ipp1 = null;
        ImageProcessor edges = ip.duplicate();
        edges.findEdges();
        int size = stack.getSize();
        Pixel points[] = getOrderedBoundary(ip.getWidth(), ip.getHeight(), xc, yc);
        double t1 = 0, t2 = 0;
        if (frame > 1 && frame < size) {
            ipm1 = stack.getProcessor(frame - 1);
            ipp1 = stack.getProcessor(frame + 1);
        }
        if (frame > 1 && frame < size) {
            t1 = thresholds[frame - 2];
            t2 = thresholds[frame];
        }
        for (int i = 0; i < points.length; i++) {
            int x = points[i].getX();
            int y = points[i].getY();
            double g = 0.0;
            if (frame > 1 && frame < size) {
                g = ipp1.getPixelValue(x, y) - t1 - ipm1.getPixelValue(x, y) + t2;
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

    public Pixel[] buildStandMapCol(double xc, double yc, ImageStack stack, int frame, int finalWidth, int depth) {
        ImageProcessor ip = stack.getProcessor(frame);
        Wand wand = new Wand(getMask(ip.getWidth(), ip.getHeight()));
        wand.autoOutline((int) Math.round(xc), (int) Math.round(yc), 0.0,
                Wand.EIGHT_CONNECTED);
        int n = wand.npoints;
        int[] xpoints = wand.xpoints;
        int[] ypoints = wand.ypoints;
        PolygonRoi proi = new PolygonRoi(xpoints, ypoints, n, Roi.POLYLINE);
        Straightener straightener = new Straightener();
        ImagePlus sigImp = new ImagePlus("", ip);
        sigImp.setRoi(proi);
        ImageProcessor sig = straightener.straighten(sigImp, proi, depth);
        sig.setInterpolate(true);
        sig.setInterpolationMethod(ImageProcessor.BILINEAR);
        ImageProcessor sig2 = sig.resize(finalWidth, depth);
        Pixel points[] = new Pixel[finalWidth];
        for (int x = 0; x < finalWidth; x++) {
            double sum = 0.0;
            for (int y = 0; y < sig2.getHeight(); y++) {
                sum += sig2.getPixelValue(x, y);
            }
            points[x] = new Pixel(0, 0, sum, 1);
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

//    public void clearPixels() {
//        pixels.clear();
//    }
    public void clearBorderPix() {
        borderPix.clear();
    }

//    public void savePixels(int width, int height) {
//        pixMem.add((ArrayList) pixels.clone());
//        borderPixMem.add((LinkedList) borderPix.clone());
//        if (pixMem.size() > memSize) {
//            pixMem.remove(0);
//        }
//        if (borderPixMem.size() > memSize) {
//            borderPixMem.remove(0);
//        }
//        Random r = new Random();
//        ColorProcessor cp = new ColorProcessor(width, height);
//        for (int i = pixMem.size() - 1; i >= 0; i--) {
//            ArrayList<Pixel> pix = pixMem.get(i);
//            cp.setColor(new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256)));
//            for (int j = 0; j < pix.size(); j++) {
//                int x = pix.get(j).getX();
//                int y = pix.get(j).getY();
//                cp.drawPixel(x, y);
//            }
//        }
//        (new ImagePlus("Pix", cp)).show();
//    }
//    public ArrayList<Pixel> getSavedPix(int index) {
//        if (index >= pixMem.size()) {
//            return null;
//        }
//        return pixMem.get(index);
//    }
//
//    public LinkedList<Pixel> getSavedBorderPix(int index) {
//        if (index >= borderPixMem.size()) {
//            return null;
//        }
//        return borderPixMem.get(index);
//    }
    public void loadPixels(LinkedList<Pixel> borderPix) {
//        this.pixels = (ArrayList) pixels.clone();
        this.borderPix = (LinkedList) borderPix.clone();
//        setSeedPix();
    }

    public boolean calcCentre(ImageProcessor mask) {
        if (calcCentroid(mask)) {
            return true;
        } else if (calcGeoMedian(mask)) {
            return true;
        }
        return false;
    }

    public boolean calcCentre(LinkedList<Pixel> border) {
        if (calcCentroid(border)) {
            return true;
        } else if (calcGeoMedian(border)) {
            return true;
        }
        return false;
    }

    public ArrayList<Pixel> getCentres() {
        return centres;
    }

}
