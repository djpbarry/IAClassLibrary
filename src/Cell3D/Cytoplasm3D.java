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
public class Cytoplasm3D extends CellRegion3D {

    public Cytoplasm3D(){
        super();
    }
    
    public Cytoplasm3D(Object3D object){
        super(object);
    }
    
}
