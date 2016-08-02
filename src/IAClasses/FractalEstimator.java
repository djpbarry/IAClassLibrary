package IAClasses;

import ij.IJ;
import ij.ImageStack;
import ij.measure.CurveFitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Color;

public class FractalEstimator {

    private int foreground, background;
    private double threeDF;
    private static final double MAX_GREY = 255.0;

    public FractalEstimator() {
    }

    public double[] do2DEstimate(ImageProcessor image) {
        int epsilonMin = 2;
        int width = image.getWidth();
        int height = image.getHeight();
        int epsilonMax = (int) Math.round(Math.max(width, height) / 4.5);
        int step = (int) Math.round((epsilonMax - epsilonMin + 1) / 10.0);
        int points = (int) Math.ceil((epsilonMax - epsilonMin) / (step - 1));
        if (points < 3) {
            return null;
        }
        int epsilon, j, k, j0, k0, x, y, massCount, surfaceCount;
        boolean mass, surface;
        double xCentre = width / 2.0;
        double yCentre = height / 2.0;
        double dbmCounts2D[] = new double[points];
        double dbsCounts2D[] = new double[points];
        double logE[] = new double[points];
        ImageStack stack = new ImageStack(width, height);
        stack.addSlice(image);
        int[] inputPix = Utils.convertPixToInt(stack.getImageArray())[0];
        if (image.isInvertedLut()) {
            foreground = 255;
            background = 0;
        } else {
            foreground = 0;
            background = 255;
        }
        for (epsilon = epsilonMin; epsilon <= epsilonMax; epsilon += step) {
            j0 = (int) Math.round((2.0 * xCentre - epsilon) / 2.0);
            int index = (epsilon - epsilonMin) / step;
            while (j0 > 0) {
                j0 -= epsilon;
            }
            k0 = (int) Math.round((2.0 * yCentre - epsilon) / 2.0);
            while (k0 > 0) {
                k0 -= epsilon;
            }
            logE[index] = Math.log(epsilon);
            for (j = j0, massCount = 0, surfaceCount = 0; j < width - j0; j += epsilon) {
                for (k = k0; k < height - k0; k += epsilon) {
                    for (y = k < 0 ? 0 : k, mass = false, surface = false; y < k + epsilon && y < height; y++) {
                        int offset = y * width;
                        for (x = j < 0 ? 0 : j; x < j + epsilon && x < width; x++) {
                            if (inputPix[offset + x] == foreground) {
                                mass = true;
                                if (!surface && edge(x, y, height, width, inputPix)) {
                                    surface = true;
                                }
                            }
                        }
                    }
                    if (mass) {
                        massCount++;
                    }
                    if (surface) {
                        surfaceCount++;
                    }
                }
            }
            dbmCounts2D[index] = Math.log(massCount);
            dbsCounts2D[index] = Math.log(surfaceCount);
        }
        double dims[] = new double[2];
        CurveFitter fitter = new CurveFitter(logE, dbmCounts2D);
        fitter.doFit(CurveFitter.STRAIGHT_LINE);
        dims[0] = -(fitter.getParams())[1];
        fitter = new CurveFitter(logE, dbsCounts2D);
        fitter.doFit(CurveFitter.STRAIGHT_LINE);
        dims[1] = -(fitter.getParams())[1];
        if (fitter.getRSquared() < 0.9) {
            return null;
        } else {
            return dims;
        }
    }

    public boolean do3DEstimate(double data[], int min, int max) {
        int x;
        int width = data.length;
        if (width < 20) {
            return false;
        }
        ByteProcessor profile = new ByteProcessor(width, max);
        profile.setColor(Color.white);
        profile.fill();
        profile.setColor(Color.black);
        for (x = 0; x < width - 1; x++) {
            profile.drawLine(x, (int) Math.round(255.0 - data[x]), x + 1, (int) Math.round(255.0 - data[x + 1]));
        }
        do2DEstimate(profile);
        return true;
    }

    public static double doSurfaceEstimate(Region region, ImageProcessor image, String plotTitle) {
        int width = image.getWidth(), height = image.getHeight();
        int epsilonMin = 2;
        int maxDim = Math.max(width, height);
        int epsilonMax = (int) Math.round(Math.max(width, height) / 4.5);
        int step = (int) Math.round((epsilonMax - epsilonMin + 1) / 10.0);
        int points = (int) Math.ceil((epsilonMax - epsilonMin) / (step - 1));
        if (points < 3) {
            return Double.NaN;
        }
//        ImageStatistics stats = image.getStatistics();
//        double imageMax = stats.max;
//        double imageMin = stats.min;
        double xCentre = width / 2.0;
        double yCentre = height / 2.0;
        int l = epsilonMax - epsilonMin + 1;
        double logE[] = new double[l];
        double boxCounts[] = new double[l];
        for (int epsilon = epsilonMin; epsilon <= epsilonMax; epsilon++) {
            double h = MAX_GREY * epsilon / maxDim;
            int i0 = (int) Math.round((2.0 * xCentre - epsilon) / 2.0);
            while (i0 > 0) {
                i0 -= epsilon;
            }
            int j0 = (int) Math.round((2.0 * yCentre - epsilon) / 2.0);
            while (j0 > 0) {
                j0 -= epsilon;
            }
            logE[epsilon - epsilonMin] = Math.log(epsilon);
            double massCount = 0.0;
            for (int i = i0; i <= width - i0; i += epsilon) {
                for (int j = j0; j <= height - j0; j += epsilon) {
                    double min = Double.MAX_VALUE;
                    double max = Double.MIN_VALUE;
                    for (int x = (i < 0) ? 0 : i; x <= i + epsilon && x < width; x++) {
                        for (int y = (j < 0) ? 0 : j; y <= j + epsilon && y < height; y++) {
                            double pix = image.getPixelValue(x, y);
                            if (pix < min) {
                                min = pix;
                            }
                            if (pix > max) {
                                max = pix;
                            }
                        }
                    }
                    if (min < Double.MAX_VALUE && max > Double.MIN_VALUE) {
                        massCount += Math.floor((max - min) / h) + 1.0;
                    }
                }
            }
            boxCounts[epsilon - epsilonMin] = Math.log(massCount);
        }
        CurveFitter fitter = new CurveFitter(logE, boxCounts);
        fitter.doFit(CurveFitter.STRAIGHT_LINE);
        double D = -(fitter.getParams()[1]);
        IJ.log(plotTitle + " D: " + D);
//        Plot plot = new Plot(plotTitle, "log[Epsilon]", "log[Box Counts]", logE, boxCounts);
//        plot.show();
        return D;
    }

    boolean edge(int x, int y, int height, int width, int[] imagePix) {
        for (int j = y - 1 < 0 ? 0 : y - 1; j <= y + 1 && j < height; j++) {
            int offset = j * width;
            for (int i = x - 1 < 0 ? 0 : x - 1; i <= x + 1 && i < width; i++) {
                if (imagePix[offset + i] == background) {
                    return true;
                }
            }
        }
        return false;
    }

    public double getThreeDF() {
        return threeDF;
    }
}
