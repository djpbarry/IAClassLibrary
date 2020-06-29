/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.calm.iaclasslibrary.Math;

import java.util.Random;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class Rand {

    /**
     * for i from n−1 downto 1 do j ← random integer such that 0 ≤ j ≤ i
     * exchange a[j] and a[i]
     */
    public static void durstenfeld(double[] array) {
        int n = array.length;
        Random rand = new Random();
        for (int i = n - 1; i >= 0; i--) {
            int j = rand.nextInt(i + 1);
            double temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    public static void bootstrap(double[] array1, double[] array2) {
        int n = array1.length;
        Random rand = new Random();
        double[] temp1 = new double[n];
        double[] temp2 = new double[n];
        for (int i = 0; i < n; i++) {
            int j = rand.nextInt(n);
            temp1[i] = array1[j];
            temp2[i] = array2[j];
        }
        System.arraycopy(temp1, 0, array1, 0, n);
        System.arraycopy(temp2, 0, array2, 0, n);
    }
}
