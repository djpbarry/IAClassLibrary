package IAClasses;

import ij.measure.CurveFitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Rectangle;

public class FractalEstimator {

    private int foreground, background;
    private double threeDF;

    public FractalEstimator() {
    }

    public double[] do2DEstimate(ImageProcessor image) {
        int epsilonMin = 2, step = 1;
        int width = image.getWidth();
        int height = image.getHeight();
        int epsilonMax = (int) Math.round(Math.max(width, height) / 4.5);
        int points = epsilonMax - epsilonMin + 1;
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

        if (image.isInvertedLut()) {
            foreground = 255;
            background = 0;
        } else {
            foreground = 0;
            background = 255;
        }

        for (epsilon = epsilonMin; epsilon <= epsilonMax; epsilon += step) {
            j0 = (int) Math.round((2.0 * xCentre - epsilon) / 2.0);
            while (j0 > 0) {
                j0 -= epsilon;
            }
            k0 = (int) Math.round((2.0 * yCentre - epsilon) / 2.0);
            while (k0 > 0) {
                k0 -= epsilon;
            }
            logE[epsilon - epsilonMin] = Math.log(epsilon);
            for (j = j0, massCount = 0, surfaceCount = 0; j < width - j0; j += epsilon) {
                for (k = k0; k < height - k0; k += epsilon) {
                    for (x = j, mass = false, surface = false; x < j + epsilon; x++) {
                        for (y = k; y < k + epsilon; y++) {
                            if (y >= 0 && y < height && x >= 0 && x < width && image.getPixel(x, y) == foreground) {
                                mass = true;
                                if (edge(x, y, image)) {
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
            dbmCounts2D[epsilon - epsilonMin] = Math.log(massCount);
            dbsCounts2D[epsilon - epsilonMin] = Math.log(surfaceCount);
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

    public static double[] doSurfaceEstimate(Region region, ImageProcessor refImage) {
        Rectangle r = region.getBounds();
        int width = r.width, height = r.height;
        double D[] = new double[3];
        int epsilonMin = 2;
        int maxDim = Math.max(width, height);
        double imageMax = region.getMax();
        double imageMin = region.getMin();
        int epsilonMax = 11;
        double xCentre = width / 2.0;
        double yCentre = height / 2.0;
        int l = epsilonMax - epsilonMin + 1;
        double logE[] = new double[l];
        double boxCounts[] = new double[l];
        int histogram[] = region.getHistogram();
        int G = 0;
        for (int g = 1; g < 256; g++) {
            if (histogram[g] > 0) {
                G++;
            }
        }
        double data[][] = region.getDataArray(refImage);
        for (int epsilon = epsilonMin; epsilon <= epsilonMax; epsilon++) {
            double h = (imageMax - imageMin) / (G * epsilon / maxDim);
            int i0 = (int) Math.round((2.0 * xCentre - epsilon) / 2.0);
            while (i0 > 0) {
                i0 = i0 - epsilon;
            }
            int j0 = (int) Math.round((2.0 * yCentre - epsilon) / 2.0);
            while (j0 > 0) {
                j0 = j0 - epsilon;
            }
            logE[epsilon - epsilonMin] = Math.log(epsilon);
            double massCount = 0.0;
            for (int i = i0; i <= width - i0; i += epsilon) {
                for (int j = j0; j <= height - j0; j += epsilon) {
                    double min = Double.MAX_VALUE;
                    double max = Double.MIN_VALUE;
                    int x = (i < 0) ? 0 : i;
                    for (; x <= i + epsilon && x < width; x++) {
                        int y = (j < 0) ? 0 : j;
                        for (; y <= j + epsilon && y < height; y++) {
                            if (data[x][y] != Double.NaN) {
                                if (data[x][y] < min) {
                                    min = data[x][y];
                                }
                                if (data[x][y] > max) {
                                    max = data[x][y];
                                }
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
        D[2] = -(fitter.getParams()[1]);
        double lowCounts[] = {boxCounts[0], boxCounts[1], boxCounts[2]};
        double lowLogE[] = {logE[0], logE[1], logE[2]};
        double midCounts[] = {boxCounts[2], boxCounts[3], boxCounts[4], boxCounts[5]};
        double midlogE[] = {logE[2], logE[3], logE[4], logE[5]};
        fitter = new CurveFitter(midlogE, midCounts);
        fitter.doFit(CurveFitter.STRAIGHT_LINE);
        D[1] = -(fitter.getParams()[1]);
        fitter = new CurveFitter(lowLogE, lowCounts);
        fitter.doFit(CurveFitter.STRAIGHT_LINE);
        D[0] = -(fitter.getParams()[1]);
        return D;
    }

    boolean edge(int x, int y, ImageProcessor image) {
        int i, j;

        for (i = x - 1; i <= x + 1; i++) {
            for (j = y - 1; j <= y + 1; j++) {
                if (image.getPixel(i, j) == background) {
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
