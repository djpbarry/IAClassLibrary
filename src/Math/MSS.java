/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Math;

import IAClasses.Utils;
import ij.measure.CurveFitter;
import java.util.Arrays;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class MSS {

    public static double calcMSS(double[][] points, double dt, double xySpatRes, double zSpatRes) {
        if (points == null) {
            return Double.NaN;
        }
        int length = points.length;
        int maxStepSize = (int) Math.round(length / 3.0);
        double timesteps[] = new double[maxStepSize];
        double g[] = new double[maxStepSize];
        double[] mss = new double[10];
        double[] dp = new double[10];
        Arrays.fill(g, 0.0);
        for (double[] point : points) {
            point[0] *= xySpatRes;
            point[1] *= xySpatRes;
            point[2] *= zSpatRes;
        }
        for (int power = 1; power <= 10; power++) {
            for (int dn = 1; dn <= maxStepSize; dn++) {
                for (int n = 0; n < length - dn - 1; n++) {
                    g[dn - 1] += 1.0 / (length - dn) * Math.pow(Utils.calcEuclidDist(points[n + dn], points[n]), power);
                }
                timesteps[dn - 1] = Math.log(dn * dt);
                g[dn - 1] = Math.log(g[dn - 1]);
//                System.out.println(dn + "," + timesteps[dn - 1] + "," + g[dn - 1]);
            }
            CurveFitter fitter = new CurveFitter(timesteps, g);
            fitter.doFit(CurveFitter.STRAIGHT_LINE);
            mss[power - 1] = (fitter.getParams())[1];
            dp[power - 1] = power;
//            System.out.println(dp[power - 1] + "," + mss[power - 1]);
//            System.out.println();
        }
        CurveFitter fitter = new CurveFitter(dp, mss);
        fitter.doFit(CurveFitter.STRAIGHT_LINE);
        return (fitter.getParams())[1];
    }
}
