/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.calm.iaclasslibrary.Cell3D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DVoxels;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Cell3D extends Object3DVoxels implements Comparator<Cell3D> {

    private ArrayList<ArrayList<Object3D>> spots;
    private Nucleus3D nucleus;
    private Cytoplasm3D cytoplasm;
    private ArrayList<Cell3D> links;
    private int ID;
    LinkedHashMap<Integer, Integer> spotIndexToChannelMap = new LinkedHashMap<>();

    public Cell3D() {

    }

    public Cell3D(int ID) {
        this.ID = ID;
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

    public void addSpot(Spot3D s, int channel) {
        if (spots == null) {
            spots = new ArrayList<>();
        }
        Integer spotIndex = spotIndexToChannelMap.get(channel);
        if (spotIndex == null) {
            spotIndexToChannelMap.put(channel, spots.size());
            spotIndex = spotIndexToChannelMap.get(channel);
            spots.add(new ArrayList<>());
        }
        spots.get(spotIndex).add(s);
    }

    public ArrayList<ArrayList<Object3D>> getSpots() {
        return spots;
    }

    public Nucleus3D getNucleus() {
        return nucleus;
    }

    public void setNucleus(Nucleus3D nucleus) {
        this.nucleus = nucleus;
    }

    public Cytoplasm3D getCytoplasm() {
        return cytoplasm;
    }

    public void setCytoplasm(Cytoplasm3D cytoplasm) {
        this.cytoplasm = cytoplasm;
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
