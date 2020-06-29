/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.calm.iaclasslibrary.UIClasses;

import ij.process.ImageProcessor;
import java.awt.Canvas;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;

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

    public static void centreContainer(Container frame) {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(dim.width / 2 - frame.getWidth() / 2, dim.height / 2 - frame.getHeight() / 2);
    }
}
