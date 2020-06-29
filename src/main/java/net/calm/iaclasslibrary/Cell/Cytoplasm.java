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
public class Cytoplasm extends CellRegion {

    public Cytoplasm(){
        super();
    }
    
    public Cytoplasm(Roi roi) {
        super(roi);
    }
}
