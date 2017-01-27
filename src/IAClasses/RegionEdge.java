package IAClasses;

import ij.process.ImageProcessor;
import java.util.ArrayList;
import java.util.List;

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
        calcGrad();
        weight = (Utils.calcEuclidDist(startVertex.getStatsArray(), endVertex.getStatsArray()) + alpha * gradient)
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
        List<Pixel> startRegionBorder = startVertex.getBorderPix();
        List<Pixel> endRegionBorder = endVertex.getBorderPix();
        gradPix = new ArrayList();
        int endIndex = endVertex.getIndex();
        for (Pixel pix : startRegionBorder) {
            if (pix.getNeighbouringRegionIndex() == endIndex) {
                gradPix.add(new Pixel(pix.getRoundedX(), pix.getRoundedY(), gradImage.getPixelValue(pix.getRoundedX(), pix.getRoundedY())));
            }
        }
        int startIndex = startVertex.getIndex();
        for (Pixel pix : endRegionBorder) {
            if (pix.getNeighbouringRegionIndex() == startIndex) {
                gradPix.add(new Pixel(pix.getRoundedX(), pix.getRoundedY(), gradImage.getPixelValue(pix.getRoundedX(), pix.getRoundedY())));
            }
        }
    }
}
