package net.calm.iaclasslibrary.IAClasses;

import net.calm.iaclasslibrary.Extrema.MaximaFinder;
import net.calm.iaclasslibrary.Image.ImageNormaliser;
import net.calm.iaclasslibrary.Particle.IsoGaussian;
import net.calm.iaclasslibrary.Particle.Particle;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackConverter;
import ij.process.StackStatistics;
import ij.process.TypeConverter;
import java.awt.Rectangle;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import org.apache.commons.io.FilenameUtils;

public class Utils {

    /**
     * Constructs an {@link ImageStack} from an image sequence stored in
     * <code>directory</code>.
     *
     * @return the constructed {@link ImageStack}.
     */
    public static ImagePlus buildStack(File directory) {
        if (directory == null) {
            return null;
        }
        File images[] = directory.listFiles();
        int i, noOfImages = images.length;
        /*
         * Load first image to obtain dimensions
         */
        ImagePlus currentImage;
        ImageProcessor processor;
        String ext = null;
        ImageStack output = null;
        for (i = 0; i < noOfImages; i++) {
            String thisext = FilenameUtils.getExtension(images[i].getName());
            currentImage = new ImagePlus(images[i].getPath());
            processor = currentImage.getProcessor();
            if (processor != null) {
                if (ext != null) {
                    if (!ext.matches(thisext)) {
                        return null;
                    }
                } else {
                    ext = thisext;
                }
                if (output == null) {
                    output = new ImageStack(processor.getWidth(), processor.getHeight());
                }
                output.addSlice(IJ.openImage(images[i].getAbsolutePath()).getProcessor());
            }
        }
        return new ImagePlus("." + ext, output);
    }

    /**
     * Copies pixel values from a square area specified by
     * <code>(xc - r, yc - r, xc + r, yc + r)</code> from <code>image</code>
     * into <code>values</code>.
     *
     * @param xCoords x-coordinates of the pixels will be copied in here
     * @param yCoords y-coordinates of the pixels will be copied in here
     * @param values the <code>2 * r + 1 x 2 * r + 1</code> array into which
     * pixel values will be copied
     * @param image the image from which pixel values are extracted
     */
    public static void extractValues(double[] xCoords, double[] yCoords,
            double[][] values, int xc, int yc, ImageProcessor image) {
        int i, j, x, y, w, h;
        if (image == null || xCoords == null || yCoords == null || values == null) {
            return;
        }
        w = image.getWidth();
        h = image.getHeight();
        if ((xc < 0) || (xc >= w) || (yc < 0) || (yc >= h)) {
            return;
        }
        int radius = (values.length - 1) / 2;
        for (x = xc - radius, i = 0; (x <= xc + radius) && (x < w) && (x >= 0); x++) {
            xCoords[i] = x;
            for (y = yc - radius, j = 0; (y <= yc + radius) && (y < h) && (y >= 0); y++) {
                yCoords[j] = y;
                values[i][j] = image.getPixelValue(x, y);
                j++;
            }
            i++;
        }
        return;
    }

    public static ImageProcessor extractValues(int xc, int yc, int radius, ImageProcessor image) {
        if (image == null) {
            return null;
        }
        if ((xc < 0) || (xc >= image.getWidth()) || (yc < 0) || (yc >= image.getHeight())) {
            return null;
        }
        image.setRoi(new Rectangle(xc - radius, yc - radius, 2 * radius + 1, 2 * radius + 1));

        return image.crop();
    }

