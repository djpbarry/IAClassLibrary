/*
 * Copyright (C) 2018 David Barry <david.barry at crick dot ac dot uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package Curvature;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class CurvatureEstimator {

    private int window = 25;
    private int minSize = 60;

//    public static void main(String[] args) {
//        (new CurvatureEstimator()).run();
//        System.exit(1);
//    }

    public CurvatureEstimator() {

    }

    public void run() {
        ImageProcessor ip = IJ.openImage().getProcessor();
        int width = ip.getWidth();
        int height = ip.getHeight();
        ImageProcessor output = new FloatProcessor(width, height);
        output.setValue(0.0);
        output.fill();
        int nProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println(String.format("%d processsors available", nProcessors));
        try {
            ExecutorService exec = Executors.newFixedThreadPool(nProcessors);
            for (int y = window; y < height - window; y++) {
                System.out.println(String.format("Line %d of %d...", (y - window), (height - window)));
                for (int x = window; x < width - window; x++) {
                    exec.submit(new CurveAnalyser(x, y, ip, output));
                }
            }
            exec.shutdown();
            exec.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            System.out.println(String.format("An exception occured - aborting: %s", e.toString()));
            return;
        }
//        IJ.saveAs(new ImagePlus("", output), "TIF", "C:\\Users\\barryd\\debugging\\curve_estimation\\curveEstimation");
    }

    private class CurveAnalyser implements Runnable {

        private int x, y;
        private ImageProcessor ip;
        private ImageProcessor output;

        public CurveAnalyser(int x, int y, ImageProcessor ip, ImageProcessor output) {
            this.x = x;
            this.y = y;
            this.ip = ip;
            this.output = output;
        }

        public void run() {
            System.out.println(String.format("%d %d", x, y));
            ArrayList<WeightedObservedPoint> coords = new ArrayList();
            if (ip.getPixel(x, y) > 0.0) {
                for (int j = y - window; j <= y + window; j++) {
                    for (int i = x - window; i <= x + window; i++) {
                        if (ip.getPixel(i, j) > 0.0) {
                            coords.add(new WeightedObservedPoint(ip.getPixelValue(i, j), i, j));
                        }
                    }
                }
                if (coords.size() > minSize) {
                    final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
                    final double[] coeff = fitter.fit(coords);
                    double kappa = Math.abs(2.0 * coeff[2]) / Math.pow(1.0 + Math.pow(2.0 * x * coeff[2] + coeff[1], 2.0), 3.0 / 2.0);
                    output.putPixelValue(x, y, kappa);
                }
            }
        }
    }
}
