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
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author barry05
 */
public class Region2 {

//    protected ArrayList<short[]> seedPix = new ArrayList<short[]>();
    protected ArrayList<Pixel> pix = new ArrayList<Pixel>();
    protected LinkedList<Pixel> borderPix = new LinkedList<Pixel>();
    protected LinkedList<Pixel> expandedBorder = new LinkedList<Pixel>();
    protected ArrayList<Pixel> centres = new ArrayList<Pixel>();
    private double min = Double.MAX_VALUE, max = Double.MIN_VALUE, mean, seedMean, sigma;
    private double mfD[];
    protected boolean edge, active;
    protected Rectangle bounds;
//    private int[] histogram = new int[256];
    public final static short MASK_FOREGROUND = 0, MASK_BACKGROUND = 255;
    protected int imageWidth, imageHeight;
    private ImageProcessor mask;
    private int index;
    private Path2D path;
    private int maskSize;

    public Region2() {

    }

    public Region2(int index, int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.index = index;
    }

    public Region2(ImageProcessor mask, Pixel centre) {
        this.active = true;
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

    public Region2(int width, int height, Pixel centre) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.centres.add(centre);
        this.newBounds(centre);
//        this.addBorderPoint(centre);
        edge = false;
        active = true;
        seedMean = mean = 0.0;
    }

    void newBounds(Pixel point) {
        if (point != null) {
            bounds = new Rectangle(point.getRoundedX(), point.getRoundedY(), 1, 1);
        }
    }

    public final void addBorderPoint(Pixel point) {
        borderPix.add(point);
//        if (mask == null) {
//            drawMask(imageWidth, imageHeight);
//        }
//        drawMaskPixel(point.getRoundedX(), point.getRoundedY());
    }

    void drawMaskPixel(int x, int y) {
        mask.drawPixel(x, y);
        updateBounds(new Pixel(x, y));
    }

    public void calcStats(ImageProcessor refImage) {
        if (refImage == null) {
            return;
        }
        int[] histogram = new int[256];
        Arrays.fill(histogram, 0);
        int size = maskSize;
        double valSum = 0.0, varSum = 0.0;
        int coords[][] = getCoordsFromPath();
        if (size > 0) {
            for (int i = 0; i < size && coords[i] != null; i++) {
                int[] c = coords[i];
                double p = refImage.getPixelValue(c[0], c[1]);
                if (p < min) {
                    min = p;
                } else if (p > max) {
                    max = p;
                }
                valSum += p;
                int bin = (int) Math.floor(p);
                histogram[bin]++;
            }
            mean = valSum / (size);
            for (int i = 0; i < size && coords[i] != null; i++) {
                int[] c = coords[i];
                varSum += Math.pow(mean - refImage.getPixelValue(c[0], c[1]), 2);
            }
            sigma = Math.sqrt(varSum) / size;
        }
    }

    public void calcCentroid(ImageProcessor mask) {
        float xsum = 0.0f, ysum = 0.0f;
        int[][] coords = getCoordsFromPath();
        int size = maskSize;
        for (int i = 0; i < size && coords[i] != null; i++) {
            int[] c = coords[i];
            xsum += c[0];
            ysum += c[1];
        }
        centres.add(new Pixel(xsum / size, ysum / size));
    }

