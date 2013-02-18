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
}
