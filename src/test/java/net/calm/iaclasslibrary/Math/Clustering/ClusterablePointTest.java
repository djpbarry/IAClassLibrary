package net.calm.iaclasslibrary.Math.Clustering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ClusterablePointTest {

    @Test
    public void testGetPoint() {
        assertArrayEquals(new double[]{3.0, 4.0}, new ClusterablePoint(1.0, 3.0, 4.0).getPoint());
        assertArrayEquals(new double[]{3.0, 4.0}, new ClusterablePoint(new double[]{3.0, 4.0}).getPoint());
    }
}
