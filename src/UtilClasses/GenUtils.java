/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UtilClasses;

import ij.IJ;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 *
 * @author barry05
 */
public class GenUtils {

    public static int maxline = 200;
    public static String hgTagDir = ".hg/cache/tags";
    /**
     * Greek letter 'mu'
     */
    public static final char mu = '\u00b5';

    /*
     * Create new parent output directory - make sure directory name is unique
     * so old results are not overwritten
     */
    public static String openResultsDirectory(String directory, String delimiter) {
        File newDir = new File(directory + "_Output" + delimiter);
        try {
            int i = 1;
            while (newDir.exists()) {
                newDir = new File(directory + "_Output_(" + i + ")" + delimiter);
                i++;
            }
            if (!newDir.mkdir()) {
                System.err.println("Failed to create output directory.");
                return null;
            }
        } catch (Exception e) {
            System.err.println(e.toString());
            return null;
        }
        return newDir.getAbsolutePath();
    }

    public static String getDelimiter() {
        if (IJ.isMacOSX()) {
            return "//";
        } else {
            return "\\";
        }
    }

    public static void drawRegionWithLabel(ImageProcessor image, Roi roi, String label,
            Rectangle bounds, Color drawColor, int lineWidth, Font font, boolean useIJ) {
        image.setColor(drawColor);
        image.setLineWidth(lineWidth);
        int w = image.getWidth();
        int h = image.getHeight();
        if (useIJ) {
            image.draw(roi);
        } else {
            Polygon poly = roi.getPolygon();
            int n = poly.npoints;
            int xp[] = poly.xpoints;
            int yp[] = poly.ypoints;
            for (int i = 0; i < n; i++) {
                image.drawDot(checkRange(xp[i], w), checkRange(yp[i], h));
            }
        }
        image.setFont(font);
        image.drawString(label, checkRange(bounds.x + bounds.width / 2, w),
                checkRange(bounds.y + bounds.height / 2, h));
    }

    public static int checkRange(int a, int b) {
        return a >= b ? a - b : a;
    }

    /*
     * Returns simple difference estimate of data, approximating first
     * derivative.
     */
    public static double[] differentiate(double[] data) {
        double derivative[] = new double[data.length];
        derivative[0] = 0.0;
        for (int i = 1; i < data.length - 1; i++) {
            derivative[i] = data[i + 1] - data[i - 1];
        }
        derivative[data.length - 1] = 0.0;
        return derivative;
    }

    public static File createDirectory(String dirName) {
        File dir = new File(dirName);
        try {
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    IJ.error("Failed to create directory: " + dirName);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println(e.toString());
            return null;
        }
        return dir;
    }

    public static String checkFileSep(String directory, char sub) {
        String dir;
        dir = directory.replace('\\', sub);
        dir = dir.replace('/', sub);
        dir = dir.replace(':', sub);
        dir = dir.replace('*', sub);
        dir = dir.replace('?', sub);
        dir = dir.replace('"', sub);
        dir = dir.replace('<', sub);
        dir = dir.replace('>', sub);
        dir = dir.replace('|', sub);

        return dir;
    }

    public static void error(String message) {
        Toolkit.getDefaultToolkit().beep();
        IJ.error(message);
    }

    public static void showDone(Object caller) {
        if (IJ.getInstance() != null) {
            IJ.showStatus(caller.getClass().getName() + ": Done.");
        }
    }

    public static double[][][] readData(int rows, int cols, File[] input, String delimiter) {
        int numOfFiles = input.length;
        double[][][] output = new double[numOfFiles][rows][cols];
        for (int i = 0; i < numOfFiles; i++) {
            int rowindex = 0;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input[i])))) {
                String line = br.readLine();
                while (line != null) {
                    int colindex = 0;
                    Scanner scan = new Scanner(line).useDelimiter(delimiter);
                    while (scan.hasNextDouble()) {
                        output[i][rowindex][colindex] = scan.nextDouble();
                        colindex++;
                    }
                    line = br.readLine();
                    rowindex++;
                }
                br.close();
            } catch (Exception e) {
                IJ.error(e.toString());
            }
        }
        return output;
    }
}
