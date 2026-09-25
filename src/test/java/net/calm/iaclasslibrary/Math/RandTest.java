package net.calm.iaclasslibrary.Math;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RandTest {

    @Test
    public void testDurstenfeldIsPermutation() {
        double[] input = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] original = input.clone();
        Rand.durstenfeld(input);
        assertEquals(original.length, input.length);
        double[] sorted = input.clone();
        Arrays.sort(sorted);
        assertArrayEquals(original, sorted);
    }
}
