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
package Extrema;

import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.StackConverter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedMaximaFinder extends MultiThreadedProcess {

    private final ArrayList<int[]> maxima;
    private int[] radii;
    private ImageStack stack;
    private float thresh;
    private final boolean varyBG;
    private final boolean absolute;

    public MultiThreadedMaximaFinder(BioFormatsImg img, Properties props, String[] propLabels) {
        super(img, props, propLabels);
        maxima = new ArrayList();
        varyBG = true;
        absolute = true;
    }

    public MultiThreadedMaximaFinder(BioFormatsImg img, int[] radii, float thresh, boolean[] criteria, Properties props) {
        super(img, props, null);
        this.radii = radii;
        this.thresh = thresh;
        this.varyBG = criteria[0];
        this.absolute = criteria[1];
        this.maxima = new ArrayList();
    }

    protected void setup() {
        int series = Integer.parseInt(props.getProperty(propLabels[0]));
        radii = getIntSigma(series, propLabels[1], propLabels[1], propLabels[2]);
        thresh = Float.parseFloat(props.getProperty(propLabels[3]));
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public ImagePlus makeLocalMaximaImage(byte background) {
        ImagePlus imp = img.getTempImg();
        (new StackConverter(imp)).convertToGray32();
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImageStack output = new ImageStack(width, height);
        byte foreground = (byte) ~background;
        for (int n = 0; n < imp.getNSlices(); n++) {
            ByteProcessor bp = new ByteProcessor(width, height);
            bp.setValue(background);
            bp.fill();
            output.addSlice(bp);
        }
        Object[] stackPix = output.getImageArray();
        for (int[] pix : maxima) {
            ((byte[]) stackPix[pix[2]])[pix[0] + pix[1] * width] = foreground;
        }
        return new ImagePlus(String.format("%s - Local Maxima", imp.getTitle()), output);
    }

    public void run() {
        setup();
        this.stack = img.getTempImg().getImageStack();
        if (stack == null) {
            return;
        }
        int width = stack.getWidth();
        int height = stack.getHeight();
        int depth = stack.getSize();
        float[][] stackPix = img.getTempStackPixels();
        int threadCount = 0;
        for (int z = radii[2]; z < depth - radii[2]; z++) {
            for (int x = radii[0]; x < width - radii[0]; x++) {
                for (int y = radii[1]; y < height - radii[1]; y++) {
                    exec.submit(new RunnableMaximaFinder(stackPix, varyBG, absolute,
                            thresh, maxima, new int[]{x, y, z}, new int[]{width, height, depth},
                            radii, "MaxFinder-" + threadCount++));
                }
            }
        }
        exec.shutdown();
        try {
            exec.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            GenUtils.logError(e, "Error detecting local maxima.");
            return;
        }
        ImagePlus maxima = makeLocalMaximaImage((byte) 0);
//        maxima.show();
        img.setTempImg(maxima);
        IJ.log("Maxima finder done.");
    }

    public ArrayList<int[]> getMaxima() {
        return maxima;
    }

}
