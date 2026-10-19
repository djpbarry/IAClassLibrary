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
package net.calm.iaclasslibrary.Process;

import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import ij.ImagePlus;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import ome.units.quantity.Length;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract base class for a processing step in a {@code ProcessPipeline}.
 * Extends {@link Thread} and implements {@link Callable}, running a single unit
 * of work that reads from {@code inputs} and writes to {@code outputDests}.
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public abstract class MultiThreadedProcess extends Thread implements Callable<BioFormatsImg> {

    protected ExecutorService exec;
    protected BioFormatsImg img;
    protected Properties props;
    protected String[] propLabels;
    protected ProcessParams params;
    protected MultiThreadedProcess[] inputs;
    protected final LinkedList<MultiThreadedProcess> outputDests;
    protected ImagePlus output;
    public static final String OUTPUT_SEP = "_";
    protected int series;
    protected int channel;
    protected int time;

    public MultiThreadedProcess(MultiThreadedProcess[] inputs) {
        this.inputs = inputs;
        this.outputDests = new LinkedList<>();
        updateInputsWithOutput();
    }

    public abstract void setup(BioFormatsImg img, Properties props, String[] propLabels);

    public abstract void run();

    public void setSeries(int series) {
        this.series = series;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    /**
     * Stores the legacy configuration pair and builds the typed facade over it.
     * Subclasses call this from {@code setup} instead of assigning
     * {@code props}/{@code propLabels} directly.
     */
    protected void initParams(Properties props, String[] propLabels) {
        this.props = props;
        this.propLabels = propLabels;
        this.params = new ProcessParams(props, propLabels);
    }

    /**
     * Loads pixel data for the configured series/channel/time into the managed
     * image, owning the dimension → load mapping. Equivalent to
     * {@code img.loadPixelData(series, cBegin, cEnd, 0, img.getSizeZ(series) - 1, time, time, null)}.
     */
    protected void loadPixels(int cBegin, int cEnd) {
        img.loadPixelData(series, cBegin, cEnd, 0, img.getSizeZ(series) - 1, time, time, null);
    }

    public void terminate(String errorMessage) {
        exec.shutdown();
        try {
            exec.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            GenUtils.logError(e, errorMessage);
        }
    }

    /**
     * Submits the given worker tasks to the managed executor and waits for them
     * to finish, so subclasses route work through the pool instead of spawning
     * and joining raw {@link Thread}s themselves.
     */
    protected void runWorkers(Runnable[] workers, String errorMessage) {
        for (Runnable worker : workers) {
            exec.execute(worker);
        }
        terminate(errorMessage);
    }

    protected int[] getCalibratedIntSigma(int series, String xLabel, String yLabel, String zLabel) {
        double[] sigma = getCalibratedDoubleSigma(series, xLabel, yLabel, zLabel);
        return new int[]{(int) Math.round(sigma[0]),
            (int) Math.round(sigma[1]),
            (int) Math.round(sigma[2])};
    }

    protected double[] getCalibratedDoubleSigma(int series, double[] sigmas) {
        double[] calibration = getCalibration(series);
        return new double[]{sigmas[0] / calibration[0],
            sigmas[1] / calibration[1],
            sigmas[2] / calibration[2]};
    }
    
    protected double[] getCalibratedDoubleSigma(int series, String xLabel, String yLabel, String zLabel) {
        double[] calibration = getCalibration(series);
        double[] sigma = getUncalibratedDoubleSigma(series, xLabel, yLabel, zLabel);
        return new double[]{sigma[0] / calibration[0],
            sigma[1] / calibration[1],
            sigma[2] / calibration[2]};
    }

    protected int[] getUncalibratedIntSigma(int series, String xLabel, String yLabel, String zLabel) {
        double[] sigma = getUncalibratedDoubleSigma(series, xLabel, yLabel, zLabel);
        return new int[]{(int) Math.round(sigma[0]),
            (int) Math.round(sigma[1]),
            (int) Math.round(sigma[2])};
    }

    protected double[] getUncalibratedDoubleSigma(int series, String xLabel, String yLabel, String zLabel) {
        return new double[]{Double.parseDouble(props.getProperty(xLabel)),
            Double.parseDouble(props.getProperty(yLabel)),
            Double.parseDouble(props.getProperty(zLabel))};
    }

    protected double[] getCalibration(int series) {
        Length xyLength = img.getXYSpatialRes(series);
        double xySpatialRes = 1.0;
        if (xyLength != null) {
            xySpatialRes = xyLength.value().doubleValue();
        }
        Length zLength = img.getZSpatialRes(series);
        double zSpatialRes = zLength != null ? zLength.value().doubleValue() : 1.0;
        return new double[]{xySpatialRes, xySpatialRes, zSpatialRes};
    }

    public String[] getPropLabels() {
        return propLabels;
    }

    protected String getString(int slot) {
        return params.getString(slot);
    }

    protected String getString(int slot, String defaultValue) {
        return params.getString(slot, defaultValue);
    }

    protected int getInt(int slot) {
        return params.getInt(slot);
    }

    protected int getInt(int slot, int defaultValue) {
        return params.getInt(slot, defaultValue);
    }

    protected double getDouble(int slot) {
        return params.getDouble(slot);
    }

    protected double getDouble(int slot, double defaultValue) {
        return params.getDouble(slot, defaultValue);
    }

    protected float getFloat(int slot) {
        return params.getFloat(slot);
    }

    protected float getFloat(int slot, float defaultValue) {
        return params.getFloat(slot, defaultValue);
    }

    protected boolean getBoolean(int slot) {
        return params.getBoolean(slot);
    }

    protected boolean getBoolean(int slot, boolean defaultValue) {
        return params.getBoolean(slot, defaultValue);
    }

    protected boolean has(int slot) {
        return params.has(slot);
    }

    public BioFormatsImg call() {
        return img;
    }

    public ImagePlus getOutput() {
        try {
            if (output == null) {
                this.start();
                this.join();
            }
        } catch (InterruptedException | IllegalThreadStateException e) {
            GenUtils.logError(e, "Failed to obtain process output.");
        }
        ImagePlus dup = output.duplicate();
        dup.setTitle(output.getTitle());
        return dup;
    }

    public abstract MultiThreadedProcess duplicate();

    final protected void updateInputsWithOutput() {
        if (inputs == null) {
            return;
        }
        for (MultiThreadedProcess input : inputs) {
            input.addOutputDest(this);
        }
    }

    final public void updateOutputDests(MultiThreadedProcess newProcess) {
        for (MultiThreadedProcess outputDest : outputDests) {
            outputDest.updateInput(this, newProcess);
        }
    }

    final protected void addOutputDest(MultiThreadedProcess output) {
        this.outputDests.add(output);
    }

    final protected void updateInput(MultiThreadedProcess oldProcess, MultiThreadedProcess newProcess) {
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i].equals(oldProcess)) {
                inputs[i] = newProcess;
                newProcess.addOutputDest(this);
            }
        }
    }

    protected void labelOutput(String basename, String label) {
        if (output == null) {
            output = new ImagePlus();
        }
        output.setTitle(constructOutputName(basename, label));
        if (img != null && img.getSizeT(series) > 1) {
            output.setTitle(String.format("%s%sT%d", output.getTitle(), OUTPUT_SEP, time));
        }
    }

    public static String constructOutputName(String basename, String label) {
        return String.format("%s%s%s", StringUtils.substringBefore(basename, OUTPUT_SEP), OUTPUT_SEP, label);
    }

}
