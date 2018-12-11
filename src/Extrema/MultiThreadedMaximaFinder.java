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
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.LogDetector;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedMaximaFinder extends MultiThreadedProcess {

    private ArrayList<int[]> maxima;
    private int[] radii;
    private double[] calibration;
    private ImageStack stack;
    private float thresh;
    private boolean varyBG;
    private boolean absolute;

    public MultiThreadedMaximaFinder(MultiThreadedProcess[] inputs) {
        super(inputs);
    }

    public MultiThreadedMaximaFinder(int[] radii, float thresh, boolean[] criteria) {
        super(null);
        this.radii = radii;
        this.thresh = thresh;
        this.varyBG = criteria[0];
        this.absolute = criteria[1];
        this.maxima = new ArrayList();
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.propLabels = propLabels;
        this.props = props;
        maxima = new ArrayList();
        varyBG = true;
        absolute = true;
        int series = Integer.parseInt(props.getProperty(propLabels[0]));
        int channel = Integer.parseInt(props.getProperty(propLabels[1]));
        calibration = getCalibration(series);
        radii = getUncalibratedIntSigma(series, propLabels[2], propLabels[2], propLabels[2]);
        thresh = Float.parseFloat(props.getProperty(propLabels[3]));
        img.loadPixelData(series, channel, channel + 1, null);
    }

    public ImagePlus makeLocalMaximaImage(byte background, int radius) {
        ImagePlus imp = img.getLoadedImage();
//        (new StackConverter(imp)).convertToGray32();
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
//            ImageProcessor slice = output.getProcessor(pix[2] + 1);
//            slice.setValue(255);
//            slice.setLineWidth(3);
//            slice.drawOval(pix[0] - radius, pix[1] - radius, 2 * radius + 1, 2 * radius + 1);
            ((byte[]) stackPix[pix[2]])[pix[0] + pix[1] * width] = foreground;
        }
        return new ImagePlus(String.format("%s - Local Maxima", imp.getTitle()), output);
    }

    public void run() {
        ImagePlus imp = img.getLoadedImage();

        this.stack = imp.getImageStack();
        if (stack == null) {
            return;
        }
        IJ.log(String.format("Searching for blobs %d pixels in diameter above a threshold of %f...", (2 * radii[0]), thresh));
        long[] min = new long[]{0, 0, 0};
        long[] max = new long[]{stack.getWidth() - 1, stack.getHeight() - 1, stack.getSize() - 1};
        Img<FloatType> sip = ImagePlusAdapter.wrap(imp);
        LogDetector<FloatType> log = new LogDetector<FloatType>(Views.interval(sip, min, max),
                Intervals.createMinMax(min[0], min[1], min[2], max[0], max[1], max[2]),
                calibration, radii[0], thresh, false, false);
        log.setNumThreads();
        log.process();
        List<Spot> maximas = log.getResult();
        IJ.log(String.format("Found %d blobs.", maximas.size()));
        for (Spot s : maximas) {
            int[] pos = new int[3];
            for (int d = 0; d < 3; d++) {
                pos[d] = (int) Math.round(s.getFloatPosition(d) / calibration[d]);
            }
            maxima.add(pos);
        }
        output = makeLocalMaximaImage((byte) 0, (int) Math.round(radii[0] / calibration[0]));
    }

    public ArrayList<int[]> getMaxima() {
        return maxima;
    }

    public MultiThreadedMaximaFinder duplicate() {
        MultiThreadedMaximaFinder newProcess = new MultiThreadedMaximaFinder(inputs);
        this.updateOutputDests(newProcess);
        return newProcess;
    }

}
