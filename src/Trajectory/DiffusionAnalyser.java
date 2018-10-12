/*
 * Copyright (C) 2018 David Barry <david.barry at crick dot ac dot uk>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package Trajectory;

import ij.IJ;
import ij.gui.Plot;
import ij.measure.CurveFitter;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class DiffusionAnalyser {

    private static Plot msdPlot;
    private static ArrayList<DescriptiveStatistics> globalMSD = new ArrayList<DescriptiveStatistics>();
    private static String plotLegend = "";
    private double diffCoeff;
    private final float D_SCALING = 4.0f;
    private final boolean showPlot;

    public DiffusionAnalyser(boolean showPlot) {
        this.showPlot = showPlot;
    }

    public double[][] calcMSD(int seg, int label, double[][] points, int MIN_POINTS_TO_AVERAGE, double timeRes) {
        int maxLength;
        double xval, yval;
        double[] xPoints = points[0], yPoints = points[1], tPoints = points[2];
        if (xPoints == null) {
            return null;
        }
        int length = xPoints.length;
        if (seg > 0) {
            maxLength = seg;
        } else {
            maxLength = length;
        }
        ArrayList<Double> timesteps = new ArrayList<>();
        ArrayList<double[]> msd = new ArrayList<>();
        for (int i = 0; i < maxLength; i++) {
            DescriptiveStatistics thisMSD = new DescriptiveStatistics();
            for (int j = 0; i + j < maxLength; j++) {
                int dt = (int) Math.round(tPoints[i + j] - tPoints[j]);
                if (dt == i) {
                    xval = Math.pow(xPoints[i + j] - xPoints[j], 2.0);
                    yval = Math.pow(yPoints[i + j] - yPoints[j], 2.0);
                    thisMSD.addValue(xval + yval);
                }
            }
            long N = thisMSD.getN();
            if (N >= MIN_POINTS_TO_AVERAGE) {
                timesteps.add(i / timeRes);
                msd.add(new double[]{thisMSD.getMean(), thisMSD.getStandardDeviation(), thisMSD.getN()});
            }
        }
        if (!(msd.size() > 0)) {
            return null;
        }
        if (msdPlot == null && showPlot) {
            msdPlot = new Plot("Mean Square Displacement",
                    "Time (s)", "Mean Square Displacement (" + IJ.micronSymbol + "m^2)");
            msdPlot.setLineWidth(3);
        }
        double[][] msdA = new double[4][msd.size()];
        for (int i = 0; i < msd.size(); i++) {
            msdA[0][i] = timesteps.get(i);
            msdA[1][i] = msd.get(i)[0];
            msdA[2][i] = msd.get(i)[1];
            msdA[3][i] = msd.get(i)[2];
            if (globalMSD.size() <= i) {
                globalMSD.add(new DescriptiveStatistics());
            }
            globalMSD.get(i).addValue(msd.get(i)[0]);
        }
        if (showPlot) {
            Random r = new Random();
            msdPlot.setColor(new Color(r.nextFloat(), r.nextFloat(), r.nextFloat()));
            msdPlot.addPoints(msdA[0], msdA[1], Plot.CONNECTED_CIRCLES);
            msdPlot.setLimitsToFit(false);
            plotLegend = ((plotLegend.concat("Particle ")).concat(String.valueOf(label))).concat("\n");
            msdPlot.addLegend(plotLegend);
            msdPlot.draw();
            msdPlot.show();
        }
        CurveFitter fitter = new CurveFitter(msdA[0], msdA[1]);
        fitter.doFit(CurveFitter.STRAIGHT_LINE);
        diffCoeff = (fitter.getParams())[1] / D_SCALING;
        return msdA;
    }

    public double getDiffCoeff() {
        return diffCoeff;
    }

    public static Plot getMsdPlot() {
        return msdPlot;
    }

    public void resetMSDPlot() {
        globalMSD = new ArrayList<DescriptiveStatistics>();
        msdPlot = null;
    }
}
