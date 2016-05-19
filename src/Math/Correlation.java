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

    public static void spearman(double[] array1, double[] array2, int N) {
        int m = array1.length, n = array2.length;
        SpearmansCorrelation spearman = new SpearmansCorrelation();
        double coef = spearman.correlation(array1, array2);
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
//        NaturalRanking nr = new NaturalRanking();
//        double[] rank1 = nr.rank(array1);
//        double[] rank2 = nr.rank(array2);
//        Plot plot = new Plot("Ranking Correlation", "Velocity", "Change in Fluorescence", rank2, rank1);
//        plot.show();
        System.out.println(" Spearman: " + String.valueOf(coef)
                + " (" + String.valueOf(p.evaluate(randCoeffs, 0.5))
                + " - " + String.valueOf(p.evaluate(randCoeffs, 99.5)) + ")");
    }
}
