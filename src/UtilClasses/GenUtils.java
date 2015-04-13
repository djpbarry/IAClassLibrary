/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UtilClasses;

import UIClasses.SpecifyInputsDialog;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.*;
import java.util.ArrayList;
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
            if (!newDir.mkdirs()) {
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
        if (IJ.isWindows()) {
            return "\\";
        } else {
            return "/";
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
        if (a < 0) {
            return a + b;
        } else if (a >= b) {
            return a - b;
        } else {
            return a;
        }
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

    public static ArrayList<double[]>[] readData(int cols, File[] input, String delimiter) {
        int numOfFiles = input.length;
        ArrayList<double[]>[] output = new ArrayList[numOfFiles];
        for (int i = 0; i < numOfFiles; i++) {
            output[i] = new ArrayList();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input[i])));
                String line = br.readLine();
                while (line != null) {
                    output[i].add(new double[cols]);
                    int colindex = 0;
                    Scanner scan = new Scanner(line);
                    while (scan.hasNextDouble()) {
                        (output[i].get(output[i].size() - 1))[colindex] = scan.nextDouble();
                        colindex++;
                    }
                    line = br.readLine();
                }
                br.close();
            } catch (IOException e) {
                IJ.error(e.toString());
            }
        }
        return output;
    }

    public static ImagePlus[] specifyInputs(String[] labels) {
        ImagePlus outputs[] = new ImagePlus[2];
        int windIDs[] = WindowManager.getIDList();
        if (windIDs == null) {
            error("No images open.");
            return null;
        }
        String winTitles[] = new String[windIDs.length + 1];
        for (int i = 0; i < windIDs.length; i++) {
            winTitles[i] = WindowManager.getImage(windIDs[i]).getTitle();
        }
        winTitles[windIDs.length] = "None";
        SpecifyInputsDialog sid = new SpecifyInputsDialog(null, true, winTitles, labels);
        sid.setVisible(true);
        if (!(sid.getReturnStatus() == SpecifyInputsDialog.RET_OK)) {
            return null;
        }
        if (!(sid.getCytoIndex() > -1 && sid.getCytoIndex() < windIDs.length)) {
            Toolkit.getDefaultToolkit().beep();
            error("Channel 1 not specified!");
            return null;
        }
        outputs[0] = WindowManager.getImage(windIDs[sid.getCytoIndex()]);
        if (sid.getSigIndex() > -1 && sid.getSigIndex() < windIDs.length) {
            outputs[1] = WindowManager.getImage(windIDs[sid.getSigIndex()]);
        } else {
            outputs[1] = null;
        }
        return outputs;
    }

    public static ImageStack convertStackTo8Bit(ImageStack stack) {
        ImageStack tempStack = new ImageStack(stack.getWidth(), stack.getHeight());
        int size = stack.getSize();
        for (int i = 1; i <= size; i++) {
            tempStack.addSlice(stack.getProcessor(i).duplicate());
        }
        ImagePlus tempCytoImp = new ImagePlus("", tempStack);
        StackConverter sc = new StackConverter(tempCytoImp);
        sc.convertToGray8();
        return tempCytoImp.getImageStack();
    }
}
