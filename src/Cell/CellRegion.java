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
public class CellRegion {

    protected Cell parent;
    protected Roi roi;
    protected double[] centroid;

    public CellRegion() {

    }

    public Cell getParent() {
        return parent;
    }

    public void setParent(Cell parent) {
        this.parent = parent;
    }

    public Roi getRoi() {
        return roi;
    }

    public void setRoi(Roi roi) {
        this.roi = roi;
    }

    public double[] getCentroid() {
        return centroid;
    }

    public void setCentroid(double[] centroid) {
        this.centroid = centroid;
    }
    
}
