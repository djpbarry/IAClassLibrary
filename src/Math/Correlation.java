/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Math;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class Correlation {

    public static double spearman(double[] array1, double[] array2) {
        return (new SpearmansCorrelation()).correlation(array1, array2);
    }

    public static double[] randSpearman(double[] array1, double[] array2, double lp, double up, int N) {
        SpearmansCorrelation spearman = new SpearmansCorrelation();
        int m = array1.length, n = array2.length;
        double[] array3 = new double[m];
        double[] array4 = new double[n];
        System.arraycopy(array1, 0, array3, 0, m);
        System.arraycopy(array2, 0, array4, 0, n);
        double[] randCoeffs = new double[N];
        for (int i = 0; i < N; i++) {
            Rand.bootstrap(array3, array4);
            randCoeffs[i] = spearman.correlation(array3, array4);
        }
        Percentile p = new Percentile();
        return new double[]{p.evaluate(randCoeffs, lp), p.evaluate(randCoeffs, up)};
    }
}