    /**
     * Searches within <code>radius</code> pixels of ( <code>x</code>,
     * <code>y</code>) in <code>image</code> for a grey level equal to
     * <code>value</code>. If more than one suitable candidate is identified,
     * that nearest to <code>(x, y)</code> is returned.
     *
     * @return the co-ordinates of the point (in the form <code>{x, y}</code>)
     * if a pixel of <code>value</code> was located, <code>null</code>
     * otherwise.
     */
    public static int[][] searchNeighbourhood(int x, int y, int radius, double value, ImageProcessor image) {
        if (image == null || x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return null;
        }
        ArrayList<Pixel> pixels = new ArrayList<Pixel>();
        double currentDist, minDist = Double.MAX_VALUE;

        for (int i = x - radius; i <= x + radius; i++) {
            for (int j = y - radius; j <= y + radius; j++) {
                if (image.getPixelValue(i, j) >= value) {
                    currentDist = calcDistance(i, j, x, y);
                    if (currentDist < minDist) {
                        Pixel p = new Pixel(i, j, currentDist);
                        pixels.add(p);
                        minDist = currentDist;
                    }
                }
            }
        }
        if (pixels.size() > 0) {
            int points[][] = new int[pixels.size()][2];
            int l = 0;
            while (!pixels.isEmpty()) {
                double currentMin = Double.MAX_VALUE;
                int minIndex = -1;
                for (int k = 0; k < pixels.size(); k++) {
                    Pixel current = (Pixel) pixels.get(k);
                    if (current.getZ() < currentMin) {
                        currentMin = current.getZ();
                        minIndex = k;
                    }
                }
                points[l][0] = ((Pixel) pixels.get(minIndex)).getRoundedX();
                points[l][1] = ((Pixel) pixels.get(minIndex)).getRoundedY();
                pixels.remove(minIndex);
                l++;
            }
            return points;
        }
        return null;
    }

    @Deprecated
    public static ByteProcessor findLocalMaxima(int kWidth, int kHeight, int drawValue, ImageProcessor image, double maxThresh, boolean varyBG, int buffer) {
        return MaximaFinder.findLocalMaxima(kWidth, kHeight, drawValue, image, maxThresh, varyBG, buffer);
    }

    @Deprecated
    public static ArrayList<int[]> findLocalMaxima(int kWidth, ImageProcessor image, double maxThresh, boolean varyBG, boolean absolute) {
        return MaximaFinder.findLocalMaxima(kWidth, image, (float) maxThresh, varyBG, absolute);
    }

    @Deprecated
    public static ArrayList<int[]> findLocalMaxima(int xyRadius, ImageStack stack, double maxThresh, boolean varyBG, boolean absolute, int zRadius) {
        return MaximaFinder.findLocalMaxima(xyRadius, stack, (float) maxThresh, varyBG, absolute, zRadius);
    }

    @Deprecated
    public static int[] findImageMaxima(ImageProcessor image) {
        return MaximaFinder.findImageMaxima(image);
    }

