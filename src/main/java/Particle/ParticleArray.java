package Particle;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
//import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 * @author barry05
 */
public class ParticleArray extends SpotCollection {

    private int depth;
    private ArrayList<ArrayList<Particle>> detections;
    private ConcurrentSkipListMap< Integer, Set< Spot>> content = new ConcurrentSkipListMap<>();

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
        Set< Spot> spots = content.get(level);
        if (null == spots) {
            spots = new HashSet<>();
            content.put(level, spots);
        }
        if (detection != null) {
            spots.add(detection);
            detection.putFeature(Spot.FRAME, Double.valueOf(level));
            detection.putFeature(VISIBLITY, ONE);
            if (detections == null || level > depth - 1) {
                return false;
            }
            detections.get(level).add(detection);
        }
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

    public ConcurrentSkipListMap<Integer, Set<Spot>> getContent() {
        return content;
    }

}
