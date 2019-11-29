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

import Cell3D.SpotFeatures;
import Process.Filtering.MultiThreadedHessian;
import IAClasses.Utils;
import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import Stacks.StackMath;
import Stacks.StackThresholder;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.LogDetector;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.GaussianBlur3D;
import ij.plugin.ImageCalculator;
import ij.plugin.SubstackMaker;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.StackConverter;
import ij.process.StackProcessor;
import ij.process.StackStatistics;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageLabeller;
import mcib3d.image3d.distanceMap3d.EDT;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedMaximaFinder extends MultiThreadedProcess {

    public static int BLOB_DETECT = 6;
    public static int HESSIAN_START_SCALE = 4;
    public static int BLOB_SIZE = 1;
    public static int CHANNEL_SELECT = 0;
    public static int HESSIAN_STOP_SCALE = 5;
    public static int BLOB_THRESH = 2;
    public static int EDM_THRESH = 3;
    public static int SERIES_SELECT = 7;
    public static int HESSIAN_DETECT = 8;
    public static int EDM_FILTER = 9;
    public static int HESSIAN_SCALE_STEP = 10;
    public static int HESSIAN_ABS = 11;
    public static int N_PROP_LABELS = 12;

    private ArrayList<int[]> maxima;
    private List<Spot> spotMaxima;
    private double[] radii;
    private double[] calibration;
    private ImageStack stack;
    private float thresh;
    private int series;
    private int channel;
    public static short BACKGROUND = 0;
    private Roi[] edmThresholdOutline;
    private Objects3DPopulation hessianObjects;

    public MultiThreadedMaximaFinder(MultiThreadedProcess[] inputs) {
        super(inputs);
    }

    public MultiThreadedMaximaFinder(double[] radii, float thresh) {
        super(null);
        this.radii = radii;
        this.thresh = thresh;
        this.maxima = new ArrayList();
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.propLabels = propLabels;
        this.props = props;
    }

    public ImagePlus makeLocalMaximaImage(short background, int radius) {
        ImagePlus imp = img.getLoadedImage();
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImageStack output = new ImageStack(width, height);
        for (int n = 0; n < imp.getNSlices(); n++) {
            ShortProcessor sp = new ShortProcessor(width, height);
            sp.setValue(background);
            sp.fill();
            output.addSlice(sp);
        }
        if (!Boolean.parseBoolean(props.getProperty(propLabels[BLOB_DETECT])) && hessianObjects != null) {
            for (int i = 0; i < hessianObjects.getNbObjects(); i++) {
                hessianObjects.getObject(i).draw(output, i + 1);
            }
        } else {
            Object[] stackPix = output.getImageArray();
            for (int i = 0; i < maxima.size(); i++) {
                int[] pix = maxima.get(i);
                ((short[]) stackPix[pix[2]])[pix[0] + pix[1] * width] = (short) (i + 1);
            }
        }
        return new ImagePlus(String.format("%s - Local Maxima", imp.getTitle()), output);
    }

    public void run() {
        maxima = new ArrayList();
        series = Integer.parseInt(props.getProperty(propLabels[SERIES_SELECT]));
        channel = Integer.parseInt(props.getProperty(propLabels[CHANNEL_SELECT]));
        calibration = getCalibration(series);
        radii = getUncalibratedDoubleSigma(series, propLabels[BLOB_SIZE], propLabels[BLOB_SIZE], propLabels[BLOB_SIZE]);
        thresh = Float.parseFloat(props.getProperty(propLabels[BLOB_THRESH]));
        img.loadPixelData(series, channel, channel + 1, null);
        ImagePlus imp = img.getLoadedImage();
        this.stack = imp.getImageStack();
        if (stack == null) {
            return;
        }
        if (!Boolean.parseBoolean(props.getProperty(propLabels[BLOB_DETECT]))) {
            hessianDetection(imp);
        } else {
            IJ.log(String.format("Searching for blobs %.1f pixels in diameter above a threshold of %.0f in \"%s\"...", (2 * radii[0] / calibration[0]), thresh, imp.getTitle()));
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
                s.putFeature(SpotFeatures.CHANNEL, new Double(channel));
                int[] pos = new int[3];
                for (int d = 0; d < 3; d++) {
                    pos[d] = (int) Math.round(s.getFloatPosition(d) / calibration[d]);
                }
                maxima.add(pos);
            }
            this.spotMaxima = maximas;
        }
        output = makeLocalMaximaImage(BACKGROUND, (int) Math.round(radii[0] / calibration[0]));
        IJ.saveAs(output, "TIF", "D:\\debugging\\giani_debug\\watershedOutput.tif");
        labelOutput(imp.getTitle(), "Blobs");
    }

    public void edmDetection(ImagePlus image) {
        ArrayList<int[]> tempMaxima = new ArrayList();
        radii = getUncalibratedDoubleSigma(series, propLabels[HESSIAN_START_SCALE], propLabels[HESSIAN_START_SCALE], propLabels[HESSIAN_START_SCALE]);
        double[] sigma = getCalibratedDoubleSigma(series, propLabels[EDM_FILTER], propLabels[EDM_FILTER], propLabels[EDM_FILTER]);
        GaussianBlur3D.blur(image, sigma[0], sigma[1], sigma[2]);
        int greyThresh = getThreshold(image, AutoThresholder.Method.valueOf(props.getProperty(propLabels[EDM_THRESH])));
        IJ.log(String.format("Searching for objects %.1f pixels in diameter in \"%s\"...", (2 * radii[0] / calibration[0]), image.getTitle()));
        ImagePlus binaryImp = image.duplicate();
        StackThresholder.thresholdStack(binaryImp, greyThresh);
        createThresholdOutline(binaryImp);
//        IJ.saveAs(binaryImp, "TIF", "D:\\debugging\\giani_debug\\binaryImp.tif");
        ImageFloat distanceMap = EDT.run(ImageHandler.wrap(binaryImp), 1, (float) calibration[0], (float) calibration[2], false, -1);
//        IJ.saveAs(distanceMap.getImagePlus(), "TIF", "D:\\debugging\\giani_debug\\distanceMap.tif");
        int nThreads = Runtime.getRuntime().availableProcessors();
        RunnableMaximaFinder[] mf = new RunnableMaximaFinder[nThreads];
        for (int thread = 0; thread < nThreads; thread++) {
            mf[thread] = new RunnableMaximaFinder(distanceMap.getImageStack().getImageArray(), (int) Math.round(radii[0]), tempMaxima, new int[]{image.getWidth(), image.getHeight(), image.getNSlices()}, thread, nThreads, new int[]{2, 2, 2});
            mf[thread].start();
        }
        try {
            for (int thread = 0; thread < nThreads; thread++) {
                mf[thread].join();
            }
        } catch (Exception e) {
            IJ.error("A thread was interrupted in step 3 .");
        }
        ImageStack maxStack = new ImageStack(image.getWidth(), image.getHeight());
        for (int n = 0; n < image.getNSlices(); n++) {
            ByteProcessor bp = new ByteProcessor(image.getWidth(), image.getHeight());
            bp.setValue(0.0);
            bp.fill();
            maxStack.addSlice(bp);
        }
        Object[] maxPix = maxStack.getImageArray();
        for (int[] m : tempMaxima) {
            ((byte[]) maxPix[m[2]])[m[0] + m[1] * image.getWidth()] = 1;
        }
//        IJ.saveAs(new ImagePlus("", maxStack), "TIF", "D:\\debugging\\giani_debug\\maxObjects.tif");
        Objects3DPopulation objects = new Objects3DPopulation(new ImageLabeller().getLabels(ImageHandler.wrap(new ImagePlus("", maxStack))));
        for (int i = 0; i < objects.getNbObjects(); i++) {
            Object3D o = objects.getObject(i);
            double[] centre = o.getCenterAsArray();
            maxima.add(new int[]{(int) Math.round(centre[0]), (int) Math.round(centre[1]), (int) Math.round(centre[2])});
        }
        consolidatePointsOnDistance(radii[0], calibration);
        consolidatePointsOnIntensity(0.8, Double.parseDouble(props.getProperty(propLabels[HESSIAN_STOP_SCALE])), calibration, ImageHandler.wrap(image));
    }

    public void hessianDetection(ImagePlus image) {
//        radii = getUncalibratedDoubleSigma(series, propLabels[EDM_MIN_SIZE], propLabels[EDM_MIN_SIZE], propLabels[EDM_MIN_SIZE]);
//        double[] sigma = getCalibratedDoubleSigma(series, propLabels[EDM_FILTER], propLabels[EDM_FILTER], propLabels[EDM_FILTER]);
        IJ.log(String.format("Searching for objects %.1f pixels in diameter in \"%s\"...", (2 * radii[0] / calibration[0]), image.getTitle()));
//        IJ.saveAs(binaryImp, "TIF", "D:\\debugging\\giani_debug\\binaryImp.tif");

        (new StackConverter(image)).convertToGray32();
        double[] cal = getCalibration(series);
        FloatImage convertedImage = (FloatImage) FloatImage.wrap(image);
        Aspects a = new Aspects(cal[0], cal[1], cal[2]);
        convertedImage.aspects(a);

        MultiThreadedHessian hessian = new MultiThreadedHessian(inputs, convertedImage);
        hessian.setup(img, props, propLabels);
        hessian.start();
        try {
            hessian.join();
        } catch (InterruptedException e) {
            IJ.log("Could not generate hessian images.");
            return;
        }
        ImagePlus hessianOutputs = hessian.getOutput();
//        IJ.saveAs(hessianOutputs, "TIF", "D:\\debugging\\giani_debug\\hessian_outputs.tif");
        SubstackMaker ssm = new SubstackMaker();
        int inputStackSize = image.getImageStackSize();
        int nScales = hessianOutputs.getImageStackSize() / (3 * inputStackSize);
        ImagePlus[] blobImps = new ImagePlus[nScales];
        for (int s = 0; s < nScales; s++) {
            int index = s * 3 * inputStackSize + 1;
            blobImps[s] = ssm.makeSubstack(hessianOutputs, String.format("%d-%d", index, index + inputStackSize - 1));
            StackMath.mutiply(blobImps[s], -1.0);
//            IJ.saveAs(blobImps[0], "TIF", "D:\\debugging\\giani_debug\\blob_outputs_pre_threshold.tif");
            StackThresholder.thresholdStack(blobImps[s], 0.001);
            (new StackProcessor(blobImps[s].getImageStack())).invert();
        }
        ImageCalculator ic = new ImageCalculator();
        for (int s = 0; s < nScales - 1; s++) {
            blobImps[0] = ic.run("AND create stack", blobImps[s], blobImps[s + 1]);
        }
//        IJ.saveAs(blobImps[0], "TIF", "D:\\debugging\\giani_debug\\blob_outputs_post_threshold.tif");
        createThresholdOutline(blobImps[0]);
        hessianObjects = new Objects3DPopulation(new ImageLabeller().getLabels(ImageHandler.wrap(blobImps[0])));
        for (int i = 0; i < hessianObjects.getNbObjects(); i++) {
            Object3D o = hessianObjects.getObject(i);
            double[] centre = o.getCenterAsArray();
            maxima.add(new int[]{(int) Math.round(centre[0]), (int) Math.round(centre[1]), (int) Math.round(centre[2])});
        }
    }

    void consolidatePointsOnDistance(double thresh, double[] calibration) {
        boolean change = true;
        while (change) {
            change = false;
            for (int i = 0; i < maxima.size(); i++) {
                int[] m1 = maxima.get(i);
                for (int j = 0; j < maxima.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    int[] m2 = maxima.get(j);
                    double dist = Utils.calcEuclidDist(new double[]{m1[0] * calibration[0], m1[1] * calibration[1], m1[2] * calibration[2]},
                            new double[]{m2[0] * calibration[0], m2[1] * calibration[1], m2[2] * calibration[2]});
                    if (dist < thresh) {
                        int x = (int) Math.round((m1[0] + m2[0]) / 2.0);
                        int y = (int) Math.round((m1[1] + m2[1]) / 2.0);
                        int z = (int) Math.round((m1[2] + m2[2]) / 2.0);
                        maxima.remove(m1);
                        maxima.remove(m2);
                        maxima.add(new int[]{x, y, z});
                        i = -1;
                        j = maxima.size();
                    }
                }
            }
        }
    }

    void consolidatePointsOnIntensity(double intensThresh, double distThresh, double[] calibration, ImageHandler stack) {
        boolean change = true;
        while (change) {
            change = false;
            for (int i = 0; i < maxima.size(); i++) {
                int[] m1 = maxima.get(i);
                for (int j = 0; j < maxima.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    int[] m2 = maxima.get(j);
                    double dist = Utils.calcEuclidDist(new double[]{m1[0] * calibration[0], m1[1] * calibration[1], m1[2] * calibration[2]},
                            new double[]{m2[0] * calibration[0], m2[1] * calibration[1], m2[2] * calibration[2]});
                    if (dist > distThresh) {
                        continue;
                    }
                    double[] profile = stack.extractLine(m1[0], m1[1], m1[2], m2[0], m2[1], m2[2], true);
                    boolean remove = true;
                    DescriptiveStatistics ds = new DescriptiveStatistics(profile);
                    for (double p : profile) {
                        if (p / ds.getMean() < intensThresh) {
                            remove = false;
                            break;
                        }
                    }
                    if (remove) {
                        int x = (int) Math.round((m1[0] + m2[0]) / 2.0);
                        int y = (int) Math.round((m1[1] + m2[1]) / 2.0);
                        int z = (int) Math.round((m1[2] + m2[2]) / 2.0);
                        maxima.remove(m1);
                        maxima.remove(m2);
                        maxima.add(new int[]{x, y, z});
                        i = -1;
                        j = maxima.size();
                    }
                }
            }
        }
    }

    private void createThresholdOutline(ImagePlus imp) {
        ImageStack stack = imp.getImageStack();
        edmThresholdOutline = new Roi[stack.size()];
        ThresholdToSelection tts = new ThresholdToSelection();
        for (int i = 1; i <= stack.size(); i++) {
            ImageProcessor ip = stack.getProcessor(i);
            ip.setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE);
            tts.setup(null, imp);
            tts.run(ip);
            edmThresholdOutline[i - 1] = imp.getRoi();
        }
    }

    public ArrayList<int[]> getMaxima() {
        return maxima;
    }

    public List<Spot> getSpotMaxima() {
        return spotMaxima;
    }

    public Roi[] getEdmThresholdOutline() {
        return edmThresholdOutline;
    }

    private int getThreshold(ImagePlus image, AutoThresholder.Method method) {
        StackStatistics stats = new StackStatistics(image);
        int tIndex = (new AutoThresholder()).getThreshold(method, stats.histogram);
        return (int) Math.round(stats.histMin + stats.binSize * tIndex);
    }

    public MultiThreadedMaximaFinder duplicate() {
        MultiThreadedMaximaFinder newProcess = new MultiThreadedMaximaFinder(inputs);
        this.updateOutputDests(newProcess);
        return newProcess;
    }

}
