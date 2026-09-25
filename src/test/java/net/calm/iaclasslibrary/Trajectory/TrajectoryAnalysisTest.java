package net.calm.iaclasslibrary.Trajectory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TrajectoryAnalysisTest {

    @TempDir
    File tempDir;

    private TrajectoryAnalysis newAnalysis() {
        return new TrajectoryAnalysis(0.01, 0.0, 3.0, 1, false, false, false, true, false,
                new int[]{0, 1, 2, 3});
    }

    @Test
    public void testProcessDataGroupsRowsByTrackId() {
        TrajectoryAnalysis analysis = newAnalysis();
        double[][] input = {
                {0, 10, 0, 1},
                {1, 11, 1, 1},
                {5, 20, 0, 2},
        };

        double[][][] result = analysis.processData(input);

        assertEquals(2, result.length);
        assertArrayEquals(new double[][]{{0, 10, 0, 1}, {1, 11, 1, 1}}, result[0]);
        assertArrayEquals(new double[][]{{5, 20, 0, 2}}, result[1]);
    }

    @Test
    public void testSaveMSDsWritesExpectedHeadings() throws IOException {
        TrajectoryAnalysis analysis = newAnalysis();
        analysis.saveMSDs(new double[1][4], tempDir);

        File file = new File(tempDir, TrajectoryAnalysis.MSD);
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
        assertEquals("Time Step (s),Mean Square Displacement (µm^2)_0,Standard Deviation_0,N_0", lines.get(0));
    }

    @Test
    public void testSaveMeanVelsWritesExpectedHeadings() throws IOException {
        TrajectoryAnalysis analysis = newAnalysis();
        analysis.processData(new double[][]{{0, 10, 0, 1}, {1, 11, 1, 1}});

        analysis.saveMeanVels(new double[][]{{1.5, 45.0, 0.9}}, tempDir);

        File file = new File(tempDir, "Mean_Velocities.csv");
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
        assertEquals("Track ID,Mag (µm/s),Theta (°),Directionality", lines.get(0));
    }
}
