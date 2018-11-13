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
package Process;

import IO.BioFormats.BioFormatsImg;
import UtilClasses.GenUtils;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import ome.units.quantity.Length;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public abstract class MultiThreadedProcess extends Thread implements Callable<BioFormatsImg> {

    protected ExecutorService exec;
    protected BioFormatsImg img;
    protected Properties props;
    protected String[] propLabels;

    public MultiThreadedProcess() {

    }

    public abstract void setup(BioFormatsImg img, Properties props, String[] propLabels);

    public abstract void run();

    public void terminate(String errorMessage) {
        exec.shutdown();
        try {
            exec.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            GenUtils.logError(e, errorMessage);
        }
    }

    protected int[] getIntSigma(int series, String xLabel, String yLabel, String zLabel) {
        double[] sigma = getDoubleSigma(series, xLabel, yLabel, zLabel);
        return new int[]{(int) Math.round(sigma[0]),
            (int) Math.round(sigma[1]),
            (int) Math.round(sigma[2])};
    }

    protected double[] getDoubleSigma(int series, String xLabel, String yLabel, String zLabel) {
        double xySpatialRes = img.getXYSpatialRes(series).value().doubleValue();
        Length zLength = img.getZSpatialRes(series);
        double zSpatialRes = zLength != null ? zLength.value().doubleValue() : Double.parseDouble(props.getProperty(zLabel));
        return new double[]{Double.parseDouble(props.getProperty(xLabel)) / xySpatialRes,
            Double.parseDouble(props.getProperty(yLabel)) / xySpatialRes,
            Double.parseDouble(props.getProperty(zLabel)) / zSpatialRes};
    }

    public String[] getPropLabels() {
        return propLabels;
    }

    public BioFormatsImg call() {
        return img;
    }
}
