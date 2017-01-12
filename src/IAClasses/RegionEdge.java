package IAClasses;

import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author barry05
 */
public class RegionEdge {

    private Region2 startVertex, endVertex;
    private double gradient, weight;
    private ArrayList<Pixel> gradPix;

    public RegionEdge(Region2 start, Region2 end) {
        startVertex = start;
        endVertex = end;
    }

    public void calcWeight(double alpha) {
        double mfd1[] = startVertex.getMfD();
        double mfd2[] = endVertex.getMfD();
        double vector1[] = {startVertex.getMean(), startVertex.getSigma()};
        double vector2[] = {endVertex.getMean(), endVertex.getSigma()};
        calcGrad();
        weight = (Utils.calcEuclidDist(vector1, vector2) + alpha * gradient)
                / (alpha + 1.0);
        return;
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
        return;
    }

    public void buildGradPix(ImageProcessor gradImage) {
        int ls = startVertex.getBorderPix().size();
        int le = endVertex.getBorderPix().size();
        int l;
        LinkedList<Pixel> points1, points2;
        gradPix = new ArrayList<Pixel>();
        if (le <= ls) {
            l = le;
            points1 = endVertex.getBorderPix();
            points2 = startVertex.getBorderPix();
        } else {
            l = ls;
            points1 = startVertex.getBorderPix();
            points2 = endVertex.getBorderPix();
        }
        for (int i = 0; i < l; i++) {
            Pixel pix = points1.get(i);
            if (points2.contains(pix)) {
                int x = pix.getRoundedX();
                int y = pix.getRoundedY();
                double z = gradImage.getPixelValue(x, y);
                gradPix.add(new Pixel(x, y, z));
            }
        }
    }
}
