/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UtilClasses;

import ij.IJ;
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
                newDir = new File(directory + "_Output (" + i + ")" + delimiter);
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
}
