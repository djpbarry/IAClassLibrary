/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Cell3D;

import ij.gui.Roi;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Nucleus3D extends CellRegion3D {

    private double[] centroid;

    public Nucleus3D() {

    }

    public Nucleus3D(Roi roi, double[] centroid) {
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
