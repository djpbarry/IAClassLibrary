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
package net.calm.iaclasslibrary.Trajectory;

import net.calm.iaclasslibrary.DataProcessing.Interpolator;
import net.calm.iaclasslibrary.DataProcessing.Smoother;
import net.calm.iaclasslibrary.IAClasses.Utils;
import net.calm.iaclasslibrary.IO.DataReader;
import net.calm.iaclasslibrary.IO.DataWriter;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import net.calm.iaclasslibrary.UtilClasses.GenVariables;
import net.calm.iaclasslibrary.UtilClasses.Utilities;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class TrajectoryAnalysis implements PlugIn {

    private static File inputFile;
    private static double minVel = 0.01;
    private static double minDist = 0.0;
    private static double framesPerSec = 3.0;
    private static int minPointsForMSD = 10;
    private static int smoothingWindow = 1;
    private static boolean smooth = false, interpolate = false;
    private static int INPUT_X_INDEX = 2, INPUT_Y_INDEX = 3, INPUT_ID_INDEX = 10, INPUT_FRAME_INDEX = 0;
    private final int _X_ = 0, _Y_ = 1, _T_ = 2, _ID_ = 3;
    private final int V_Fr = 0, V_X = 1, V_Y = 2, V_M = 3, V_Th = 4, V_F = 5, V_ID = 6, V_D = 7, V_T = 8;
    private final String TITLE = "net.calm.iaclasslibrary.net.calm.trackerlibrary.Trajectory Analysis";
    public static final String MIC = String.format("%cm", IJ.micronSymbol);
    private final String MIC_PER_SEC = String.format("%s/s", MIC);
    private LinkedHashMap<Integer, Integer> idIndexMap;
    private final boolean batch;
    public static final String MSD = "Mean_Square_Displacements.csv";
    private static boolean labelledData = false;
    private boolean openResultsDirectory = true;

    public TrajectoryAnalysis(double minVel, double minDist, double framesPerSec, int smoothingWindow, boolean smooth, boolean interpolate, boolean labelledData, boolean batch, boolean openResultsDirectory, int[] cols) {
        this(batch);
        TrajectoryAnalysis.minVel = minVel;
        TrajectoryAnalysis.minDist = minDist;
        TrajectoryAnalysis.framesPerSec = framesPerSec;
        TrajectoryAnalysis.smoothingWindow = smoothingWindow;
        TrajectoryAnalysis.smooth = smooth;
        TrajectoryAnalysis.interpolate = interpolate;
        TrajectoryAnalysis.labelledData = labelledData;
        TrajectoryAnalysis.INPUT_X_INDEX = cols[0];
        TrajectoryAnalysis.INPUT_Y_INDEX = cols[1];
        TrajectoryAnalysis.INPUT_FRAME_INDEX = cols[2];
        TrajectoryAnalysis.INPUT_ID_INDEX = cols[3];
        this.openResultsDirectory = openResultsDirectory;
    }

    public TrajectoryAnalysis(boolean batch) {
        this.batch = batch;
    }

    public TrajectoryAnalysis() {
        this(false);
    }
//D:\OneDrive - The Francis Crick Institute\Working Data\Ultanir\TrkB\Manual tracking

    public void run(String inputFileName) {
        IJ.log(String.format("Running %s\n", TITLE));
        double[][] inputData;
        try {
            if (!batch) {
                inputFile = Utilities.getFile(inputFile, "Select input file", true);
            } else {
                inputFile = new File(inputFileName);
            }
        } catch (Exception e) {
            GenUtils.error("Cannot read input file.");
            return;
        }
        File parentOutputDirectory = openResultsDirectory
                ? new File(GenUtils.openResultsDirectory(String.format("%s%s%s_%s", inputFile.getParent(), File.separator, TITLE, inputFile.getName())))
                : new File(inputFile.getParent());
        String[] headingsArray = getFileHeadings(inputFile, false);
        if (!batch && !showDialog(headingsArray)) {
            return;
        }
        headingsArray = getFileHeadings(inputFile, labelledData);
        if (labelledData) {
            TrajectoryAnalysis.INPUT_X_INDEX--;
            TrajectoryAnalysis.INPUT_Y_INDEX--;
            TrajectoryAnalysis.INPUT_FRAME_INDEX--;
            TrajectoryAnalysis.INPUT_ID_INDEX--;
        }
        ArrayList<String> labels = labelledData ? new ArrayList() : null;
        try {
            IJ.log(String.format("Reading %s...", inputFile.getAbsolutePath()));
            inputData = DataReader.readCSVFile(inputFile, CSVFormat.DEFAULT, new ArrayList(), labels);
            IJ.log("Parsing data...");
        } catch (Exception e) {
            GenUtils.error("Cannot read input file.");
            return;
        }
        try {
            double[][][] processedInputData = processData(inputData);
            double[][][] interData = processedInputData;
            if (interpolate) {
                interData = Interpolator.interpolateLinearly(processedInputData, _T_, new boolean[]{true, true, true, false});
                saveData(interData, "Interpolated_Coordinates.csv",
                        new String[]{headingsArray[INPUT_X_INDEX], headingsArray[INPUT_Y_INDEX],
                            headingsArray[INPUT_FRAME_INDEX], headingsArray[INPUT_ID_INDEX]}, parentOutputDirectory);
            }
            double[][][] smoothedData = interData;
            if (smooth) {
                smoothedData = Smoother.smoothData(interData, smoothingWindow, new boolean[]{true, true, false, false});
                saveData(smoothedData, "Smoothed_Coordinates.csv",
                        new String[]{headingsArray[INPUT_X_INDEX], headingsArray[INPUT_Y_INDEX],
                            headingsArray[INPUT_FRAME_INDEX], headingsArray[INPUT_ID_INDEX]}, parentOutputDirectory);
            }
            IJ.log("Calculating instantaneous velocities...");
            double[][][] vels = calcInstVels(smoothedData, _X_, _Y_, _T_);
            IJ.log("Calculating mean velocities...");
            double[][] meanVels = calcMeanVels(smoothedData, vels, minVel);
            IJ.log("Analysing runs...");
            double[][][] runLengths = calcRunLengths(vels, minVel);
            IJ.log("Analysing mean square displacements...");
            double[][] msds = calcMSDs(smoothedData, minPointsForMSD, framesPerSec);
            IJ.log("Generating spider plot data...");
            double[][] spiderPlotData = calcSpiderPlotData(smoothedData);
            IJ.log("Saving outputs...");
            saveData(vels, "Instantaneous_Velocities.csv",
                    new String[]{"Frame No.", String.format("X Vel (%s)", MIC_PER_SEC),
                        String.format("Y Vel (%s)", MIC_PER_SEC), String.format("Mag (%s)", MIC_PER_SEC),
                        String.format("Theta (%c)", IJ.degreeSymbol),
                        "Time (frames)", "Track ID", "Distance", "Time (s)"}, parentOutputDirectory);
            saveMSDs(msds, parentOutputDirectory);
            saveMeanVels(meanVels, parentOutputDirectory);
            saveRunLengths(runLengths, parentOutputDirectory);
            saveSpiderPlotData(spiderPlotData, parentOutputDirectory);
        } catch (IOException e) {
            GenUtils.logError(e, null);
        }
        IJ.log("Done.");
    }

    double[][][] processData(double[][] inputData) {
        ArrayList<Integer> lengths = new ArrayList();
        idIndexMap = new LinkedHashMap();
        LinkedHashMap<Double, Integer> localMap = new LinkedHashMap();
        for (int i = 0; i < inputData.length; i++) {
            double thisID = inputData[i][INPUT_ID_INDEX];
            if (Double.isNaN(thisID)) {
                continue;
            }
            if (localMap.get(thisID) == null) {
                localMap.put(thisID, localMap.size());
                lengths.add(1);
            } else {
                int currentLength = lengths.get(localMap.get(thisID));
                currentLength++;
                lengths.set(localMap.get(thisID), currentLength);
            }
        }
        double[][][] output = new double[localMap.size()][][];
        int[] outputRowNumbers = new int[output.length];
        for (int inputIndex = 0; inputIndex < inputData.length; inputIndex++) {
            double thisID = inputData[inputIndex][INPUT_ID_INDEX];
            if (Double.isNaN(thisID)) {
                continue;
            }
            int outputIndex = localMap.get(thisID);
            if (output[outputIndex] == null) {
                outputRowNumbers[outputIndex] = 0;
                output[outputIndex] = new double[lengths.get(outputIndex)][4];
                idIndexMap.put(outputIndex, (int) Math.round(thisID));
            }
            output[outputIndex][outputRowNumbers[outputIndex]][_X_] = inputData[inputIndex][INPUT_X_INDEX];
            output[outputIndex][outputRowNumbers[outputIndex]][_Y_] = inputData[inputIndex][INPUT_Y_INDEX];
            output[outputIndex][outputRowNumbers[outputIndex]][_T_] = inputData[inputIndex][INPUT_FRAME_INDEX];
            output[outputIndex][outputRowNumbers[outputIndex]][_ID_] = inputData[inputIndex][INPUT_ID_INDEX];
            outputRowNumbers[outputIndex]++;
        }
        return output;
    }

    double[][][] calcInstVels(double[][][] data, int X_INDEX, int Y_INDEX, int FRAME_INDEX) {
        int a = data.length;
        double[][][] vels = new double[a][][];
        for (int i = 0; i < a; i++) {
            int b = data[i].length;
            vels[i] = new double[b][9];
            Arrays.fill(vels[i][0], 0.0);
            vels[i][0][V_ID] = idIndexMap.get(i);
            for (int j = 1; j < b; j++) {
                double x2 = data[i][j][X_INDEX];
                double x1 = data[i][j - 1][X_INDEX];
                double y2 = data[i][j][Y_INDEX];
                double y1 = data[i][j - 1][Y_INDEX];
                double dt = data[i][j][FRAME_INDEX] - data[i][j - 1][FRAME_INDEX];
                vels[i][j][V_Fr] = data[i][j][FRAME_INDEX];
                vels[i][j][V_X] = framesPerSec * (x2 - x1) / dt;
                vels[i][j][V_Y] = framesPerSec * (y2 - y1) / dt;
                vels[i][j][V_D] = Utils.calcDistance(x1, y1, x2, y2);
                vels[i][j][V_M] = framesPerSec * vels[i][j][V_D] / dt;
                vels[i][j][V_Th] = Utils.arcTan(x2 - x1, y2 - y1);
                vels[i][j][V_F] = dt;
                vels[i][j][V_ID] = idIndexMap.get(i);
                vels[i][j][V_T] = dt / framesPerSec;
            }
        }
        return vels;
    }

    double[][] calcMeanVels(double[][][] data, double[][][] vels, double minVel) {
        int a = vels.length;
        double[][] meanVels = new double[a][3];
        for (int i = 0; i < a; i++) {
            int b = vels[i].length;
            DescriptiveStatistics xVel = new DescriptiveStatistics();
            DescriptiveStatistics yVel = new DescriptiveStatistics();
            DescriptiveStatistics mVel = new DescriptiveStatistics();
            for (int j = 0; j < b; j++) {
                if (vels[i][j][V_M] > minVel) {
                    xVel.addValue(vels[i][j][V_X]);
                    yVel.addValue(vels[i][j][V_Y]);
                    mVel.addValue(vels[i][j][V_M]);
                }
            }
            double theta = Utils.arcTan(xVel.getMean(), yVel.getMean());
            if (theta > 180.0) {
                theta -= 360.0;
            }
            meanVels[i][0] = mVel.getMean();
            meanVels[i][1] = theta;
        }
        calcDirectionalities(data, vels, meanVels);
        return meanVels;
    }

    void calcDirectionalities(double[][][] data, double[][][] instVels, double[][] meanVels) {
        int a = data.length;
        for (int i = 0; i < a; i++) {
            int b = data[i].length;
            double totalDisplacement = 0.0;
            for (int j = 0; j < b; j++) {
                if (instVels[i][j][V_M] > minVel) {
                    totalDisplacement += instVels[i][j][V_D];
                }
            }
            meanVels[i][2] = Utils.calcDistance(data[i][0][_X_], data[i][0][_Y_], data[i][b - 1][_X_], data[i][b - 1][_Y_]) / totalDisplacement;
        }
    }

    double[][][] calcRunLengths(double[][][] vels, double minVel) {
        int a = vels.length;
        ArrayList<ArrayList<double[]>> runs = new ArrayList();
        for (int i = 0; i < a; i++) {
            int id = idIndexMap.get(i);
            ArrayList<double[]> current = new ArrayList();
            int b = vels[i].length;
            if (b < 1) {
                continue;
            }
            DescriptiveStatistics mVel = null;
            DescriptiveStatistics xVel = null;
            DescriptiveStatistics yVel = null;
            double time = 0.0, cumDist = 0.0, netDistX = 0.0, netDistY = 0.0, sumX = 0.0, sumY = 0.0;
            boolean dir = vels[i][0][V_Th] > 90 && vels[i][0][V_Th] < 270;
            boolean lastDir = dir;
            for (int j = 0; j < b; j++) {
                dir = vels[i][j][V_Th] > 90 && vels[i][j][V_Th] < 270;
                if (vels[i][j][V_M] >= minVel && dir == lastDir) {
                    if (mVel == null) {
                        mVel = new DescriptiveStatistics();
                        xVel = new DescriptiveStatistics();
                        yVel = new DescriptiveStatistics();
                        time = 0.0;
                    }
                    time += vels[i][j][V_F];
                    for (int t = 0; t < vels[i][j][V_F]; t++) {
                        mVel.addValue(Math.sqrt(vels[i][j][V_X] * vels[i][j][V_X] + vels[i][j][V_Y] * vels[i][j][V_Y]));
                        xVel.addValue(vels[i][j][V_X]);
                        yVel.addValue(vels[i][j][V_Y]);
                    }
                    cumDist += vels[i][j][V_D];
                    netDistX += vels[i][j][V_X] * vels[i][j][V_T];
                    netDistY += vels[i][j][V_Y] * vels[i][j][V_T];
                    lastDir = dir;
                } else if (mVel != null) {
                    addRun(mVel, xVel, yVel, time, current, id, cumDist, Utils.calcDistance(0.0, 0.0, netDistX, netDistY));
                    mVel = null;
                    xVel = null;
                    yVel = null;
                    time = 0.0;
                    cumDist = 0.0;
                    netDistX = 0.0;
                    netDistY = 0.0;
                    if (j < b - 1) {
                        lastDir = vels[i][j + 1][V_Th] > 90 && vels[i][j + 1][V_Th] < 270;
                    }
                } else {
                    lastDir = dir;
                }
            }
            if (mVel != null) {
                addRun(mVel, xVel, yVel, time, current, id, cumDist, Utils.calcDistance(0.0, 0.0, netDistX, netDistY));
            }
            runs.add(current);
        }
        int m = runs.size();
        double[][][] output = new double[m][][];
        for (int k = 0; k < m; k++) {
            ArrayList<double[]> record = runs.get(k);
            int size1 = record.size();
            output[k] = new double[size1][2];
            for (int j = 0; j < size1; j++) {
                double[] current = record.get(j);
                output[k][j] = new double[current.length];
                for (int i = 0; i < current.length; i++) {
                    output[k][j][i] = current[i];
                }
            }
        }
        return output;
    }

    private double[][] calcMSDs(double[][][] data, int minPointsForMSD, double framesPerSec) {
        int maxLength = -1;
        for (int traj = 0; traj < data.length; traj++) {
            if (data[traj].length > maxLength) {
                maxLength = data[traj].length;
            }
        }
        double[][] msds = new double[maxLength][data.length * 3 + 1];
        for (double[] d : msds) {
            Arrays.fill(d, Double.NaN);
        }
        for (int traj = 0; traj < data.length; traj++) {
            double[][] currentData = data[traj];
            if (currentData.length < 1) {
                continue;
            }
            double[][] tempData = new double[3][currentData.length];
            for (int time = 0; time < currentData.length; time++) {
                tempData[0][time] = currentData[time][_X_];
                tempData[1][time] = currentData[time][_Y_];
                tempData[2][time] = currentData[time][_T_];
            }
            double[][] currentMSD = (new DiffusionAnalyser(false)).calcMSD(-1, -1, tempData, minPointsForMSD, framesPerSec);
            if (currentMSD == null) {
                continue;
            }
            for (int time = 0; time < currentMSD[0].length; time++) {
                msds[time][0] = currentMSD[0][time];
                for (int i = 1; i < 4; i++) {
                    msds[time][traj * 3 + i] = currentMSD[i][time];
                }
            }
        }
        return msds;
    }

    private double[][] calcSpiderPlotData(double[][][] data) {
        int totalLength = 0;
        for (int i = 0; i < data.length; i++) {
            totalLength += data[i].length;
        }
        double[][] output = new double[totalLength][data.length + 1];
        for (int i = 0; i < output.length; i++) {
            Arrays.fill(output[i], Double.NaN);
        }
        int outIndex = 0;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                output[outIndex][0] = data[i][j][_X_] - data[i][0][_X_];
                output[outIndex][i + 1] = data[i][j][_Y_] - data[i][0][_Y_];
                outIndex++;
            }
        }
        return output;
    }

    private void addRun(DescriptiveStatistics mVel, DescriptiveStatistics xVel, DescriptiveStatistics yVel, double time, ArrayList<double[]> current, int id, double dist, double netDist) {
        double meanMag = mVel.getMean();
        double meanTheta = Utils.arcTan(xVel.getMean(), yVel.getMean());
        if (netDist > minDist) {
            current.add(new double[]{id, meanMag, meanTheta, netDist, dist, time / framesPerSec});
        }
    }

    public void saveData(double[][][] data, String filename, String[] headings, File dir) throws IOException {
        File file = new File(String.format("%s%s%s", dir, File.separator, filename));
        if (file.exists()) {
            file.delete();
        }
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(file), GenVariables.ISO), CSVFormat.EXCEL);
        printer.printRecord(((Object[]) headings));
        printer.close();
        for (int i = 0; i < data.length; i++) {
            double[][] d = data[i];
            DataWriter.saveValues(d, file, null, null, true);
        }
    }

    void saveMeanVels(double[][] meanVels, File dir) throws IOException {
        File velData = new File(String.format("%s%s%s", dir, File.separator, "Mean_Velocities.csv"));
        if (velData.exists()) {
            velData.delete();
        }
        String[] rowLabels = new String[meanVels.length];
        for (int i = 0; i < meanVels.length; i++) {
            rowLabels[i] = String.valueOf(idIndexMap.get(i));
        }
        DataWriter.saveValues(meanVels, velData, new String[]{"Track ID", String.format("Mag (%s)", MIC_PER_SEC), String.format("Theta (%c)", IJ.degreeSymbol), "Directionality"}, rowLabels, false);
    }

    void saveRunLengths(double[][][] runs, File dir) throws IOException {
        File velData = new File(String.format("%s%s%s", dir, File.separator, "Run_Lengths.csv"));
        if (velData.exists()) {
            velData.delete();
        }
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(velData), GenVariables.ISO), CSVFormat.EXCEL);
        printer.printRecord(((Object[]) new String[]{"Track ID", String.format("Mag (%s)", MIC_PER_SEC), String.format("Theta (%c)", IJ.degreeSymbol), "Net Distance", "Cumulative Distance", "Duration (s)"}));
        printer.close();
        for (int i = 0; i < runs.length; i++) {
            double[][] v = runs[i];
            DataWriter.saveValues(v, velData, null, null, true);
        }
    }

    void saveMSDs(double[][] msds, File parentOutputDirectory) throws IOException {
        String[] headings = new String[msds[0].length];
        headings[0] = "Time Step (s)";
        for (int i = 1; i < msds[0].length; i += 3) {
            int j = (i - 1) / 3;
            headings[i] = String.format("Mean Square Displacement (%s^2)_%d", MIC, j);
            headings[i + 1] = String.format("Standard Deviation_%d", j);
            headings[i + 2] = String.format("N_%d", j);
        }
        saveData(new double[][][]{msds}, MSD,
                headings, parentOutputDirectory);
    }

    void saveSpiderPlotData(double[][] data, File parentOutputDirectory) throws IOException {
        String[] headings = new String[data[0].length];
        headings[0] = String.format("X (%cm)", IJ.micronSymbol);
        for (int i = 1; i < data[0].length; i++) {
            headings[i] = String.format("net.calm.iaclasslibrary.Particle %d Y", i);
        }
        File spiderData = new File(String.format("%s%s%s", parentOutputDirectory, File.separator, "Spider_Plot_Data.csv"));
        if (spiderData.exists()) {
            spiderData.delete();
        }
        DataWriter.saveValues(data, spiderData, headings, null, false);
    }

    public String[] getFileHeadings(File inputFile, boolean labelledData) {
        ArrayList<String> headings = new ArrayList();
        try {
            DataReader.readFileHeadings(inputFile, CSVFormat.DEFAULT, headings, labelledData);
        } catch (Exception e) {
            GenUtils.error("Cannot read input file.");
            return null;
        }
        String[] headingsArray = new String[headings.size()];
        return headings.toArray(headingsArray);
    }

    public boolean showDialog(String[] headings) {
        String defaultX = INPUT_X_INDEX < headings.length ? headings[INPUT_X_INDEX] : headings[headings.length - 1];
        String defaultY = INPUT_Y_INDEX < headings.length ? headings[INPUT_Y_INDEX] : headings[headings.length - 1];
        String defaultF = INPUT_FRAME_INDEX < headings.length ? headings[INPUT_FRAME_INDEX] : headings[headings.length - 1];
        String defaultI = INPUT_ID_INDEX < headings.length ? headings[INPUT_ID_INDEX] : headings[headings.length - 1];
        GenericDialog gd = new GenericDialog(TITLE);
        gd.addNumericField("Minimum Velocity", minVel, 3, 5, MIC_PER_SEC);
        gd.addNumericField("Minimum Distance", minDist, 3, 5, MIC);
        gd.addNumericField("Temporal Resolution", framesPerSec, 3, 5, "Hz");
        gd.addNumericField("Smoothing Window", smoothingWindow, 0, 5, "Frames");
        gd.addChoice("Specify Column for X coordinates:", headings, defaultX);
        gd.addChoice("Specify Column for Y coordinates:", headings, defaultY);
        gd.addChoice("Specify Column for Frame Number:", headings, defaultF);
        gd.addChoice("Specify Column for Track ID:", headings, defaultI);
        gd.addCheckboxGroup(1, 3, new String[]{"Smooth Data", "Interpolate Data", "Labelled Data"}, new boolean[]{smooth, interpolate, labelledData});
        gd.showDialog();
        if (!gd.wasOKed()) {
            return false;
        }
        minVel = gd.getNextNumber();
        minDist = gd.getNextNumber();
        framesPerSec = gd.getNextNumber();
        INPUT_X_INDEX = gd.getNextChoiceIndex();
        INPUT_Y_INDEX = gd.getNextChoiceIndex();
        INPUT_FRAME_INDEX = gd.getNextChoiceIndex();
        INPUT_ID_INDEX = gd.getNextChoiceIndex();
        smooth = gd.getNextBoolean();
        interpolate = gd.getNextBoolean();
        labelledData = gd.getNextBoolean();
        return true;
    }

}
