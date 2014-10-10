package IAClasses;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.Straightener;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
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
    private int imageWidth, imageHeight;
//    private final int memSize = 10;

    public Region(ImageProcessor mask, Pixel centre) {
        this.imageWidth = mask.getWidth();
        this.imageHeight = mask.getHeight();
        if (centre != null) {
            this.centres.add(centre);
        }
        this.newBounds(centre);
        Pixel[] bp = this.getOrderedBoundary(imageWidth, imageHeight, mask, centre);
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

    public Region(int width, int height, Pixel centre) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.centres.add(centre);
        this.newBounds(centre);
        this.addBorderPoint(centre);
        edge = false;
        active = true;
        seedMean = mean = 0.0;
    }

    void newBounds(Pixel point) {
        if (point != null) {
            bounds = new Rectangle(point.getX(), point.getY(), 1, 1);
        }
    }
//    public void addPoint(Pixel point) {
//        pixels.add(point);
//    }

    public void addBorderPoint(Pixel point) {
        borderPix.add(point);
        updateBounds(point);
    }

    public void calcStats(ImageProcessor refImage) {
        if (refImage == null) {
            return;
        }
        Arrays.fill(histogram, 0);
//        int size = pixels.size();
        ImageProcessor mask = drawMask(imageWidth, imageHeight);
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

//    private boolean calcCentroid(LinkedList<Pixel> borderPix) {
//        int bordersize = borderPix.size();
//        double xsum = 0.0, ysum = 0.0;
//        Pixel current;
//        for (int i = 0; i < bordersize; i++) {
//            current = (Pixel) borderPix.get(i);
//            xsum += current.getX();
//            ysum += current.getY();
//        }
//        PolygonRoi proi = getPolygonRoi();
//        double x = xsum / (borderPix.size());
//        double y = ysum / (borderPix.size());
//        if (proi.contains((int) Math.round(x), (int) Math.round(y))) {
//            centres.add(new Pixel(x, y, 0.0, 1));
//            return true;
//        } else {
//            return false;
//        }
//    }
    public void calcCentroid(ImageProcessor mask) {
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
//        double x = xsum / count;
//        double y = ysum / count;
//        if (mask.getPixel((int) Math.round(x), (int) Math.round(y)) < Region.BACKGROUND) {
        centres.add(new Pixel(xsum / count, ysum / count, 0.0, 1));
//            return true;
//        } else {
//            return false;
//        }
    }

//    private boolean calcGeoMedian(ImageProcessor mask) {
//        ArrayList<Pixel> pixels = new ArrayList();
//        int height = mask.getHeight();
//        int width = mask.getWidth();
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                if (mask.getPixel(x, y) < BACKGROUND) {
//                    pixels.add(new Pixel(x, y, 0));
//                }
//            }
//        }
//        double minDist = Double.MAX_VALUE;
//        int xm = -1;
//        int ym = -1;
//        int size = pixels.size();
//        for (int p1 = 0; p1 < size; p1++) {
//            double dist = 0.0;
//            for (int p2 = 0; p2 < size; p2++) {
//                Pixel pix1 = pixels.get(p1);
//                Pixel pix2 = pixels.get(p2);
//                dist += Math.abs(pix2.getX() - pix1.getX()) + Math.abs(pix2.getY() - pix1.getY());
//            }
//            if (dist < minDist) {
//                minDist = dist;
//                xm = pixels.get(p1).getX();
//                ym = pixels.get(p1).getY();
//            }
//        }
//        if (xm >= 0 && ym >= 0) {
//            centres.add(new Pixel(xm, ym, 0.0, 1));
//            return true;
//        } else {
//            return false;
//        }
//    }
//    private boolean calcGeoMedian(LinkedList<Pixel> borderPix) {
//        int bordersize = borderPix.size();
//        if (bordersize < 3) {
//            Pixel pix = borderPix.get(0);
//            centres.add(new Pixel(pix.getX(), pix.getY(), 0.0, 1));
//            return true;
//        }
//        double minDist = Double.MAX_VALUE;
//        int xm = -1;
//        int ym = -1;
//        bounds = getBounds();
//        PolygonRoi proi = getPolygonRoi();
//        for (int y = bounds.y; y < bounds.height + bounds.y; y++) {
//            for (int x = bounds.x; x < bounds.width + bounds.x; x++) {
//                double dist = 0.0;
//                for (int b = 0; b < bordersize; b++) {
//                    Pixel pix = borderPix.get(b);
//                    dist += Math.abs(pix.getX() - x) + Math.abs(pix.getY() - y);
//                }
//                if (dist < minDist && proi.contains(x, y)) {
//                    minDist = dist;
//                    xm = x;
//                    ym = y;
//                }
//            }
//        }
//        if (xm >= 0 && ym >= 0) {
//            centres.add(new Pixel(xm, ym, 0.0, 1));
//            return true;
//        } else {
//            return false;
//        }
//    }
    PolygonRoi getPolygonRoi() {
        int bordersize = borderPix.size();
        ByteProcessor tempImage = new ByteProcessor(imageWidth, imageHeight);
        tempImage.setValue(BACKGROUND);
        tempImage.fill();
        tempImage.setValue(FOREGROUND);
//        Polygon poly = new Polygon();
        for (int i = 0; i < bordersize; i++) {
            tempImage.drawPixel(borderPix.get(i).getX(), borderPix.get(i).getY());
//            poly.addPoint(borderPix.get(i).getX(), borderPix.get(i).getY());
        }
        fill(tempImage, FOREGROUND, BACKGROUND);
//        IJ.saveAs((new ImagePlus("", tempImage)), "PNG", "C:/users/barry05/desktop/tempImage.png");
        Wand wand = new Wand(tempImage);
        wand.autoOutline(borderPix.get(0).getX(), borderPix.get(0).getY(), FOREGROUND, FOREGROUND);
        int xpix[] = new int[wand.npoints];
        int ypix[] = new int[wand.npoints];
        System.arraycopy(wand.xpoints, 0, xpix, 0, wand.npoints);
        System.arraycopy(wand.ypoints, 0, ypix, 0, wand.npoints);
        return new PolygonRoi(xpix, ypix, wand.npoints, Roi.POLYGON);
    }

    /**
     * Binary fill by Gabriel Landini, G.Landini at bham.ac.uk. Copied from
     * ij.plugin.filter.Binary
     *
     * @param ip
     * @param foreground
     * @param background
     */
    void fill(ImageProcessor ip, int foreground, int background) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloodFiller ff = new FloodFiller(ip);
        ip.setColor(127);
        for (int y = 0; y < height; y++) {
            if (ip.getPixel(0, y) == background) {
                ff.fill(0, y);
            }
            if (ip.getPixel(width - 1, y) == background) {
                ff.fill(width - 1, y);
            }
        }
        for (int x = 0; x < width; x++) {
            if (ip.getPixel(x, 0) == background) {
                ff.fill(x, 0);
            }
            if (ip.getPixel(x, height - 1) == background) {
                ff.fill(x, height - 1);
            }
        }
        byte[] pixels = (byte[]) ip.getPixels();
        int n = width * height;
        for (int i = 0; i < n; i++) {
            if (pixels[i] == 127) {
                pixels[i] = (byte) background;
            } else {
                pixels[i] = (byte) foreground;
            }
        }
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
        updateBounds(p);
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
        return bounds;
    }

    void updateBounds(Pixel pixel) {
        if (pixel == null) {
            return;
        }
        int x = pixel.getX();
        int y = pixel.getY();
        if (x < bounds.x) {
            bounds.x = x - 1;
        } else if (x > bounds.x + bounds.width) {
            bounds.width = x + 1 - bounds.x;
        }
        if (y < bounds.y) {
            bounds.y = y - 1;
        } else if (y > bounds.y + bounds.height) {
            bounds.height = y + 1 - bounds.y;
        }
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

//    public ImageProcessor getMask() {
//        Rectangle roi = getBounds();
//        int w = roi.width, h = roi.height;
//        return getMask(w, h);
//    }
    ImageProcessor drawMask(int width, int height) {
        ImageProcessor mask = new ByteProcessor(width, height);
        mask.setColor(BACKGROUND);
        mask.fill();
        mask.setColor(FOREGROUND);
        int m = borderPix.size();
        for (int i = 0; i < m; i++) {
            Pixel current = (Pixel) borderPix.get(i);
            mask.drawPixel(current.getX(), current.getY());
        }
        fill(mask, FOREGROUND, BACKGROUND);
        return mask;
    }

    public ImageProcessor getMask() {
        return drawMask(imageWidth, imageHeight);
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
                if (Math.abs(theta1 - theta2) >= 180.0) {
                    if (theta2 > theta1) {
                        theta2 -= 360.0;
                    } else {
                        theta1 -= 360.0;
                    }
                }
                curvature[j] = theta1 - theta2;
            }
        }
        return curvature;
    }

