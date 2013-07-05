package IAClasses;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;

public class Utils {

    /**
     * Constructs an {@link ImageStack} from an image sequence stored in
     * <code>directory</code>.
     *
     * @return the constructed {@link ImageStack}.
     */
    public static ImageStack buildStack(File directory) {
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
        ImageStack output = new ImageStack();
        for (i = 0; i < noOfImages; i++) {
            currentImage = new ImagePlus(images[i].getPath());
            processor = currentImage.getProcessor();
            if (processor != null) {
                if (output.getSize() < 1) {
                    output = new ImageStack(processor.getWidth(), processor.getHeight());
                }
                output.addSlice("" + i, processor);
            }
        }
        return output;
    }

    /**
     * Copies pixel values from a square area specified by
     * <code>(xc - r, yc - r, xc + r, yc + r)</code> from
     * <code>image</code> into
     * <code>values</code>.
     *
     * @param xCoords x-coordinates of the pixels will be copied in here
     * @param yCoords y-coordinates of the pixels will be copied in here
     * @param values the
     * <code>2 * r + 1 x 2 * r + 1</code> array into which pixel values will be
     * copied
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
     * Searches within
     * <code>radius</code> pixels of (
     * <code>x</code>,
     * <code>y</code>) in
     * <code>image</code> for a grey level equal to
     * <code>value</code>. If more than one suitable candidate is identified,
     * that nearest to
     * <code>(x, y)</code> is returned.
     *
     * @return the co-ordinates of the point (in the form
     * <code>{x, y}</code>) if a pixel of
     * <code>value</code> was located,
     * <code>null</code> otherwise.
     */
    public static int[][] searchNeighbourhood(int x, int y, int radius, int value,
            ImageProcessor image) {
        if (image == null || x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return null;
        }
        ArrayList pixels = new ArrayList();
        double currentDist, minDist = Double.MAX_VALUE;

        for (int i = x - radius; i <= x + radius; i++) {
            for (int j = y - radius; j <= y + radius; j++) {
                if (image.getPixel(i, j) == value) {
                    currentDist = calcDistance(i, j, x, y);
                    if (currentDist < minDist) {
                        Pixel p = new Pixel(i, j, currentDist, 0);
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
                points[l][0] = ((Pixel) pixels.get(minIndex)).getX();
                points[l][1] = ((Pixel) pixels.get(minIndex)).getY();
                pixels.remove(minIndex);
                l++;
            }
            return points;
        }
        return null;
    }

    public static ByteProcessor findLocalMaxima(int kWidth, int kHeight, int drawValue,
            ImageProcessor image, double maxThresh, boolean varyBG) {
        if (image == null) {
            return null;
        }
        int i, j, x, y, width = image.getWidth(), height = image.getHeight();
        double max, current, min;
        ByteProcessor bproc = new ByteProcessor(width, height);
        bproc.setValue(drawValue);
        for (x = kWidth; x < width - kWidth; x++) {
            for (y = kHeight; y < height - kHeight; y++) {
                for (min = Double.MAX_VALUE, max = 0.0, i = x - kWidth; i <= x + kWidth; i++) {
                    for (j = y - kHeight; j <= y + kHeight; j++) {
                        current = image.getPixelValue(i, j);
                        if ((current > max) && !((x == i) && (y == j))) {
                            max = current;
                        }
                        if ((current < min) && !((x == i) && (y == j))) {
                            min = current;
                        }
                    }
                }
                double pix = image.getPixelValue(x, y);
                double diff;
                if (varyBG) {
                    diff = pix - min;
                } else {
                    diff = pix;
                }
                if ((pix > max) && (diff > maxThresh)) {
                    bproc.drawPixel(x, y);
                }
            }
        }
        return bproc;
    }

    public static ArrayList<int[]> findLocalMaxima(int kWidth, int kHeight,
            ImageProcessor image, double maxThresh, boolean varyBG) {
        if (image == null) {
            return null;
        }
        int i, j, x, y, width = image.getWidth(), height = image.getHeight();
        double max, current, min;
        ArrayList maxima = new ArrayList<int[]>();
        for (x = kWidth; x < width - kWidth; x++) {
            for (y = kHeight; y < height - kHeight; y++) {
                for (min = Double.MAX_VALUE, max = 0.0, i = x - kWidth; i <= x + kWidth; i++) {
                    for (j = y - kHeight; j <= y + kHeight; j++) {
                        current = image.getPixelValue(i, j);
                        if ((current > max) && !((x == i) && (y == j))) {
                            max = current;
                        }
                        if ((current < min) && !((x == i) && (y == j))) {
                            min = current;
                        }
                    }
                }
                double pix = image.getPixelValue(x, y);
                double diff;
                if (varyBG) {
                    diff = pix - min;
                } else {
                    diff = pix;
                }
                if ((pix > max) && (diff > maxThresh)) {
                    int thismax[] = {x, y};
                    maxima.add(thismax);
                }
            }
        }
        return maxima;
    }

    /**
     * Returns the distance between
     * <code>{x1, y1}</code> and
     * <code>{x2, y2}</code>.
     */
    public static double calcDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
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
     * @param xValues a vector of length
     * <code>n</code>.
     * @param yValues a vector of length
     * <code>m</code>.
     * @param xSum the sum of all entries in
     * <code>xValues</code>.
     * @param ySum the sum of all entries in
     * <code>yValues</code>.
     * @return the covariance matrix for
     * <code>xValues</code> and
     * <code>yValues</code>, or
     * <code>null</code> if
     * <code>m != n</code>.
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
     * @param variable1 a vector of length
     * <code>n</code>.
     * @param variable2 a vector of length
     * <code>m</code>.
     * @param mean1 the mean of the elements of
     * <code>variable1</code>.
     * @param mean2 the mean of the elements of
     * <code>variable2</code>.
     * @return the covariance of
     * <code>variable1</code> and
     * <code>variable2</code>, or
     * <code>Double.Nan</code> if
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
     * @return the eigenvalues of
     * <code>matrix</code> in the form of an array with two elements.
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
     * Converts each entry in a
     * <code>byte</code> array to a
     * <code>float</code>.
     *
     * @param array an array of
     * <code>byte</code>s.
     * @return an array of
     * <code>float</code>s.
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
            outVal = Double.NaN;
        }

        return outVal;
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

    public static ByteProcessor normaliseToByte(ImageProcessor original) {
        ImageProcessor normalised = (new TypeConverter(original, true)).convertToFloat(null);
        double max = normalised.getMax();
        normalised.multiply(255.0);
        normalised.multiply(1.0 / max);
        normalised.resetMinAndMax();
        return (ByteProcessor) ((new TypeConverter(normalised, true)).convertToByte());
    }

    /*
     * Normalise all images in the given stack
     */
    public static void normaliseStack(int stackLength, ImageStack stack) {
        for (int i = 0; i < stackLength; i++) {
            ImageProcessor cip = stack.getProcessor(1);
            cip = (ImageProcessor) (Utils.normaliseToByte(cip));
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
}
