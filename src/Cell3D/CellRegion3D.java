/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Cell3D;

import Cell.Cell;
import ij.gui.Roi;
import ij.process.ImageStatistics;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DVoxels;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class CellRegion3D extends Object3DVoxels {

    protected Cell parent;
    protected Roi roi;
    protected ImageStatistics fluorStats;
    protected final String regionName;
    public static final String CELL = "Cell", NUCLEUS = "Nucleus", CYTO = "Cytoplasm", MISC = "Miscellaneous";

    public CellRegion3D() {
        this(null);
    }

    public CellRegion3D(Object3D object) {
        super(object);
        this.regionName = CellRegion3D.MISC;
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

    public ImageStatistics getFluorStats() {
        return fluorStats;
    }

    public void setFluorStats(ImageStatistics fluorStats) {
        this.fluorStats = fluorStats;
    }

    public String getRegionName() {
        return regionName;
    }

}