    public static int[] searchImage(ByteProcessor image, int searchVal) {
        if (image == null) {
            return null;
        }
        int pix[] = {-1, -1};
        int width = image.getWidth(), height = image.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (image.getPixel(x, y) == searchVal) {
                    pix[0] = x;
                    pix[1] = y;
                    return pix;
                }
            }
        }
        return pix;
    }

    /**
     * Returns the distance between <code>{x1, y1}</code> and
     * <code>{x2, y2}</code>.
     */
    public static double calcDistance(double x1, double y1, double x2, double y2) {
        return calcEuclidDist(new double[]{x1, y1}, new double[]{x2, y2});
    }

    public static double calcEuclidDist(double vector1[], double vector2[]) {
        if (vector1 == null || vector2 == null || (vector1.length != vector2.length)) {
            return Double.NaN;
        }
        int m = vector1.length;
        double sum = 0.0;
        for (int i = 0; i < m; i++) {
            sum += Math.pow(vector1[i] - vector2[i], 2.0);
        }
        return Math.sqrt(sum);
    }

    /**
     * Calculates a 2 x 2 covariance matrix.
     *
     * @param xValues a vector of length <code>n</code>.
     * @param yValues a vector of length <code>m</code>.
     * @param xSum the sum of all entries in <code>xValues</code>.
     * @param ySum the sum of all entries in <code>yValues</code>.
     * @return the covariance matrix for <code>xValues</code> and
     * <code>yValues</code>, or <code>null</code> if <code>m != n</code>.
     */
    public static double[][] covarianceMatrix(double[] xValues, double[] yValues, double xSum, double ySum) {
        if (xValues == null || yValues == null || (xValues.length != yValues.length)) {
            return null;
        }
        int size = xValues.length;

        double covariance[][] = new double[2][2];
        double xMean = xSum / size;
        double yMean = ySum / size;

        covariance[0][0] = calcCovariance(xValues, xValues, xMean, xMean);
        covariance[0][1] = calcCovariance(xValues, yValues, xMean, yMean);
        covariance[1][0] = covariance[0][1];
        covariance[1][1] = calcCovariance(yValues, yValues, yMean, yMean);

        return covariance;
    }

    /**
     * Calculates the covariance of two variables.
     *
     * @param variable1 a vector of length <code>n</code>.
     * @param variable2 a vector of length <code>m</code>.
     * @param mean1 the mean of the elements of <code>variable1</code>.
     * @param mean2 the mean of the elements of <code>variable2</code>.
     * @return the covariance of <code>variable1</code> and
     * <code>variable2</code>, or <code>Double.Nan</code> if
     * <code>m != n</code>.
     */
    public static double calcCovariance(double[] variable1, double[] variable2, double mean1, double mean2) {
        if (variable1 == null || variable2 == null || (variable1.length != variable2.length)) {
            return Double.NaN;
        }
        double covariance = 0;
        int i, n = variable1.length;
        if (n <= 1) {
            return Double.NaN;
        }
        for (i = 0; i < n; i++) {
            covariance += (variable1[i] - mean1) * (variable2[i] - mean2);
        }

        return covariance / (n - 1);
    }

    /**
     * Calculates the eigenvalues of a 2 x 2 matrix.
     *
     * @param matrix a 2 x 2 matrix.
     * @return the eigenvalues of <code>matrix</code> in the form of an array
     * with two elements.
     */
    public static double[] calcEigenvalues(double[][] matrix) {
        if (matrix == null || matrix.length != 2 || matrix[0].length != 2) {
            return null;
        }
        double eigenvalues[] = new double[2];

        double a = matrix[0][0];
        double b = matrix[0][1];
        double c = matrix[1][0];
        double d = matrix[1][1];
        double sqrt = Math.sqrt(a * a - 2 * a * d + 4 * b * c + d * d);

        eigenvalues[0] = ((a + d) + sqrt) / 2.0d;
        eigenvalues[1] = ((a + d) - sqrt) / 2.0d;

        return eigenvalues;
    }

    /**
     * Converts each entry in a <code>byte</code> array to a <code>float</code>.
     *
     * @param array an array of <code>byte</code>s.
     * @return an array of <code>float</code>s.
     */
    public static float[] byteArrayToFloat(byte[] array) {
        if (array == null) {
            return null;
        }
        int i, length = array.length;
        float[] output = new float[length];
        for (i = 0; i < length; i++) {
            output[i] = (float) (array[i] & 0xff);
        }

        return output;
    }

    public static double arcTan(double xVal, double yVal) {
        double outVal;

        if (xVal != 0.0) {
            if (xVal > 0.0 && yVal > 0.0) {
                outVal = 360.0 - Math.atan(yVal / xVal) * 180.0 / Math.PI;
            } else if (xVal < 0.0 && yVal > 0.0) {
                xVal *= -1;
                outVal = 180.0 + Math.atan(yVal / xVal) * 180.0 / Math.PI;
            } else if (xVal < 0.0 && yVal < 0.0) {
                xVal *= -1;
                yVal *= -1;
                outVal = 180.0 - Math.atan(yVal / xVal) * 180.0 / Math.PI;
            } else if (xVal > 0.0 && yVal < 0.0) {
                yVal *= -1;
                outVal = Math.atan(yVal / xVal) * 180.0 / Math.PI;
            } else if (xVal > 0.0 && yVal == 0.0) {
                outVal = 0.0;
            } else {
                outVal = 180.0;
            }
        } else if (yVal > 0.0) {
            outVal = 270.0;
        } else if (yVal < 0.0) {
            outVal = 90.0;
        } else {
            outVal = 0.0;
        }

        return outVal;
    }

    public static double angleBetweenTwoLines(double vector1[], double vector2[]) {
        double m1 = vector1[1] / vector1[0];
        double m2 = vector2[1] / vector2[0];

        return Math.atan(Math.abs((m1 - m2) / (1.0 + m1 * m2)));
    }

    public static double[] convolve(double vector1[], double vector2[]) {
        int m = vector1.length, n = vector2.length;
        double convolution[];
        convolution = new double[m + n - 1];
        int j, k;
        for (k = 0; k < convolution.length; k++) {
            convolution[k] = 0.0;
            for (j = 0; j < m; j++) {
                if (k - j >= 0 && k - j < n) {
                    convolution[k] += vector1[j] * vector2[k - j];
                }
            }
        }
        return convolution;
    }

    public static double[] convolveCurve(double vector1[], double vector2[]) {
        int m = vector1.length, n = vector2.length;
        double convolution[];
        convolution = new double[n];
        int j, k;
        int radius = (m - 1) / 2;
        for (k = 0; k < convolution.length; k++) {
            convolution[k] = 0.0;
            for (j = 0; j < m; j++) {
                int index = k - radius + j;
                if (index < 0) {
                    index += n;
                } else if (index >= n) {
                    index -= n;
                }
                convolution[k] += vector1[j] * vector2[index];
            }
        }
        return convolution;
    }

    public static double[] generateGaussian(double sigma, int length) {
        double gaussian[] = new double[length];
        double a = 1.0 / (sigma * Math.sqrt(2.0 * Math.PI));
        double twoSigma2 = 2.0 * sigma * sigma;
        int i0 = (length - 1) / 2;
        for (int i = 0; i < length; i++) {
            gaussian[i] = a * Math.exp(-((i - i0) * (i - i0)) / twoSigma2);
        }
        return gaussian;
    }

    public static double[] generateGaussianFirstDeriv(double sigma, int length) {
        double dGaussian[] = new double[length];
        double twoSigma2 = 2.0 * sigma * sigma;
        double a = 1.0 / (sigma * sigma * sigma * Math.sqrt(2.0 * Math.PI));
        int i0 = (length - 1) / 2;
        for (int i = 0; i < length; i++) {
            dGaussian[i] = -i * a * Math.exp(-((i - i0) * (i - i0)) / twoSigma2);
        }
        return dGaussian;
    }

    public static double[] generateGaussianSecondDeriv(double sigma, int length) {
        double ddGaussian[] = new double[length];
        double a = 1.0 / (sigma * sigma * sigma * Math.sqrt(2.0 * Math.PI)), b;
        double sigma2 = sigma * sigma;
        double twoSigma2 = 2.0 * sigma2;
        int i0 = (length - 1) / 2;
        for (int i = 0; i < length; i++) {
            b = ((i * i) / sigma2) - 1.0;
            ddGaussian[i] = a * b * Math.exp(-((i - i0) * (i - i0)) / twoSigma2);
        }
        return ddGaussian;
    }

    public static double getArrayMean(double[][] array) {
        double sum = 0.0;
        for (int x = 0; x < array.length; x++) {
            for (int y = 0; y < array[x].length; y++) {
                sum += array[x][y];
            }
        }
        return sum / (array.length * array[0].length);
    }

    public static ImageProcessor normalise(ImageProcessor original, double norm) {
        ImageProcessor normalised = (new TypeConverter(original, true)).convertToFloat(null);
        double max = normalised.getMax();
        double min = normalised.getMin();
        normalised.subtract(min);
        normalised.multiply(norm / (max - min));
        normalised.resetMinAndMax();
        return normalised;
    }

    /*
     * Normalise all images in the given stack
     */
    public static void normaliseStack(ImageStack stack, double norm) {
        int stackLength = stack.getSize();
        for (int i = 0; i < stackLength; i++) {
            ImageProcessor cip = stack.getProcessor(1);
            cip = Utils.normalise(cip, norm);
            stack.deleteSlice(1);
            stack.addSlice(cip);
        }
    }

    public static FloatProcessor gradDirection(ImageProcessor input) {
        int rx = 1, ry = 1;
        float[] xKernel = {-1.0f, -2.0f, -1.0f,
            0.0f, 0.0f, 0.0f,
            1.0f, 2.0f, 1.0f};
        float[] yKernel = {-1.0f, 0.0f, 1.0f,
            -2.0f, 0.0f, 2.0f,
            -1.0f, 0.0f, 1.0f};

        return getAngle(convolve(input, xKernel, rx, ry), convolve(input, yKernel, rx, ry));
    }

    public static FloatProcessor convolve(ImageProcessor input, float[] kernel, int rx, int ry) {
        // TODO There probably exists a method in a package somewhere to do this more efficiently
        int x, y, i, j, width = input.getWidth(), height = input.getHeight();
        int yoffset = 2 * rx + 1;
        float value;
        FloatProcessor output = new FloatProcessor(width, height);

        for (x = rx; x < width - rx; x++) {
            for (y = ry; y < height - ry; y++) {
                for (value = 0.0f, i = x - rx; i <= x + rx; i++) {
                    for (j = y - ry; j <= y + ry; j++) {
                        value += input.getPixelValue(i, j) * kernel[(i - x + rx) + (j - y + ry) * yoffset];
                    }
                }
                output.putPixelValue(x, y, value);
            }
        }
        return output;
    }

    public static FloatProcessor getAngle(ImageProcessor xInput, ImageProcessor yInput) {
        int x, y, width = xInput.getWidth(), height = xInput.getHeight();
        FloatProcessor output = new FloatProcessor(width, height);
        double xVal, yVal, outVal;

        for (x = 0; x < width; x++) {
            for (y = 0; y < height; y++) {
                xVal = xInput.getPixelValue(x, y);
                yVal = yInput.getPixelValue(x, y);
                outVal = Utils.arcTan(xVal, yVal);
                output.putPixelValue(x, y, outVal);
            }
        }
        return output;
    }

    public static double[] getStackMinMax(ImageStack stack) {
        double[] minMax = new double[2];
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        int length = stack.getSize();
        for (int i = 1; i <= length; i++) {
            double thisMax = stack.getProcessor(i).getStatistics().max;
            double thisMin = stack.getProcessor(i).getStatistics().min;
            if (thisMax > max) {
                max = thisMax;
            }
            if (thisMin < min) {
                min = thisMin;
            }
        }
        minMax[0] = min;
        minMax[1] = max;
        return minMax;
    }

    public static ImageStack convertStackToRGB(ImageStack input) {
        if (input == null) {
            return null;
        }
        ImageStack output = new ImageStack(input.getWidth(), input.getHeight());
        for (int i = 1; i <= input.getSize(); i++) {
            output.addSlice(new TypeConverter(input.getProcessor(i), true).convertToRGB());
        }
        return output;
    }

    /**
     *
     * @param image
     * @param p
     * @param tol
     * @param res
     * @param invert
     * @return
     *
     * @deprecated use {@link net.calm.iaclasslibrary.ParticleWriter.ParticleWriter#drawParticle(ij.process.ImageProcessor, Particle.Particle, boolean, double, double, int) instead
     */
    public static boolean drawParticle(ImageProcessor image, Particle p, double tol, double res, boolean invert) {
        if (p instanceof IsoGaussian) {
            return draw2DGaussian(image, (IsoGaussian) p, tol, res, invert);
        } else {
            return drawBlob(image, p, res);
        }
    }

    public static boolean drawBlob(ImageProcessor image, Particle p, double res) {
        if (image == null || p == null) {
            return false;
        }
        int x = (int) Math.round(p.getX() / res);
        int y = (int) Math.round(p.getY() / res);
        double value = image.getPixelValue(x, y);
        image.putPixelValue(x, y, value + 1.0);
        return true;
    }

    /**
     * Draws a 2D IsoGaussian using the parameters contained in <code>g</code>
     * over a square region of dimension <code>2 * radius + 1</code> in
     * <code>image</code>.
     */
    public static boolean draw2DGaussian(ImageProcessor image, IsoGaussian g, double tol, double res, boolean invert) {
        if (image == null || g == null || g.getFit() < tol) {
            return false;
        }
        int x, y, drawRad;
        double x0 = g.getX() / res;
        double y0 = g.getY() / res;
        double xSigma = g.getXSigma();
        double xSigma2 = 2.0 * xSigma * xSigma;
        double ySigma = g.getYSigma();
        double ySigma2 = 2.0 * ySigma * ySigma;
        double value;
        drawRad = (int) Math.round(xSigma * 3.0);
        for (x = (int) Math.floor(x0 - drawRad); x <= x0 + drawRad; x++) {
            for (y = (int) Math.floor(y0 - drawRad); y <= y0 + drawRad; y++) {
                /*
                 * The current pixel value is added so as not to "overwrite"
                 * other Gaussians in close proximity:
                 */
                double pval = image.getPixelValue(x, y);
                value = g.getMagnitude() * Math.exp(-(((x - x0) * (x - x0)) / xSigma2
                        + ((y - y0) * (y - y0)) / ySigma2));
                if (invert) {
                    pval -= value;
                } else {
                    pval += value;
                }
                if (pval < 0.0) {
                    pval = 0.0;
                }
                image.putPixelValue(x, y, pval);
            }
        }
        return true;
    }

    public static void drawCross(ImageProcessor image, int x, int y, int size) {
        int radius = (int) Math.ceil(size / 2.0);
        image.drawLine(x - radius, y, x + radius, y);
        image.drawLine(x, y - radius, x, y + radius);
    }

    public static boolean searchNeighbourhood(ImageProcessor image, int x, int y, int val, int radius) {
        int x0 = x - radius;
        if (x0 < 0) {
            x0 = 0;
        }
        int x1 = x + radius;
        if (x1 >= image.getWidth()) {
            x1 = image.getWidth() - 1;
        }
        int y0 = y - radius;
        if (y0 < 0) {
            y0 = 0;
        }
        int y1 = y + radius;
        if (y1 >= image.getHeight()) {
            y1 = image.getHeight() - 1;
        }
        for (int i = x0; i <= x1; i++) {
            for (int j = y0; j <= y1; j++) {
                if (image.getPixel(i, j) == val) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void saveStackAsSeries(ImageStack stack, String directory, String format, DecimalFormat numFormat) {
        int size = stack.getSize();
        for (int s = 0; s < size; s++) {
            IJ.saveAs(new ImagePlus("", stack.getProcessor(s + 1)),
                    format, directory + numFormat.format(s));
        }
    }

    public static byte[] getPixels(ColorProcessor image, int channel) {
        int size = image.getWidth() * image.getHeight();
        byte[][] tempPix = new byte[3][size];
        image.getRGB(tempPix[0], tempPix[1], tempPix[2]);
        return tempPix[channel];
    }

    public static double getPercentileThresh(ImageProcessor image, double thresh) {
        ImageStack stack = new ImageStack(image.getWidth(), image.getHeight());
        stack.addSlice(image);
        return getPercentileThresh(stack, thresh);
    }

    public static double getPercentileThresh(ImageStack image, double thresh) {
        if (image == null) {
            return 0.0;
        }
        ImageStatistics stats;
        if (image.getSize() > 1) {
            ImagePlus imp = new ImagePlus("", image.duplicate());
            (new StackConverter(imp)).convertToGray32();
            stats = new StackStatistics(imp);
        } else {
            stats = new FloatStatistics(new TypeConverter(image.getProcessor(1), false).convertToFloat(null));
        }
        long histogram[] = stats.getHistogram();
        int l = histogram.length;
        int total = image.getWidth() * image.getHeight();
        double sum = 0.0;
        int i = 0;
        while (sum / total < thresh && i < l) {
            sum += histogram[i];
            i++;
        }
        return stats.histMin + i * stats.binSize;
    }

    public static boolean isEdgePixel(int x, int y, int width, int height, int margin) {
        return (x <= margin) || (x >= width - 1 - margin) || (y <= margin) || (y >= height - 1 - margin);
    }

    public static ImageProcessor updateImage(ImageStack channel1, ImageStack channel2, int slice) {
        ImageProcessor redIP = ImageNormaliser.normaliseImage(channel1.getProcessor(slice), 255.0, ImageNormaliser.FLOAT);
        ImageStack red = (new ImagePlus("", (new TypeConverter(redIP, true)).convertToByte())).getImageStack();
        ImageStack green = null;
        if (channel2 != null) {
            ImageProcessor greenIP = ImageNormaliser.normaliseImage(channel2.getProcessor(slice), 255.0, ImageNormaliser.FLOAT);
            green = (new ImagePlus("", (new TypeConverter(greenIP, true)).convertToByte())).getImageStack();
        }
        return ((new RGBStackMerge()).mergeStacks(channel1.getWidth(), channel1.getHeight(), 1, red, green, null, false)).getProcessor(1);
    }

    public static short[][] convertPixToShort(Object[] pix) {
        int n = pix.length;
        short[][] output = new short[n][];
        for (int i = 0; i < n; i++) {
            output[i] = (short[]) pix[i];
        }
        return output;
    }

    public static int[][] convertPixToInt(Object[] pix) {
        int n = pix.length;
        int[][] output = new int[n][];
        for (int i = 0; i < n; i++) {
            if (pix[i] == null) {
                continue;
            }
            byte[] current = (byte[]) pix[i];
            int l = current.length;
            output[i] = new int[l];
            for (int j = 0; j < l; j++) {
                output[i][j] = current[j] & 0xFF;
            }
        }
        return output;
    }
}
