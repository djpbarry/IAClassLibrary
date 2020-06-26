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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class Graph {

    private Set<Node> nodes = new HashSet<>();

    public Graph() {

    }

    public Graph(Graph graph) {
        for (Node n : graph.getNodes()) {
            this.addNode(new Node(n));
        }
    }

    public void addNode(Node nodeA) {
        nodes.add(nodeA);
    }

    // getters and setters 
    public Set<Node> getNodes() {
        return nodes;
    }

    public void resetNodes() {
        for (Node n : getNodes()) {
            n.setDistance(Integer.MAX_VALUE);
            n.setShortestPath(new LinkedList());
        }
    }
}
