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

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class Dijkstra {

    static int index = 0;

    public static Graph calculateShortestPathFromSource(Graph graph, Node source) {
        source.setDistance(0);

        Set<Node> settledNodes = new HashSet<>();
        Set<Node> unsettledNodes = new HashSet<>();

        unsettledNodes.add(source);

        while (!unsettledNodes.isEmpty()) {
//                                drawGraph(graph);
            Node currentNode = getLowestDistanceNode(unsettledNodes);
            unsettledNodes.remove(currentNode);
            for (Entry< Node, int[][]> adjacencyPair : currentNode.getAdjacentNodes().entrySet()) {
                Node adjacentNode = adjacencyPair.getKey();
                Integer edgeWeight = adjacencyPair.getValue()[0].length;
                if (!settledNodes.contains(adjacentNode)) {
                    calculateMinimumDistance(adjacentNode, edgeWeight, currentNode);
                    unsettledNodes.add(adjacentNode);
                }
            }
            settledNodes.add(currentNode);
        }
        return graph;
    }

    private static Node getLowestDistanceNode(Set< Node> unsettledNodes) {
        Node lowestDistanceNode = null;
        int lowestDistance = Integer.MAX_VALUE;
        for (Node node : unsettledNodes) {
            int nodeDistance = node.getDistance();
            if (nodeDistance < lowestDistance) {
                lowestDistance = nodeDistance;
                lowestDistanceNode = node;
            }
        }
        return lowestDistanceNode;
    }

    private static void calculateMinimumDistance(Node evaluationNode,
            Integer edgeWeight, Node sourceNode) {
        Integer sourceDistance = sourceNode.getDistance();
        if (sourceDistance + edgeWeight < evaluationNode.getDistance()) {
            evaluationNode.setDistance(sourceDistance + edgeWeight);
            LinkedList<Node> shortestPath = new LinkedList<>(sourceNode.getShortestPath());
            shortestPath.add(sourceNode);
            evaluationNode.setShortestPath(shortestPath);
        }
    }

    private static void drawGraph(Graph graph) {
        ByteProcessor bp = new ByteProcessor(640, 480);
        bp.setColor(255);
        bp.fill();
        bp.setColor(0);
        Set<Node> nodes = graph.getNodes();
        for (Node n1 : nodes) {
//            bp.drawString(String.valueOf(n1.getDistance()), n1.getX(), n1.getY());
            for (Entry<Node, int[][]> e : n1.getAdjacentNodes().entrySet()) {
                Node n2 = e.getKey();
                int[][] path = e.getValue();
                int d2 = n2.getDistance();
                if (d2 == Integer.MAX_VALUE) {
                    d2 = -1;
                }
                bp.drawString(String.valueOf(d2), n2.getX(), n2.getY());
                for (int i = 0; i < path[0].length; i++) {
                    bp.drawPixel(path[0][i], path[1][i]);
                }
            }
        }
        IJ.saveAs(new ImagePlus("", bp), "PNG", "C:\\Users\\barryd\\debugging\\anamorf_debug\\graph_" + index++);
    }
}
