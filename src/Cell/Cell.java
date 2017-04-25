/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Cell;

import Particle.Particle;
import java.util.ArrayList;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Cell implements Comparable<Cell> {

    private Nucleus nucleus;
    private Cytoplasm cytoplasm;
    private ArrayList<Particle> particles;
    private int ID;

    public Cell() {

    }

    public Cell(Nucleus nucleus) {
        this.nucleus = nucleus;
    }

    public Nucleus getNucleus() {
        return nucleus;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int compareTo(Cell cell) {
        if (cell == null) {
            throw new NullPointerException();
        }
        if (!(cell instanceof Cell)) {
            throw new ClassCastException();
        }
        return this.ID - ((Cell) cell).getID();
    }
    
    public void addParticle(Particle p){
        if(particles==null)particles=new ArrayList();
        particles.add(p);
    }

    public ArrayList<Particle> getParticles() {
        return particles;
    }
    
}
