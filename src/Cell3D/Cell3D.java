/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Cell3D;

import Particle.Particle;
import java.util.ArrayList;
import java.util.Comparator;
import mcib3d.geom.Object3DVoxels;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Cell3D extends Object3DVoxels implements Comparator<Cell3D> {

    private ArrayList<Particle> particles;
    private ArrayList<CellRegion3D> regions;
    private ArrayList<Cell3D> links;
    private int ID;

    public Cell3D() {

    }

    public Cell3D(int ID) {
        this.ID = ID;
    }

    public Cell3D(CellRegion3D region) {
        this.addCellRegion(region);
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int compareTo(Cell3D cell) {
        if (cell == null) {
            throw new NullPointerException();
        }
        if (!(cell instanceof Cell3D)) {
            throw new ClassCastException();
        }
        return this.ID - cell.getID();
    }

    public int compare(Cell3D cell1, Cell3D cell2) {
        if (cell1 == null || cell2 == null) {
            throw new NullPointerException();
        }
        if (!(cell1 instanceof Cell3D && cell2 instanceof Cell3D)) {
            throw new ClassCastException();
        }
        return cell1.getID() - cell2.getID();
    }

    public void addParticle(Particle p) {
        if (particles == null) {
            particles = new ArrayList<>();
        }
        particles.add(p);
    }

    public ArrayList<Particle> getParticles() {
        return particles;
    }

    public final boolean addCellRegion(CellRegion3D region) {
        if (regions == null) {
            regions = new ArrayList<>();
        }
        return regions.add(region);
    }

    public Nucleus3D getNucleus() {
        for (CellRegion3D region : regions) {
            if (region instanceof Nucleus3D) {
                return (Nucleus3D) region;
            }
        }
        return null;
    }

    public CellRegion3D getRegion(CellRegion3D regionType) {
        for (CellRegion3D region : regions) {
            if (regionType.getClass().isInstance(region)) {
                return region;
            }
        }
        return null;
    }

    public void addLink(Cell3D c) {
        if (links == null) {
            links = new ArrayList<>();
        }
        if (!links.contains(c)) {
            links.add(c);
        }
    }

    public ArrayList<Cell3D> getLinks() {
        return links;
    }

}
