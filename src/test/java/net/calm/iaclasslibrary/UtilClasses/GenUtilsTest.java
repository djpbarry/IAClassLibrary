package net.calm.iaclasslibrary.UtilClasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenUtilsTest {

    @Test
    public void testDifferentiate() {
        assertArrayEquals(new double[]{0, 2, 2, 0}, GenUtils.differentiate(new double[]{1, 2, 3, 4}));
    }

    @Test
    public void testCheckRange() {
        assertEquals(4, GenUtils.checkRange(-1, 5));
        assertEquals(2, GenUtils.checkRange(5, 3));
        assertEquals(2, GenUtils.checkRange(2, 10));
    }

    @Test
    public void testCheckFileSep() {
        assertEquals("a_b_c_d_e_f_g_h_i_j", GenUtils.checkFileSep("a\\b/c:d*e?f\"g<h>i|j", '_'));
    }
}
