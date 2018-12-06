/*
 * Copyright (C) 2018 David Barry <david.barry at crick dot ac dot uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package Graph;

import IAClasses.Utils;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class Node {

    public final static int BRANCH = 0, END = 1;
    private LinkedList<Node> shortestPath = new LinkedList<>();
    private int type, x, y;
    private int distance = Integer.MAX_VALUE;

    Map<Node, int[][]> adjacentNodes = new HashMap<>();

    public void addDestination(Node destination, int[][] path) {
        if (this.getSimpleDist(destination.getX(), destination.getY()) > 0) {
            adjacentNodes.put(destination, path);
        }
    }

    public Node(Node n) {
        this(n.getType(), n.getX(), n.getY());
        this.adjacentNodes = n.getAdjacentNodes();
        this.distance = n.getDistance();
        this.shortestPath = n.getShortestPath();
    }

    public Node(int type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public LinkedList<Node> getShortestPath() {
        return shortestPath;
    }

    public void setShortestPath(LinkedList<Node> shortestPath) {
        this.shortestPath = shortestPath;
    }

    public int getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getDist(int x, int y) {
        return Utils.calcDistance(x, y, this.x, this.y);
    }

    public int getSimpleDist(int x, int y) {
        return Math.max(Math.abs(this.x - x), Math.abs(this.y - y));
    }

    public Map<Node, int[][]> getAdjacentNodes() {
        return adjacentNodes;
    }

    public void setAdjacentNodes(Map<Node, int[][]> adjacentNodes) {
        this.adjacentNodes = adjacentNodes;
    }

    public boolean equals(Node n) {
        return this.getType() == n.getType()
                && this.getX() == n.getX()
                && this.getY() == n.getY();
    }

}
