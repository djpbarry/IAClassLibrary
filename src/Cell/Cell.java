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

    private ArrayList<Particle> particles;
    private ArrayList<CellRegion> regions;
    private int ID;

    public Cell() {

    }

    public Cell(CellRegion region) {
        this.addCellRegion(region);
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

    public void addParticle(Particle p) {
        if (particles == null) {
            particles = new ArrayList();
        }
        particles.add(p);
    }

    public ArrayList<Particle> getParticles() {
        return particles;
    }

    public final boolean addCellRegion(CellRegion region) {
        if (regions == null) {
            regions = new ArrayList();
        }
        return regions.add(region);
    }

    public Nucleus getNucleus() {
        for (CellRegion region : regions) {
            if (region instanceof Nucleus) {
                return (Nucleus) region;
            }
        }
        return null;
    }

    public <T> T getRegionOfType(T r) {
        Class type = ((T) new Object()).getClass();
        for (CellRegion region : regions) {
            if (type.isInstance(region)) {
                return (T) region;
            }
        }
        return null;
    }
}
