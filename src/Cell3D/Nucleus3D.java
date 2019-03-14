/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Cell3D;

import ij.gui.Roi;
import mcib3d.geom.Object3D;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Nucleus3D extends CellRegion3D {

    private double[] centroid;

    public Nucleus3D() {

    }
    
    public Nucleus3D(Object3D object){
        super(object);
    }

    public Nucleus3D(Roi roi, double[] centroid) {
        super(roi);
        this.centroid = centroid;
    }

    public double[] getCentroid() {
        return getCenterAsArray();
    }

    public void setCentroid(double[] centroid) {
        this.centroid = centroid;
    }
}
