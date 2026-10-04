/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.calm.iaclasslibrary.Cell;

import net.calm.iaclasslibrary.Particle.Particle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Cell extends CellRegion implements Comparable<Cell>, Comparator<Cell> {

    private ArrayList<Particle> particles;
    private ArrayList<CellRegion> regions;
    private ArrayList<Cell> links;
    private int ID;

    public Cell() {

    }

    public Cell(int ID) {
        this.ID = ID;
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
        Objects.requireNonNull(cell);
        return this.ID - cell.getID();
    }

    public int compare(Cell cell1, Cell cell2) {
        return cell1.compareTo(cell2);
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

    public final boolean addCellRegion(CellRegion region) {
        if (regions == null) {
            regions = new ArrayList<>();
        }
        return regions.add(region);
    }

    public Nucleus getNucleus() {
        for (CellRegion region : regions) {
            if (region instanceof Nucleus nucleus) {
                return nucleus;
            }
        }
        return null;
    }

    public CellRegion getRegion(CellRegion regionType) {
        for (CellRegion region : regions) {
            if (regionType.getClass().isInstance(region)) {
                return region;
            }
        }
        return null;
    }

    public void addLink(Cell c) {
        if (links == null) {
            links = new ArrayList<>();
        }
        if (!links.contains(c)) {
            links.add(c);
        }
    }

    public ArrayList<Cell> getLinks() {
        return links;
    }
}