//    public Pixel[] getBoundarySig(double xc, double yc) {
//        Wand wand = getWand(getMask(), xc, yc);
//        int n = wand.npoints;
//        return DSPProcessor.getDistanceSignal(n, xc,
//                yc, wand.xpoints, wand.ypoints, 1.0);
//    }
//    public Wand getWand(ImageProcessor mask, double xc, double yc) {
//        Wand wand = new Wand(getMask());
//        wand.autoOutline((int) Math.round(xc - bounds.x), (int) Math.round(yc - bounds.y), 0.0,
//                Wand.EIGHT_CONNECTED);
//        return wand;
//    }
//    public Pixel[] getOrderedBoundary(int width, int height, double xc, double yc) {
//        return getOrderedBoundary(width, height, getMask(width, height));
//    }
    public Pixel[] getOrderedBoundary(int width, int height, ImageProcessor mask, Pixel centre) {
        if (centre == null) {
            Pixel seed = findSeed(mask);
            if (seed == null) {
                return null;
            } else {
                centre = seed;
            }
        }
//        if (centres.size() < 1) {
//            calcCentre(mask);
//        }
//        ArrayList<Pixel> centres = getCentres();
//        Pixel seed = centres.get(centres.size() - 1);
        Wand wand = new Wand(mask);
        wand.autoOutline(centre.getX(), centre.getY(), 0.0, Wand.EIGHT_CONNECTED);
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
        Pixel points[] = getOrderedBoundary(ip.getWidth(), ip.getHeight(),
                drawMask(ip.getWidth(), ip.getHeight()), new Pixel(xc, yc, 0.0));
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

    public ImageProcessor buildVelImage(ImageStack stack, int frame, double timeRes, double spatialRes, int[] thresholds) {
        ImageProcessor ip = stack.getProcessor(frame);
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloatProcessor output = new FloatProcessor(width, height);
        ImageProcessor ipm1 = null, ipp1 = null;
        ImageProcessor edges = ip.duplicate();
        edges.findEdges();
        int size = stack.getSize();
        double t1 = 0, t2 = 0;
        if (frame > 1 && frame < size) {
            ipm1 = stack.getProcessor(frame - 1);
            ipp1 = stack.getProcessor(frame + 1);
        }
        if (frame > 1 && frame < size) {
            t1 = thresholds[frame - 2];
            t2 = thresholds[frame];
        }
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double g = 0.0;
                if (frame > 1 && frame < size) {
                    g = ipp1.getPixelValue(i, j) - t1 - ipm1.getPixelValue(i, j) + t2;
                }
                double delta = edges.getPixelValue(i, j);
                double z;
                if (delta == 0.0) {
                    z = 0.0;
                } else {
                    z = g / delta;
                }
                output.putPixelValue(i, j, z * spatialRes * timeRes);
            }
        }
        return output;
    }

    public Pixel[] buildMapCol(ImageProcessor ip, int finalWidth, int depth) {
        if (depth < 3) {
            depth = 3;
        }
        PolygonRoi proi = getPolygonRoi();
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
            points[x] = new Pixel(0, 0, sum / depth, 1);
        }
        return points;
    }

