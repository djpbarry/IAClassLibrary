package IAClasses;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.Straightener;
import ij.plugin.filter.EDM;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author barry05
 */
public class Region implements Cloneable {

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
    private ImageProcessor mask;

    public Region() {

    }

    public Region(ImageProcessor mask, Pixel centre) {
        this.imageWidth = mask.getWidth();
        this.imageHeight = mask.getHeight();
        if (centre != null) {
            this.centres.add(centre);
        }
        this.mask = mask;
        this.newBounds(centre);
        Pixel[] bp = this.getOrderedBoundary(imageWidth, imageHeight, mask, centre);
        if (bp != null) {
            for (int i = 0; i < bp.length; i++) {
                this.addBorderPoint(bp[i]);
            }
        }
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

    public void addBorderPoint(Pixel point) {
        borderPix.add(point);
        updateBounds(point);
        mask.drawPixel(point.getX(), point.getY());
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

    public void calcCentroid(ImageProcessor mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        int count = 0;
        double xsum = 0.0, ysum = 0.0;
        byte[] pix = (byte[]) mask.getPixels();
        for (int j = 0; j < height; j++) {
            int offset = j * width;
            for (int i = 0; i < width; i++) {
                if (pix[i + offset] != (byte) Region.BACKGROUND) {
                    xsum += i;
                    ysum += j;
                    count++;
                }
            }
        }
//        double x = xsum / count;
//        double y = ysum / count;
//        if (mask.getPixel((int) Math.round(x), (int) Math.round(y)) < Region.BACKGROUND) {
        centres.add(new Pixel(xsum / count, ysum / count, 0.0));
//            return true;
//        } else {
//            return false;
//        }
    }

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
        ip.setColor(foreground);
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
        mask.drawPixel(p.getX(), p.getY());
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

    public Rectangle getBounds() {
        return bounds;
    }

    void updateBounds(Pixel pixel) {
        if (pixel == null) {
            return;
        }
        int x = pixel.getX();
        int y = pixel.getY();
        if (bounds == null) {
            bounds = new Rectangle(x - 1, y - 1, 3, 3);
            return;
        }
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

    void drawMask(int width, int height) {
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
        this.mask = mask;
    }

    public ImageProcessor getMask() {
        if (mask == null) {
            drawMask(imageWidth, imageHeight);
        }
        return mask.duplicate();
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

    public Pixel[] getOrderedBoundary(int width, int height, ImageProcessor mask, Pixel centre) {
        if (centre == null) {
            Pixel seed = findSeed(mask);
            if (seed == null) {
                return null;
            } else {
                centre = seed;
            }
        }
        Wand wand = new Wand(mask);
        wand.autoOutline(centre.getX(), centre.getY(), 0.0, Wand.EIGHT_CONNECTED);
        int n = wand.npoints;
        int[] xpoints = wand.xpoints;
        int[] ypoints = wand.ypoints;
        Rectangle r = (new PolygonRoi(xpoints, ypoints, n, Roi.POLYGON)).getBounds();
        mask.setValue(BACKGROUND);
        mask.fillOutside(new Roi(r));
        mask.setValue(FOREGROUND);
//        if (interpolate) {
        return DSPProcessor.interpolatePoints(n, xpoints, ypoints);
//        } else {
//            Pixel[] pix = new Pixel[n];
//            for (int i = 0; i < n; i++) {
//                pix[i] = new Pixel(xpoints[i], ypoints[i], 1.0, 1);
//            }
//            return pix;
//        }
    }

    public Pixel[] buildVelMapCol(double xc, double yc, ImageStack stack, int frame, double timeRes, double spatialRes, int[] thresholds) {
        ImageProcessor ip = stack.getProcessor(frame);
        ImageProcessor ipm1 = null, ipp1 = null;
        ImageProcessor edges = ip.duplicate();
        edges.findEdges();
        int size = stack.getSize();
        drawMask(ip.getWidth(), ip.getHeight());
        Pixel points[] = getOrderedBoundary(ip.getWidth(), ip.getHeight(),
                mask, new Pixel(xc, yc, 0.0));
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
            points[i] = new Pixel(x, y, z * spatialRes * timeRes);
        }
        return points;
    }

    public ImageProcessor buildVelImage(ImageStack stack, int frame, double timeRes, double spatialRes, int[] thresholds) {
        ImageProcessor ip = stack.getProcessor(frame);
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloatProcessor output = new FloatProcessor(width, height);
        float outPix[] = (float[]) output.getPixels();
        short ipm1[] = null, ipp1[] = null;
        FloatProcessor edges = (FloatProcessor) (new TypeConverter(ip.duplicate(), false)).convertToFloat(null);
        edges.findEdges();
        float edgePix[] = (float[]) edges.getPixels();
        int size = stack.getSize();
        float t1 = 0, t2 = 0;
        float spatTimeRes = (float)(spatialRes * timeRes);
        if (frame > 1 && frame < size) {
            ipm1 = (short[]) ((new TypeConverter(stack.getProcessor(frame - 1), false)).convertToShort()).getPixels();
            ipp1 = (short[]) ((new TypeConverter(stack.getProcessor(frame + 1), false)).convertToShort()).getPixels();
        }
        if (frame > 1 && frame < size) {
            t1 = (float)(thresholds[frame - 2]);
            t2 = (float)(thresholds[frame]);
        }
        for (int j = 0; j < height; j++) {
            int offset = j * width;
            for (int i = 0; i < width; i++) {
                float g = 0;
                if (frame > 1 && frame < size) {
                    g = ipp1[i + offset] - t1 - ipm1[i + offset] + t2;
                }
                float delta = edgePix[i + offset];
                float z;
                if (delta == 0.0) {
                    z = 0.0f;
                } else {
                    z = g / delta;
                }
                outPix[i + offset] = z * spatTimeRes;
            }
        }
        output.setPixels(outPix);
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
        FloatPolygon fPoly = proi.getFloatPolygon();
        FloatProcessor xp = new FloatProcessor(fPoly.npoints, depth);
        FloatProcessor yp = new FloatProcessor(fPoly.npoints, depth);
        for (int x = 0; x < fPoly.npoints; x++) {
            for (int y = 0; y < depth; y++) {
                xp.putPixelValue(x, y, fPoly.xpoints[x]);
                yp.putPixelValue(x, y, fPoly.ypoints[x]);
            }
        }
        xp.setInterpolate(true);
        xp.setInterpolationMethod(ImageProcessor.BILINEAR);
        yp.setInterpolate(true);
        yp.setInterpolationMethod(ImageProcessor.BILINEAR);
        sig.setInterpolate(true);
        sig.setInterpolationMethod(ImageProcessor.BILINEAR);
        ImageProcessor sig2 = sig.resize(finalWidth, depth);
        ImageProcessor xp2 = xp.resize(finalWidth, depth);
        ImageProcessor yp2 = yp.resize(finalWidth, depth);
        Pixel points[] = new Pixel[finalWidth];
        for (int x = 0; x < finalWidth; x++) {
            double sum = 0.0;
            for (int y = 0; y < sig2.getHeight(); y++) {
                sum += sig2.getPixelValue(x, y);
            }
            points[x] = new Pixel(xp2.getPixelValue(x, 0), yp2.getPixelValue(x, 0), sum / depth);
        }
        return points;
    }

    public void clearBorderPix() {
        borderPix.clear();
    }

    public void loadPixels(LinkedList<Pixel> borderPix) {
//        this.pixels = (ArrayList) pixels.clone();
        this.borderPix = (LinkedList) borderPix.clone();
//        setSeedPix();
    }

    public ArrayList<Pixel> getCentres() {
        return centres;
    }

    public Pixel findSeed(ImageProcessor input) {
        int bx = 0, by = 0;
        if (bounds != null) {
            checkBounds();
            input.setRoi(bounds);
            bx = bounds.x;
            by = bounds.y;
        }
        ImageProcessor mask = input.crop();
        mask.invert();
        EDM edm = new EDM();
        edm.toEDM(mask);
        int[] max = Utils.findImageMaxima(mask);
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/edm.png");
        if (!(max[0] < 0.0 || max[1] < 0.0)) {
            return new Pixel(max[0] + bx, max[1] + by);
        } else {
            return null;
        }
    }

    void checkBounds() {
        if (bounds.x < 0) {
            bounds.x = 0;
        }
        if (bounds.y < 0) {
            bounds.y = 0;
        }
    }

    public boolean shrink(int iterations, boolean interpolate, int index) {
        ImageProcessor currentMask = getMask();
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/Test_Data_Sets/adapt_test_data/masks/mask_b" + index + ".png");
        for (int i = 0; i < iterations; i++) {
            currentMask.erode();
        }
        this.mask = currentMask;
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/Test_Data_Sets/adapt_test_data/masks/mask_a" + index + ".png");
        Pixel[] newBorder = getOrderedBoundary(currentMask.getWidth(), currentMask.getHeight(), currentMask, null);
        if (newBorder == null) {
            return false;
        }
        borderPix = new LinkedList();
        for (int j = 0; j < newBorder.length; j++) {
            addBorderPoint(newBorder[j]);
        }
        return true;
    }

    public Object clone() {
        try {
            super.clone();
        } catch (Exception e) {
            return null;
        }
        Region clone = new Region();
        clone.seedPix = (ArrayList) seedPix.clone();
        clone.borderPix = (LinkedList) borderPix.clone();
        clone.expandedBorder = (LinkedList) expandedBorder.clone();
        clone.centres = (ArrayList) centres.clone();
        clone.min = min;
        clone.max = max;
        clone.mean = mean;
        clone.seedMean = seedMean;
        clone.sigma = sigma;
        clone.mfD = mfD;
        clone.edge = edge;
        clone.active = active;
        clone.bounds = (Rectangle) bounds.clone();
        clone.histogram = (int[]) histogram.clone();
        clone.imageHeight = imageHeight;
        clone.imageWidth = imageWidth;
        return clone;
    }
}
