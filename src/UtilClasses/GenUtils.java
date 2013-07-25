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
import java.awt.Rectangle;
import java.io.*;

/**
 *
 * @author barry05
 */
public class GenUtils {

    public static int maxline = 200;
    public static String hgTagDir = ".hg/cache/tags";

    public static int getRevisionNumber(String fileLocation, int maxline) {
        File inputFile = new File(fileLocation);
        InputStreamReader reader;
        try {
            reader = new InputStreamReader(new FileInputStream(inputFile));
        } catch (FileNotFoundException e) {
            IJ.error("File not found error: " + e.toString());
            return -1;
        }
        char charBuffer[] = new char[maxline];
        try {
            reader.read(charBuffer);
        } catch (IOException e) {
            IJ.error("Error reading from file: " + e.toString());
            return -1;
        }
        int i = 0;
        while (charBuffer[i] != ' ') {
            i++;
        }
        return Integer.parseInt((new String(charBuffer)).substring(0, i));
    }

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

    public static void drawRegionWithLabel(ImageProcessor image, Roi velroi, String label,
            Rectangle bounds, Color drawColor, int lineWidth, Font font) {
        image.setColor(drawColor);
        image.setLineWidth(lineWidth);
        image.draw(velroi);
        image.setFont(font);
        image.drawString(label, bounds.x + bounds.width / 2,
                bounds.y + bounds.height / 2);
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
                if (!dir.mkdir()) {
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
}