//    public Roi getRoi() {
//        ImageProcessor mask = getMask();
//        Wand wand = new Wand(mask);
//        wand.autoOutline((int) Math.round(mask.getWidth() / 2), (int) Math.round(mask.getHeight() / 2), 0.0,
//                Wand.EIGHT_CONNECTED);
//        PolygonRoi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON);
//        roi.setLocation(bounds.x + 1, bounds.y + 1);
//
//        return roi;
//    }
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

//    public boolean calcCentre(ImageProcessor mask) {
//        if (calcCentroid(mask)) {
//            nudgeCentreFromEdge(mask);
//            return true;
//        }
//        } else if (calcGeoMedian(mask)) {
//            nudgeCentreFromEdge(mask);
//            return true;
//        }
//        return false;
//    }
//    void nudgeCentreFromEdge(ImageProcessor mask) {
//        PolygonRoi proi = getPolygonRoi();
//        Pixel centre = centres.get(centres.size() - 1);
//        int x = centre.getX();
//        int y = centre.getY();
//        double xsum = 0.0;
//        double ysum = 0.0;
////        Rectangle box = proi.getBounds();
////        ByteProcessor proiImage = new ByteProcessor(130, 144);
////        proiImage.setValue(255);
////        proiImage.fill();
////        proiImage.setValue(0);
////        int N = proi.getNCoordinates();
////        int proix[] = proi.getXCoordinates();
////        int proiy[] = proi.getYCoordinates();
////        for (int n = 0; n < N; n++) {
////            proiImage.drawPixel(proix[n] + box.x, proiy[n] + box.y);
////        }
////        IJ.saveAs((new ImagePlus("", proiImage)), "PNG", "C:/users/barry05/desktop/proiImage.png");
//        for (int j = y - 1; j <= y + 1; j++) {
//            for (int i = x - 1; i <= x + 1; i++) {
//                if (mask != null) {
//                    if (mask.getPixel(i, j) < BACKGROUND) {
//                        xsum += i - x;
//                        ysum += j - y;
//                    }
//                } else {
//                    if (proi.contains(i, j)) {
//                        xsum += i - x;
//                        ysum += j - y;
//                    }
//                }
//            }
//        }
//        int xDiff = 0, yDiff = 0;
//        if (xsum < 0.0) {
//            xDiff--;
//        } else if (xsum > 0.0) {
//            xDiff++;
//        }
//        if (ysum < 0.0) {
//            yDiff--;
//        } else if (ysum > 0.0) {
//            yDiff++;
//        }
//        centres.add(new Pixel(x + xDiff, y + yDiff, 0.0, 1));
//    }
//    public boolean calcCentre(LinkedList<Pixel> border) {
//        if (calcCentroid(border)) {
//            nudgeCentreFromEdge(null);
//            return true;
//        } else if (calcGeoMedian(border)) {
//            nudgeCentreFromEdge(null);
//            return true;
//        }
//        return false;
//    }
    public ArrayList<Pixel> getCentres() {
        return centres;
    }

    public Pixel findSeed(ImageProcessor input) {
        input.setRoi(bounds);
        ImageProcessor mask = input.crop();
        mask.invert();
        EDM edm = new EDM();
        edm.toEDM(mask);
        int[] max = Utils.findImageMaxima(mask);
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/adapt_test_data/masks/edm.png");
        if (!(max[0] < 0.0 || max[1] < 0.0)) {
            return new Pixel(max[0] + bounds.x, max[1] + bounds.y);
        } else {
            return null;
        }
    }
}
