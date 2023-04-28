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
package net.calm.iaclasslibrary.Extrema;

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
import ij.process.*;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageLabeller;
import net.calm.iaclasslibrary.Binary.BinaryMaker;
import net.calm.iaclasslibrary.Cell3D.Spot3D;
import net.calm.iaclasslibrary.Cell3D.SpotFeatures;
import net.calm.iaclasslibrary.IAClasses.Utils;
import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import net.calm.iaclasslibrary.Process.Filtering.MultiThreadedHessian;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import net.calm.iaclasslibrary.Stacks.StackMath;
import net.calm.iaclasslibrary.Stacks.StackThresholder;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class MultiThreadedMaximaFinder extends MultiThreadedProcess {

    public static int BLOB_DETECT = 6;
    public static int HESSIAN_START_SCALE = 4;
    public static int BLOB_SIZE = 1;
    public static int CHANNEL_SELECT = 0;
    //public static int HESSIAN_STOP_SCALE = 5;
    public static int BLOB_THRESH = 2;
    //public static int EDM_THRESH = 3;
    public static int SERIES_SELECT = 7;
    public static int HESSIAN_DETECT = 8;
    //public static int EDM_FILTER = 9;
    public static int METHOD = 9;
    //public static int HESSIAN_SCALE_STEP = 10;
    public static int HESSIAN_ABS = 11;
    public static int HESSIAN_THRESH = 12;
    public static int STARDIST_DETECT = 13;
    public static int STARDIST_PROB = 14;
    public static int STARDIST_OVERLAP = 15;
    public static int STARDIST_DIR = 16;
    public static int STARDIST_MODEL = 17;
    public static int STARDIST_TILE_XY = 18;
    public static int STARDIST_TILE_Z = 19;
    public static int ILASTIK_DETECT = 20;
    public static int ILASTIK_FILE = 21;
    public static int ILASTIK_CHANNEL = 22;
    public static int ILASTIK_DIR = 23;
    public static int ILASTIK_THRESH = 24;
    public static int ILASTIK_SMOOTHING = 25;
    public static int THRESH_METHOD = 26;
    public static int THRESH_DETECT = 27;
    public static int N_PROP_LABELS = 28;

    private ArrayList<int[]> maxima;
    private ArrayList<Spot3D> spotMaxima;
    private double[] radii;
    private double[] calibration;
    private ImageStack stack;
    private float thresh;
    private int series;
    private int channel;
    public static short BACKGROUND = 0;
    private Roi[] detectedObjectsOutline;
    private Objects3DPopulation detectedObjects;

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
        if (propLabels[STARDIST_DETECT] != null && Boolean.parseBoolean(props.getProperty(propLabels[STARDIST_DETECT]))) {
            return output;
        }
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
        if (!Boolean.parseBoolean(props.getProperty(propLabels[BLOB_DETECT])) && detectedObjects != null) {
            for (int i = 0; i < detectedObjects.getNbObjects(); i++) {
                detectedObjects.getObject(i).draw(output, i + 1);
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
        maxima = new ArrayList<>();
        spotMaxima = new ArrayList<>();
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
        if (Boolean.parseBoolean(props.getProperty(propLabels[THRESH_DETECT]))) {
            thresholdDetection(imp);
        } else if (Boolean.parseBoolean(props.getProperty(propLabels[HESSIAN_DETECT]))) {
            hessianDetection(imp);
        } else if (propLabels[STARDIST_DETECT] != null && Boolean.parseBoolean(props.getProperty(propLabels[STARDIST_DETECT], "false"))) {
            runStarDist(imp);
        } else if (propLabels[ILASTIK_DETECT] != null && Boolean.parseBoolean(props.getProperty(propLabels[ILASTIK_DETECT], "false"))) {
            runIlastik();
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
                LinkedList<Voxel3D> voxels = new LinkedList<>();
                voxels.add(new Voxel3D(pos[0], pos[1], pos[2], 0.0));
                spotMaxima.add(new Spot3D(s, voxels));
            }
        }
        output = makeLocalMaximaImage(BACKGROUND, (int) Math.round(radii[0] / calibration[0]));
//        IJ.saveAs(output, "TIF", "D:\\debugging\\giani_debug\\watershedOutput.tif");
        labelOutput(imp.getTitle(), "Blobs");
    }

    public void hessianDetection(ImagePlus image) {
        int nEigenValues;
        Aspects a = new Aspects();
        if (calibration == null) {
            if (img == null) {
                calibration = new double[3];
                calibration[0] = image.getCalibration().pixelWidth;
                calibration[1] = image.getCalibration().pixelHeight;
                calibration[2] = image.getCalibration().pixelDepth;
            } else {
                calibration = getCalibration(series);
            }
        }
        if (maxima == null) {
            maxima = new ArrayList();
        }
//        double[] minRadii = getUncalibratedDoubleSigma(series, propLabels[HESSIAN_START_SCALE], propLabels[HESSIAN_START_SCALE], propLabels[HESSIAN_START_SCALE]);
//        double[] maxRadii = getUncalibratedDoubleSigma(series, propLabels[HESSIAN_STOP_SCALE], propLabels[HESSIAN_STOP_SCALE], propLabels[HESSIAN_STOP_SCALE]);
//        double[] sigma = getCalibratedDoubleSigma(series, propLabels[EDM_FILTER], propLabels[EDM_FILTER], propLabels[EDM_FILTER]);
        IJ.log(String.format("Searching for objects %.1f pixels in diameter in \"%s\"...", (
                Double.parseDouble(props.getProperty(propLabels[HESSIAN_START_SCALE])) / calibration[0]), image.getTitle()));
//        IJ.saveAs(binaryImp, "TIF", "D:\\debugging\\giani_debug\\binaryImp.tif");

        if (image.getStackSize() > 1) {
            nEigenValues = 3;
            a = new Aspects(calibration[0], calibration[1], calibration[2]);
            (new StackConverter(image)).convertToGray32();
        } else {
            nEigenValues = 2;
            image.setProcessor(image.getProcessor().convertToFloatProcessor());
        }
        FloatImage convertedImage = (FloatImage) FloatImage.wrap(image);
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
//        IJ.saveAs(hessianOutputs, "TIF", FileName.uniqueFileName("E:/Dropbox (The Francis Crick)/Debugging/Giani/images/outputs", "hessian_outputs", "tif"));
        SubstackMaker ssm = new SubstackMaker();
        int inputStackSize = image.getImageStackSize();
        int nScales = hessianOutputs.getImageStackSize() / (nEigenValues * inputStackSize);
        ImagePlus[] blobImps = new ImagePlus[nScales];
        double hessianThresh = Double.parseDouble(props.getProperty(propLabels[HESSIAN_THRESH]));
        for (int s = 0; s < nScales; s++) {
            int index = s * nEigenValues * inputStackSize + 1;
            blobImps[s] = ssm.makeSubstack(hessianOutputs, String.format("%d-%d", index, index + inputStackSize - 1));
            StackMath.mutiply(blobImps[s], -1.0);
//            IJ.saveAs(blobImps[0], "TIF", "D:\\debugging\\giani_debug\\blob_outputs_pre_threshold.tif");
            StackThresholder.thresholdStack(blobImps[s], s % nEigenValues == 0 ? hessianThresh : Double.MIN_VALUE);
            (new StackProcessor(blobImps[s].getImageStack())).invert();
        }
        ImageCalculator ic = new ImageCalculator();
        for (int s = 1; s < nScales; s++) {
            blobImps[0] = ic.run("AND create stack", blobImps[0], blobImps[s]);
        }
//        IJ.saveAs(blobImps[0], "TIF", "D:\\debugging\\giani_debug\\blob_outputs_post_threshold.tif");
//        IJ.saveAs(blobImps[0],"TIFF", "E:\\Dropbox (The Francis Crick)\\Debugging\\Giani//hessian_output.tiff");
        processThresholdedObjects(blobImps[0]);
        output = blobImps[0];
    }

    public void thresholdDetection(ImagePlus image) {
        int thresh = BinaryMaker.getThreshold(image,
                AutoThresholder.Method.valueOf(props.getProperty(propLabels[MultiThreadedMaximaFinder.THRESH_METHOD])));
        IJ.log(String.format("Segmenting \"%s\" with a threshold of %d...", image.getTitle(), thresh));
        ImagePlus mask = image.duplicate();
        StackThresholder.thresholdStack(mask, thresh);
        (new StackProcessor(mask.getImageStack())).invert();
        processThresholdedObjects(mask);
        output = mask;
    }

    private void processThresholdedObjects(ImagePlus imp) {
//        IJ.saveAs(imp, "TIF", "D:\\Dropbox (The Francis Crick)\\Debugging\\Giani\\mask.tif");
        createThresholdOutline(imp);
        detectedObjects = new Objects3DPopulation(new ImageLabeller().getLabels(ImageHandler.wrap(imp)));
        for (int i = 0; i < detectedObjects.getNbObjects(); i++) {
            Object3D o = detectedObjects.getObject(i);
            double[] centre = o.getCenterAsArray();
            maxima.add(new int[]{(int) Math.round(centre[0]), (int) Math.round(centre[1]), (int) Math.round(centre[2])});
            Spot s = new Spot(centre[0] * calibration[0], centre[1] * calibration[1], centre[2] * calibration[2], 1.0, 1.0);
            s.putFeature(SpotFeatures.CHANNEL, (double) channel);
            spotMaxima.add(new Spot3D(o, s));
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
        detectedObjectsOutline = new Roi[stack.size()];
        ThresholdToSelection tts = new ThresholdToSelection();
        for (int i = 1; i <= stack.size(); i++) {
            ImageProcessor ip = stack.getProcessor(i);
            ip.setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE);
            tts.setup(null, imp);
            tts.run(ip);
            detectedObjectsOutline[i - 1] = imp.getRoi();
        }
    }

    public ArrayList<int[]> getMaxima() {
        return maxima;
    }

    public ArrayList<Spot3D> getSpotMaxima() {
        return spotMaxima;
    }

    public Roi[] getDetectedObjectsOutline() {
        return detectedObjectsOutline;
    }

    public void runStarDist(ImagePlus imp) {
        File stardistTempDir = new File(IJ.getDirectory("home"), "StarDistTemp");
        stardistTempDir.mkdir();
        String tempImage = "stardist_temp.tif";
        String starDistOutput = FilenameUtils.getBaseName(tempImage) + ".stardist." + FilenameUtils.getExtension(tempImage);
        imp = img.getLoadedImage();
        IJ.saveAs(imp, "TIF", (new File(stardistTempDir, tempImage).getAbsolutePath()));

        List<String> cmd = new ArrayList<>();
        if (IJ.isWindows()) {
            cmd.add("cmd.exe");
            cmd.add("/C");
            cmd.add("cd");
            cmd.add(String.format("%s%svenv%sScripts", props.getProperty(propLabels[STARDIST_DIR]), File.separator, File.separator));
            cmd.add("&");
            cmd.add("activate.bat");
            cmd.add("&");
            cmd.add("cd");
            cmd.add("../..");
            cmd.add("&");
            cmd.add("python");
            cmd.add("stardist/scripts/predict3d.py");
        } else if (IJ.isLinux() || IJ.isMacOSX()) {
            cmd.add(String.format("%s/bin/python", props.getProperty(propLabels[STARDIST_DIR])));
            cmd.add(String.format("%s/stardist/scripts/predict3d.py", props.getProperty(propLabels[STARDIST_DIR])));
        }
        cmd.add("-i");
        cmd.add((new File(stardistTempDir, tempImage).getAbsolutePath()));
        cmd.add("-m");
        cmd.add(props.getProperty(propLabels[STARDIST_MODEL]));
        cmd.add("-o");
        cmd.add(stardistTempDir.getAbsolutePath());
        cmd.add("--prob_thresh");
        cmd.add(props.getProperty(propLabels[STARDIST_PROB]));
        cmd.add("--nms_thresh");
        cmd.add(props.getProperty(propLabels[STARDIST_OVERLAP]));
        cmd.add("--n_tiles");
        cmd.add(props.getProperty(propLabels[STARDIST_TILE_XY]));
        cmd.add(props.getProperty(propLabels[STARDIST_TILE_XY]));
        cmd.add(props.getProperty(propLabels[STARDIST_TILE_Z]));

        System.out.println(cmd.toString().replace(",", ""));

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);

            Process p = pb.start();

            Thread t = new Thread(Thread.currentThread().getName() + "-" + p.hashCode()) {
                @Override
                public void run() {
                    BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    try {
                        for (String line = stdIn.readLine(); line != null; ) {
                            System.out.println(line);
                            line = stdIn.readLine();// you don't want to remove or comment that line! no you don't :P
                        }
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            };
            t.setDaemon(true);
            t.start();

            p.waitFor();

            int exitValue = p.exitValue();

            if (exitValue != 0) {
                System.out.println("StarDist exited with value " + exitValue + ". Please check output above for indications of the problem.");
            } else {
                System.out.println("StarDist finished");
            }
            output = IJ.openImage((new File(stardistTempDir, starDistOutput).getAbsolutePath()));
            FileUtils.forceDelete(stardistTempDir);
        } catch (InterruptedException | IOException e) {

        }
        //stardistTempDir.delete();
    }

    public void runIlastik() {
        ImagePlus ilastikProbMap = null;
        File ilastikTempDir = new File(IJ.getDirectory("home"), "ilastikTemp");
        ilastikTempDir.mkdir();
        String tempImage = "ilastik_temp.tiff";
        String ilastikOutput = FilenameUtils.getBaseName(tempImage) + ".ilastik." + FilenameUtils.getExtension(tempImage);
        ImagePlus imp = img.getLoadedImage();
        IJ.saveAs(imp, "TIF", (new File(ilastikTempDir, tempImage).getAbsolutePath()));
        File copyOfProjectFile = new File(ilastikTempDir, FilenameUtils.getName(props.getProperty(propLabels[ILASTIK_FILE])));
        Process p = null;
        try {
            FileUtils.copyFile(new File(props.getProperty(propLabels[ILASTIK_FILE])), copyOfProjectFile);

            List<String> cmd = new ArrayList<>();
            if (IJ.isWindows()) {
                cmd.add("cmd.exe");
                cmd.add("/C");
                cmd.add("cd");
                cmd.add(props.getProperty(propLabels[ILASTIK_DIR]));
                cmd.add("&");
                cmd.add("ilastik.exe");
            } else if (IJ.isLinux() || IJ.isMacOSX()) {
                cmd.add(String.format("%s/run_ilastik.sh", props.getProperty(propLabels[ILASTIK_DIR])));
            }
            cmd.add("--headless");
            cmd.add(String.format("--project=\"%s\"", copyOfProjectFile));
            cmd.add("--export_source=\"Probabilities\"");
            cmd.add("--export_dtype=\"uint16\"");
            cmd.add("--pipeline_result_drange=\"(0.0,1.0)\"");
            cmd.add("--export_drange=\"(0,65535)\"");
            cmd.add("--output_format");
            cmd.add("multipage tiff");
            cmd.add(String.format("--output_filename_format=\"%s\"", new File(ilastikTempDir, ilastikOutput).getAbsolutePath()));
            cmd.add(String.format("\"%s\"", new File(ilastikTempDir, tempImage).getAbsolutePath()));

            System.out.println(cmd.toString());

            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);

            p = pb.start();
            final InputStream is = p.getInputStream();
            Thread t = new Thread(Thread.currentThread().getName() + "-" + p.hashCode()) {
                @Override
                public void run() {
                    BufferedReader stdIn = new BufferedReader(new InputStreamReader(is));
                    try {
                        for (String line = stdIn.readLine(); line != null; ) {
                            System.out.println(line);
                            line = stdIn.readLine();// you don't want to remove or comment that line! no you don't :P
                        }
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            };
            t.setDaemon(true);
            t.start();

            p.waitFor();

            int exitValue = p.exitValue();

            if (exitValue != 0) {
                System.out.println("ilastik exited with value " + exitValue + ". Please check output above for indications of the problem.");
            } else {
                System.out.println("ilastik finished");
            }
            ImporterOptions io = new ImporterOptions();
            io.setCBegin(0, Integer.parseInt(props.getProperty(propLabels[ILASTIK_CHANNEL])));
            io.setCEnd(0, Integer.parseInt(props.getProperty(propLabels[ILASTIK_CHANNEL])));
            io.setCStep(0, 1);
            io.setSpecifyRanges(true);
            io.setId(new File(ilastikTempDir, ilastikOutput).getAbsolutePath());
            ilastikProbMap = BF.openImagePlus(io)[0];
            FileUtils.forceDelete(ilastikTempDir);
        } catch (InterruptedException | IOException | FormatException e) {
            if (p != null) p.destroyForcibly();
            IJ.log("Error: ilastik detection failed.");
        }
//        IJ.saveAs(ilastikProbMap, "TIFF", "D:/Dropbox (The Francis Crick)/Debugging/Giani/ilastik_output.tiff");
        if (Double.parseDouble(props.getProperty(propLabels[ILASTIK_SMOOTHING])) > 0.0) {
            double[] sigma = getCalibratedDoubleSigma(series, propLabels[ILASTIK_SMOOTHING], propLabels[ILASTIK_SMOOTHING], propLabels[ILASTIK_SMOOTHING]);
            GaussianBlur3D.blur(ilastikProbMap, sigma[0], sigma[1], sigma[2]);
        }
//        IJ.saveAs(ilastikProbMap, "TIFF", "D:/Dropbox (The Francis Crick)/Debugging/Giani/ilastik_output_smoothed.tiff");
        StackThresholder.thresholdStack(ilastikProbMap, 65535 * Double.parseDouble(props.getProperty(propLabels[ILASTIK_THRESH])));
        (new StackProcessor(ilastikProbMap.getImageStack())).invert();
//        IJ.saveAs(ilastikProbMap, "TIFF", "D:/Dropbox (The Francis Crick)/Debugging/Giani/ilastik_output_thresholded.tiff");
        processThresholdedObjects(ilastikProbMap);
        output = ilastikProbMap;
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
        }
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
