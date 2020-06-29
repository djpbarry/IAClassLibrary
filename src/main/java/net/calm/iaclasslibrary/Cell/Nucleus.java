/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.calm.iaclasslibrary.Cell;

import ij.gui.Roi;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Nucleus extends CellRegion {

    private double[] centroid;

    public Nucleus() {

    }

    public Nucleus(Roi roi, double[] centroid) {
        super(roi);
        this.centroid = centroid;
    }

    public double[] getCentroid() {
        return centroid;
    }

    public void setCentroid(double[] centroid) {
        this.centroid = centroid;
    }
}
