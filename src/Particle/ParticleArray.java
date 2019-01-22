package Particle;

import fiji.plugin.trackmate.SpotCollection;
import java.util.ArrayList;

/**
 *
 * @author barry05
 */
public class ParticleArray extends SpotCollection {

    private int depth;
    private ArrayList<ArrayList<Particle>> detections;

    public ParticleArray(int depth) {
        super();
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
        add(detection, level);
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
