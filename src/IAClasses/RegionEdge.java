package IAClasses;

import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author barry05
 */
public class RegionEdge {

    private final Region2 startVertex, endVertex;
    private double gradient, weight;
    private ArrayList<Pixel> gradPix;

    public RegionEdge(Region2 start, Region2 end) {
        startVertex = start;
        endVertex = end;
    }

    public void calcWeight(double alpha) {
        double vector1[] = {startVertex.getMean(), startVertex.getSigma()};
        double vector2[] = {endVertex.getMean(), endVertex.getSigma()};
        calcGrad();
        weight = (Utils.calcEuclidDist(vector1, vector2) + alpha * gradient)
                / (alpha + 1.0);
    }

    public Region2 getEndVertex() {
        return endVertex;
    }

    public Region2 getStartVertex() {
        return startVertex;
    }

    public double getWeight() {
        return weight;
    }

    public void calcGrad() {
        int l = gradPix.size();
        if (l < 1) {
            return;
        }
        double sum = 0.0;
        for (int i = 0; i < l; i++) {
            Pixel pix = (Pixel) gradPix.get(i);
            sum += pix.getZ();
        }
        gradient = sum / l;
    }

    public void buildGradPix(ImageProcessor gradImage) {
        LinkedList<int[]> startRegionBorder = new LinkedList(Arrays.asList(startVertex.getMaskOutline()));
        LinkedList<int[]> endRegionBorder = new LinkedList(Arrays.asList(endVertex.getMaskOutline()));
        int ls = startRegionBorder.size();
        int le = endRegionBorder.size();
        int l;
        LinkedList<int[]> points1, points2;
        gradPix = new ArrayList();
        if (le <= ls) {
            l = le;
            points1 = endRegionBorder;
            points2 = startRegionBorder;
        } else {
            l = ls;
            points1 = startRegionBorder;
            points2 = endRegionBorder;
        }
        for (int i = 0; i < l; i++) {
            int[] pix = points1.get(i);
            if (points2.contains(pix)) {
                gradPix.add(new Pixel(pix[0], pix[1], gradImage.getPixelValue(pix[0], pix[1])));
            }
        }
    }
}