    public PolygonRoi getPolygonRoi(ImageProcessor mask) {
//        int bordersize = borderPix.size();
//        mask.setRoi(r);
//        mask = mask.crop();
//        int rx = 0, ry = 0;
//        if (r != null) {
//            rx = r.x;
//            ry = r.y;
//        }
//        tempImage.setValue(BACKGROUND);
//        tempImage.fill();
//        tempImage.setValue(FOREGROUND);
//        Polygon poly = new Polygon();
//        for (int i = 0; i < bordersize; i++) {
//            tempImage.drawPixel(borderPix.get(i).getX() - r.x, borderPix.get(i).getY() - r.y);
////            poly.addPoint(borderPix.get(i).getX(), borderPix.get(i).getY());
//        }
//        fill(tempImage, FOREGROUND, BACKGROUND);
        ArrayList<Pixel> centres = getCentres();
        Pixel centre = centres.get(centres.size() - 1);
//        short xc = (short) Math.round(centre[0] - rx);
//        short yc = (short) Math.round(centre[1] - ry);
        short xc = (short) Math.round(centre.getX());
        short yc = (short) Math.round(centre.getY());
        int[][] pix = getMaskOutline(new Pixel(xc, yc), mask);
        if (pix != null) {
            return new PolygonRoi(pix[0], pix[1], pix[0].length, Roi.POLYGON);
        } else {
            return null;
        }
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
            Pixel p = borderPix.get(i);
            if ((p.getRoundedX() == x) && (p.getRoundedY() == y)) {
                return i;
            }
        }
        return -1;
    }

    public List<Pixel> getBorderPix() {
        if (borderPix.size() > 0) {
            return borderPix;
        } else {
            return Arrays.asList(getOrderedBoundary(imageWidth, imageHeight, getMask(), getCentre()));
        }
    }

    public void removeBorderPoint(int index) {
        borderPix.remove(index);
    }

    public boolean expandBorder() {
        if (expandedBorder.size() < 1) {
            return false;
        } else {
            borderPix = expandedBorder;
            expandedBorder = new LinkedList<Pixel>();
            return true;
        }
    }

    public void addExpandedBorderPix(Pixel p) {
        expandedBorder.add(p);
        drawMaskPixel(p.getRoundedX(), p.getRoundedY());
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
        final Region2 other = (Region2) obj;
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

//    public ArrayList<short[]> getSeedPix() {
//        return seedPix;
//    }
    public ArrayList<Pixel> getPixels() {
        Rectangle bounds = getBounds();
        ImageProcessor mask = getMask();
        ArrayList<Pixel> pix = new ArrayList();
        for (int y = bounds.y; y < bounds.height + bounds.y; y++) {
            for (int x = bounds.x; x < bounds.width + bounds.x; x++) {
                if (mask.getPixel(x, y) == Region2.MASK_FOREGROUND) {
                    pix.add(new Pixel(x, y, 0, 1));
                }
            }
        }
        return pix;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    void updateBounds(Pixel pixel) {
        if (pixel == null) {
            return;
        }
        int x = pixel.getRoundedX();
        int y = pixel.getRoundedY();
        if (bounds == null) {
            bounds = new Rectangle(x, y, 1, 1);
            return;
        }
        if (x < bounds.x) {
            int inc = bounds.x - x;
            bounds.x -= inc;
            bounds.width += inc;
        } else if (x >= bounds.x + bounds.width) {
            bounds.width = x - bounds.x + 1;
        }
        if (y < bounds.y) {
            int inc = bounds.y - y;
            bounds.y -= inc;
            bounds.height += inc;
        } else if (y >= bounds.y + bounds.height) {
            bounds.height = y - bounds.y + 1;
        }
    }

    void updateBounds(Path2D path) {
        PathIterator pi = path.getPathIterator(null);
        while (!pi.isDone()) {
            float[] coords = new float[6];
            pi.currentSegment(coords);
            updateBounds(new Pixel(coords[0], coords[1]));
            pi.next();
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

//    public int[] getHistogram() {
//        return histogram;
//    }

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
        mask.setColor(MASK_BACKGROUND);
        mask.fill();
        mask.setColor(MASK_FOREGROUND);
        int m = borderPix.size();
        for (int i = 0; i < m; i++) {
            Pixel current = borderPix.get(i);
            mask.drawPixel(current.getRoundedX(), current.getRoundedY());
        }
        fill(mask, MASK_FOREGROUND, MASK_BACKGROUND);
        this.mask = mask;
        setMaskSize();
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

    public Pixel[] getOrderedBoundary(int width, int height, ImageProcessor mask, Pixel centre) {
        int[][] pix = getMaskOutline(centre, mask);
        if (pix == null) {
            return null;
        } else {
            short[][] points = DSPProcessor.interpolatePoints(pix[0].length, pix[0], pix[1]);
            Pixel[] output = new Pixel[points.length];
            for (int i = 0; i < points.length; i++) {
                output[i] = new Pixel(points[i][0], points[i][1]);
            }
            return output;
        }
    }

    int[][] getMaskOutline() {
        return getMaskOutline(this.getCentre(), this.getMask());
    }

    int[][] getMaskOutline(Pixel centre, ImageProcessor mask) {
        if (centre == null || mask.getPixel(centre.getRoundedX(), centre.getRoundedY()) != MASK_FOREGROUND) {
            Pixel seed = findSeed(mask);
            if (seed == null) {
                return null;
            } else {
                centre = seed;
            }
        }
        Wand wand = new Wand(mask);
        wand.autoOutline(centre.getRoundedX(), centre.getRoundedY(), 0.0, Wand.EIGHT_CONNECTED);
        int n = wand.npoints;
        int[][] pix = new int[n][2];
        Rectangle bounds = getBounds();
        for (int j = 0; j < n; j++) {
            pix[j][0] = wand.xpoints[j] + bounds.x;
            pix[j][1] = wand.ypoints[j] + bounds.y;
            if (pix[j][0] >= imageWidth) {
                pix[j][0] = imageWidth - 1;
            }
            if (pix[j][1] >= imageHeight) {
                pix[j][1] = imageHeight - 1;
            }
        }
        return pix;
    }

    public float[][] buildVelMapCol(short xc, short yc, ImageStack stack, int frame, double timeRes, double spatialRes, int[] thresholds) {
        ImageProcessor ip = stack.getProcessor(frame);
        ImageProcessor ipm1 = null, ipp1 = null;
        ImageProcessor edges = ip.duplicate();
        edges.findEdges();
        int size = stack.getSize();
        drawMask(ip.getWidth(), ip.getHeight());
        Pixel[] points = getOrderedBoundary(ip.getWidth(), ip.getHeight(),
                mask, new Pixel(xc, yc));
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
            int x = points[i].getRoundedX();
            int y = points[i].getRoundedY();
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
        PolygonRoi proi = getPolygonRoi(getMask());
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

    public ArrayList<Pixel> getCentres() {
        if (centres.size() < 1) {
            calcCentroid(getMask());
        }
        return centres;
    }

    public Pixel getCentre() {
        ArrayList<Pixel> c = getCentres();
        return c.get(c.size() - 1);
    }

    public Pixel findSeed(ImageProcessor input) {
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
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/adapt_debug/edm.png");
        if (!(max[0] < 0.0 || max[1] < 0.0)) {
            return new Pixel(max[0] + bx, max[1] + by);
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
        Pixel[] newBorder = getOrderedBoundary(mask.getWidth(), mask.getHeight(), mask, null);
        if (newBorder == null) {
            return false;
        }
        borderPix = new LinkedList<Pixel>();
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
        m.setColor(Region2.MASK_BACKGROUND);
        m.fill();
        m.copyBits(mask, bounds.x, bounds.y, Blitter.AND);
        return m;
    }

    public int getIndex() {
        return index;
    }

    public void addPoint(Pixel p) {
        if (mask == null) {
            drawMask(imageWidth, imageHeight);
        }
        pix.add(p);
        drawMaskPixel(p.getRoundedX(), p.getRoundedY());
        setMaskSize();
    }

    public void addPath(Path2D path) {
        if (this.path == null) {
            this.path = new Path2D.Float();
        }
        this.path.append(path, false);
        PathIterator pi = this.path.getPathIterator(null);
        mask = new ByteProcessor(imageWidth, imageHeight);
        mask.setValue(Region.MASK_BACKGROUND);
        mask.fill();
        mask.setValue(Region.MASK_FOREGROUND);

        float[] current = new float[6];
        pi.currentSegment(current);
        int[] last = {(int) Math.round(current[0]), (int) Math.round(current[1])};
        pi.next();
        int count = 0;
        while (!pi.isDone()) {
            if (pi.currentSegment(current) == PathIterator.SEG_LINETO) {
                int currentX = (int) Math.round(current[0]);
                int x1, x2;
                if (last[0] < currentX) {
                    x1 = last[0];
                    x2 = currentX;
                } else {
                    x2 = last[0];
                    x1 = currentX;
                }
                int y = last[1];
                for (int x = x1; x <= x2; x++) {
                    drawMaskPixel(x, y);
                }
            }
            pi.next();
            last = new int[]{(int) Math.round(current[0]), (int) Math.round(current[1])};
        }

//        float[] current = new float[6];
//        pi.currentSegment(current);
//        int[] last = {(int) Math.round(current[0]), (int) Math.round(current[1])};
//        pi.next();
//        while (!pi.isDone()) {
//            if (pi.currentSegment(current) == PathIterator.SEG_LINETO) {
//                int currentX = (int) Math.round(current[0]);
//                mask.drawLine(currentX, last[1], last[0], last[1]);
//                updateBounds(new Pixel(currentX, last[1]));
//                updateBounds(new Pixel(last[0], last[1]));
//            }
//            pi.next();
//            last = new int[]{(int) Math.round(current[0]), (int) Math.round(current[1])};
//        }
//IJ.saveAs((new ImagePlus("", mask)), "PNG", "/Users/Dave/Desktop/EMSeg Test Output/Mask_addPath_PreCrop_" + index);
        mask.setRoi(getBounds());
        mask = mask.crop();
        setMaskSize();
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "/Users/Dave/Desktop/EMSeg Test Output/Mask_addPath_PostCrop_" + index);
    }

    void setMaskSize() {
        this.maskSize = mask.getStatistics().histogram[Region.MASK_FOREGROUND];
    }

    public int[][] getCoordsFromPath() {
        int size = maskSize;
        int coords[][] = new int[size][];
        PathIterator pi = path.getPathIterator(null);
        float[] current = new float[6];
        pi.currentSegment(current);
        int[] last = {(int) Math.round(current[0]), (int) Math.round(current[1])};
        pi.next();
        int count = 0;
        while (!pi.isDone()) {
            if (pi.currentSegment(current) == PathIterator.SEG_LINETO) {
                int currentX = (int) Math.round(current[0]);
                int x1, x2;
                if (last[0] < currentX) {
                    x1 = last[0];
                    x2 = currentX;
                } else {
                    x2 = last[0];
                    x1 = currentX;
                }
                int y = last[1];
                for (int x = x1; x <= x2; x++) {
                    coords[count++] = new int[]{x, y};
                }
            }
            pi.next();
            last = new int[]{(int) Math.round(current[0]), (int) Math.round(current[1])};
        }
//        System.out.println(index + ": " + size + " " + count);
//        IJ.saveAs((new ImagePlus("", mask)), "PNG", "/Users/Dave/Desktop/EMSeg Test Output/Mask_getCoorsFromPath_" + index);
        return coords;
    }

    public Path2D getPath() {
        return path;
    }

    public ImageProcessor drawPath() {
        PathIterator pi = path.getPathIterator(null);
        ByteProcessor bp = new ByteProcessor(imageWidth, imageHeight);
        bp.setValue(Region.MASK_BACKGROUND);
        bp.fill();
        bp.setValue(Region.MASK_FOREGROUND);
        bp.setLineWidth(3);
        while (!pi.isDone()) {
            float[] coords = new float[6];
            pi.currentSegment(coords);
            bp.drawDot((int) Math.round(coords[0]), (int) Math.round(coords[1]));
            pi.next();
        }
        return bp;
    }

    public ImageProcessor drawArea() {
        PathIterator pi = path.getPathIterator(null);
        ByteProcessor bp = new ByteProcessor(imageWidth, imageHeight);
        bp.setValue(Region.MASK_BACKGROUND);
        bp.fill();
        bp.setValue(Region.MASK_FOREGROUND);
        bp.setLineWidth(3);
        float[] current = new float[6];
        pi.currentSegment(current);
        float[] last = {current[0], current[1]};
        pi.next();
        while (!pi.isDone()) {
            if (pi.currentSegment(current) == PathIterator.SEG_LINETO) {
                bp.drawLine((int) Math.round(current[0]), (int) Math.round(current[1]),
                        (int) Math.round(last[0]), (int) Math.round(last[1]));
            }
            pi.next();
            last = new float[]{current[0], current[1]};
        }
        return bp;
    }

    public void addBorderPoints(List<Pixel> borderPointsToBeAdded, ArrayList<Region2> regions, int baseIndex, int size) {
        List<Pixel> thisBorderPoints = this.getBorderPix();
        int ownerOfNewBorderPoints = borderPointsToBeAdded.get(0).getRoundedZ();
        List<Pixel> pointsToBeRemoved = new LinkedList();
        boolean[] indices = new boolean[size];
        Arrays.fill(indices, false);
        for (Pixel pix : thisBorderPoints) {
            int neighbour = pix.getNeighbouringRegionIndex();
            if (neighbour == ownerOfNewBorderPoints) {
                pointsToBeRemoved.add(pix);
            }
        }
        for (Pixel pix : pointsToBeRemoved) {
            thisBorderPoints.remove(pix);
        }
        pointsToBeRemoved.clear();
        for (Pixel pix : borderPointsToBeAdded) {
            int neighbour = pix.getNeighbouringRegionIndex();
            if (neighbour == index) {
                pointsToBeRemoved.add(pix);
            } else {
                pix.setZ(index);
                if (neighbour >= baseIndex) {
                    indices[neighbour - baseIndex] = true;
                }
            }
        }
        for (Pixel pix : pointsToBeRemoved) {
            borderPointsToBeAdded.remove(pix);
        }
        for (Pixel pix : borderPointsToBeAdded) {
            this.addBorderPoint(pix);
        }
        for (int i = 0; i < indices.length; i++) {
            if (indices[i]) {
                for (Region2 r : regions) {
                    if (r.getIndex() == i + baseIndex) {
                        r.updateBorderPoints(ownerOfNewBorderPoints, index);
                    }
                }
            }
        }
    }

    public void updateBorderPoints(int oldID, int newID) {
        List<Pixel> bp = getBorderPix();
        for (Pixel p : bp) {
            if (p.getNeighbouringRegionIndex() == oldID) {
                p.setNeighbouringRegionIndex(newID);
            }
        }
    }
}
