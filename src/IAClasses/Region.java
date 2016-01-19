package IAClasses;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.Straightener;
import ij.plugin.filter.EDM;
import ij.process.Blitter;
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
    private ArrayList<short[]> seedPix = new ArrayList<short[]>();
//    private ArrayList<ArrayList> pixMem = new ArrayList<ArrayList>();
//    private ArrayList<LinkedList> borderPixMem = new ArrayList<LinkedList>();
    private LinkedList<short[]> borderPix = new LinkedList<short[]>();
    private LinkedList<short[]> expandedBorder = new LinkedList<short[]>();
//    private ArrayList<Pixel> centroids = new ArrayList<Pixel>();
//    private ArrayList<Pixel> geoMedians = new ArrayList<Pixel>();
    private ArrayList<float[]> centres = new ArrayList<float[]>();
    private double min = Double.MAX_VALUE, max = Double.MIN_VALUE, mean, seedMean, sigma;
    private double mfD[];
    private boolean edge, active;
    private Rectangle bounds;
    private int[] histogram = new int[256];
//    private int initX, initY;
    public final static short FOREGROUND = 0, BACKGROUND = 255;
    private int imageWidth, imageHeight;
//    private final int memSize = 10;
    private ImageProcessor mask;

    public Region() {

    }

    public Region(ImageProcessor mask, short[] centre) {
        this.active = true;
        this.imageWidth = mask.getWidth();
        this.imageHeight = mask.getHeight();
        if (centre != null) {
            this.centres.add(new float[]{centre[0], centre[1]});
        }
        this.mask = mask;
        this.newBounds(centre);
        short[][] bp = this.getOrderedBoundary(imageWidth, imageHeight, mask, centre);
        if (bp != null) {
            for (int i = 0; i < bp.length; i++) {
                this.addBorderPoint(bp[i]);
            }
        }
    }

    public Region(int width, int height, short[] centre) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.centres.add(new float[]{centre[0], centre[1]});
        this.newBounds(centre);
        this.addBorderPoint(centre);
        edge = false;
        active = true;
        seedMean = mean = 0.0;
    }

    void newBounds(short[] point) {
        if (point != null) {
            bounds = new Rectangle(point[0], point[1], 1, 1);
        }
    }

    public void addBorderPoint(short[] point) {
        borderPix.add(point);
        updateBounds(point);
        mask.drawPixel(point[0], point[1]);
    }

    public void calcStats(ImageProcessor refImage) {
        if (refImage == null) {
            return;
        }
        Arrays.fill(histogram, 0);
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
        float xsum = 0.0f, ysum = 0.0f;
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
        centres.add(new float[]{xsum / count, ysum / count});
//            return true;
//        } else {
//            return false;
//        }
    }

    PolygonRoi getPolygonRoi() {
//        int bordersize = borderPix.size();
        Rectangle r = getBounds();
        ImageProcessor mask = getMask().duplicate();
        mask.setRoi(r);
        mask = mask.crop();
//        tempImage.setValue(BACKGROUND);
//        tempImage.fill();
//        tempImage.setValue(FOREGROUND);
//        Polygon poly = new Polygon();
//        for (int i = 0; i < bordersize; i++) {
//            tempImage.drawPixel(borderPix.get(i).getX() - r.x, borderPix.get(i).getY() - r.y);
////            poly.addPoint(borderPix.get(i).getX(), borderPix.get(i).getY());
//        }
//        fill(tempImage, FOREGROUND, BACKGROUND);
//        IJ.saveAs((new ImagePlus("", tempImage)), "PNG", "C:/users/barry05/desktop/tempImage.png");
        Wand wand = new Wand(mask);
        wand.autoOutline(borderPix.get(0)[0] - r.x, borderPix.get(0)[1] - r.y, FOREGROUND, FOREGROUND);
        int n = wand.npoints;
        int xpix[] = new int[n];
        int ypix[] = new int[n];
        System.arraycopy(wand.xpoints, 0, xpix, 0, wand.npoints);
        System.arraycopy(wand.ypoints, 0, ypix, 0, wand.npoints);
        for (int j = 0; j < n; j++) {
            xpix[j] = wand.xpoints[j] + r.x;
            ypix[j] = wand.ypoints[j] + r.y;
        }
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
            short[] p = borderPix.get(i);
            if ((p[0] == x) && (p[1] == y)) {
                return i;
            }
        }
        return -1;
    }

    public LinkedList<short[]> getBorderPix() {
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

    public void addExpandedBorderPix(short[] p) {
        expandedBorder.add(p);
        updateBounds(p);
        mask.drawPixel(p[0], p[1]);
    }

    public LinkedList<short[]> getExpandedBorder() {
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

    public ArrayList<short[]> getSeedPix() {
        return seedPix;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    void updateBounds(short[] pixel) {
        if (pixel == null) {
            return;
        }
        int x = pixel[0];
        int y = pixel[1];
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
            short[] current = borderPix.get(i);
            mask.drawPixel(current[0], current[1]);
        }
        fill(mask, FOREGROUND, BACKGROUND);
        this.mask = mask;
    }

    public ImageProcessor getMask() {
        if (mask == null) {
            drawMask(imageWidth, imageHeight);
        } else if (mask.getWidth() != imageWidth || mask.getHeight() != imageHeight) {
            return constructFullSizeMask(imageWidth, imageHeight);
        }
        return mask.duplicate();
    }

    public static double[] calcCurvature(short[][] pix, int step) {
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
                double theta1 = Utils.arcTan(pix[j][0] - pix[i][0], pix[j][1] - pix[i][1]);
                double theta2 = Utils.arcTan(pix[k][0] - pix[j][0], pix[k][1] - pix[j][1]);
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

    public short[][] getOrderedBoundary(int width, int height, ImageProcessor mask, short[] centre) {
        if (centre == null || mask.getPixel(centre[0], centre[1]) != FOREGROUND) {
            short[] seed = findSeed(mask);
            if (seed == null) {
                return null;
            } else {
                centre = seed;
            }
        }
        Wand wand = new Wand(mask);
        wand.autoOutline(centre[0], centre[1], 0.0, Wand.EIGHT_CONNECTED);
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

    public float[][] buildVelMapCol(short xc, short yc, ImageStack stack, int frame, double timeRes, double spatialRes, int[] thresholds) {
        ImageProcessor ip = stack.getProcessor(frame);
        ImageProcessor ipm1 = null, ipp1 = null;
        ImageProcessor edges = ip.duplicate();
        edges.findEdges();
        int size = stack.getSize();
        drawMask(ip.getWidth(), ip.getHeight());
        short points[][] = getOrderedBoundary(ip.getWidth(), ip.getHeight(),
                mask, new short[]{xc, yc});
        float floatPoints[][] = new float[points.length][3];
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
            int x = points[i][0];
            int y = points[i][1];
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
            floatPoints[i] = new float[]{x, y, (float) (z * spatialRes * timeRes)};
        }
        return floatPoints;
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
        float spatTimeRes = (float) (spatialRes * timeRes);
        if (frame > 1 && frame < size) {
            ipm1 = (short[]) ((new TypeConverter(stack.getProcessor(frame - 1), false)).convertToShort()).getPixels();
            ipp1 = (short[]) ((new TypeConverter(stack.getProcessor(frame + 1), false)).convertToShort()).getPixels();
        }
        if (frame > 1 && frame < size) {
            t1 = (float) (thresholds[frame - 2]);
            t2 = (float) (thresholds[frame]);
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

    public float[][] buildMapCol(ImageProcessor ip, int finalWidth, int depth) {
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
        float points[][] = new float[finalWidth][3];
        for (int x = 0; x < finalWidth; x++) {
            double sum = 0.0;
            for (int y = 0; y < sig2.getHeight(); y++) {
                sum += sig2.getPixelValue(x, y);
            }
            points[x] = new float[]{(float) xp2.getPixelValue(x, 0), (float) yp2.getPixelValue(x, 0), (float) (sum / depth)};
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

    public ArrayList<float[]> getCentres() {
        return centres;
    }

    public short[] findSeed(ImageProcessor input) {
        int bx = 0, by = 0;
        if (bounds != null) {
            bounds = checkBounds(bounds);
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
            return new short[]{(short) (max[0] + bx), (short) (max[1] + by)};
        } else {
            return null;
        }
    }

    public Rectangle checkBounds(Rectangle bounds) {
        if (bounds.x < 0) {
            bounds.x = 0;
        }
        if (bounds.y < 0) {
            bounds.y = 0;
        }
        if (bounds.x + bounds.width > imageWidth) {
            bounds.width = imageWidth - bounds.x;
        }
        if (bounds.y + bounds.height > imageHeight) {
            bounds.height = imageHeight - bounds.y;
        }
        return bounds;
    }

    public boolean shrink(int iterations, boolean interpolate, int index) {
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/Test_Data_Sets/adapt_test_data/masks/mask_b" + index + ".png");
        if (mask == null) {
            mask = getMask();
        }
        for (int i = 0; i < iterations; i++) {
            mask.erode();
        }
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/Test_Data_Sets/adapt_test_data/masks/mask_a" + index + ".png");
        short[][] newBorder = getOrderedBoundary(mask.getWidth(), mask.getHeight(), mask, null);
        if (newBorder == null) {
            return false;
        }
        borderPix = new LinkedList();
        for (int j = 0; j < newBorder.length; j++) {
            addBorderPoint(newBorder[j]);
        }
        return true;
    }

    public void setFinalMask() {
        mask.setRoi(bounds);
        mask = mask.crop();
    }

    ImageProcessor constructFullSizeMask(int width, int height) {
        ByteProcessor m = new ByteProcessor(width, height);
        m.setColor(Region.BACKGROUND);
        m.fill();
        m.copyBits(mask, bounds.x, bounds.y, Blitter.AND);
        return m;
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
