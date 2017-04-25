/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Cell;

import ij.gui.Roi;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Nucleus extends CellRegion {

    public Nucleus() {

    }

    public Nucleus(Roi roi, double[] centroid) {
        this.roi = roi;
        this.centroid = centroid;
    }
}
