package Particle;

import java.util.ArrayList;

/**
 *
 * @author barry05
 */
public class ParticleArray {

    private int depth;
    private ArrayList<ArrayList<Particle>> detections;

    public ParticleArray(int depth) {
        this.depth = depth;
        if (depth > 0) {
            detections = new ArrayList<>();
            for (int i = 0; i < depth; i++) {
                detections.add(new ArrayList<>());
            }
        } else {
            this.depth = 0;
            detections = null;
        }
    }

    public boolean addDetection(int level, Particle detection) {
        if (detections == null || level > depth - 1) {
            return false;
        }
        detections.get(level).add(detection);
        return true;
    }
    
    public boolean nullifyDetection(int level, int index) {
        if (detections == null || level > depth - 1) {
            return false;
        }
        Particle detection = null;
        detections.get(level).set(index, detection);
        return true;
    }
    
    public int getDepth() {
        return depth;
    }

    public ArrayList<Particle> getLevel(int level) {
        if (detections != null) {
            return detections.get(level);
        } else {
            return null;
        }
    }
}
