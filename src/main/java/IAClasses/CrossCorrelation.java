/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package IAClasses;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class CrossCorrelation {

    public static double crossCorrelation(float map1[], float map2[], int t, int s, double mean1, double mean2, int w, int h) {
        double sum = 0.0;
        int count = 0;
        int y1 = s < 0 ? -s : 0;
        int y2 = s > 0 ? h - s : h;
        int x1 = t < 0 ? -t : 0;
        int x2 = t > 0 ? w - t : w;
        for (int j = y1; j < y2; j++) {
            int offset1 = j * w;
            int offset2 = (j + s) * w;
            for (int i = x1; i < x2; i++) {
                sum += (map1[i + t + offset2] - mean1) * (map2[i + offset1] - mean2);
                count++;
            }
        }
        return sum / count;
    }

    /**
     * Computes the cross-correlation of two maps
     *
     * @param mapa MorphMap in the form of an
     * {@link ij.process.ImageProcessor ImageProcessor}
     * @param mapb MorphMap in the form of an
     * {@link ij.process.ImageProcessor ImageProcessor}
     * @param newsize maps are resized to this square dimension to speed
     * calculation
     * @return an {@link ij.ImagePlus ImagePlus} representing the
     * cross-correlation of the two maps
     */
    public static ImagePlus periodicity2D(ImageProcessor mapa, ImageProcessor mapb, int newsize) {
        ImageProcessor map1 = mapa.resize(newsize, newsize, true);
        ImageProcessor map2 = mapb.resize(newsize, newsize, true);
        double sd2 = map2.getStatistics().stdDev * map1.getStatistics().stdDev;
        int w = map1.getWidth();
        int h = map1.getHeight();
        int T = w;
        int S = h;
        int midX = T / 2;
        int midY = S / 2;
        double mean1 = map1.getStatistics().mean;
        double mean2 = map2.getStatistics().mean;
        float map1Pix[] = (float[]) map1.getPixels();
        float map2Pix[] = (float[]) map2.getPixels();
        FloatProcessor crossCorrelation = new FloatProcessor(T, S);
        crossCorrelation.putPixelValue(midX, midY, 1.0);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < T; x++) {
                double c = crossCorrelation(map1Pix, map2Pix, x - midX, y - midY, mean1, mean2, w, h) / sd2;
                crossCorrelation.putPixelValue(x, y, c);
            }
        }
        return new ImagePlus("2D AutoCorrelation", crossCorrelation);
    }

}
