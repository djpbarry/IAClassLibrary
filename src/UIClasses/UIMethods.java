/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UIClasses;

import ij.process.ImageProcessor;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JDialog;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class UIMethods {

    public static double getMagnification(ImageProcessor image, Canvas canvas) {
        int iWidth = image.getWidth();
        int iHeight = image.getHeight();
        int cWidth = canvas.getWidth();
        int cHeight = canvas.getHeight();
        double hScale = ((double) iHeight) / cHeight;
        double wScale = ((double) iWidth) / cWidth;
        return Math.max(hScale, wScale);
    }

    public static void centreDialog(JDialog dialog) {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation(dim.width / 2 - dialog.getWidth() / 2, dim.height / 2 - dialog.getHeight() / 2);
    }
}
