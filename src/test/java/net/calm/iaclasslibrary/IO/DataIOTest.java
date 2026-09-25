package net.calm.iaclasslibrary.IO;

import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataIOTest {

    @TempDir
    File tempDir;

    @Test
    public void testSaveValuesWritesExpectedHeadings() throws IOException {
        File file = new File(tempDir, "headings.csv");
        DataWriter.saveValues(new double[][]{{1.0, 2.0, 3.0}}, file,
                new String[]{"Col1", "Col2", "Col3"}, null, false);

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
        assertEquals("Col1,Col2,Col3", lines.get(0));
    }

    @Test
    public void testSaveValuesEncodesNaNAsBlank() throws IOException {
        File file = new File(tempDir, "nan.csv");
        DataWriter.saveValues(new double[][]{{1.0, Double.NaN, 3.0}}, file,
                new String[]{"A", "B", "C"}, null, false);

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
        assertEquals("1.0,\" \",3.0", lines.get(1));
    }

    @Test
    public void testReadFileHeadings() throws IOException {
        File file = new File(tempDir, "headings.csv");
        DataWriter.saveValues(new double[][]{{1.0, 2.0, 3.0}}, file,
                new String[]{"A", "B", "C"}, null, false);

        ArrayList<String> headings = new ArrayList<>();
        DataReader.readFileHeadings(file, CSVFormat.EXCEL, headings, false);
        assertEquals(List.of("A", "B", "C"), headings);
    }

    @Test
    public void testSaveAndReadRoundTrip() throws IOException {
        File file = new File(tempDir, "data.csv");
        double[][] input = {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
        DataWriter.saveValues(input, file, new String[]{"A", "B", "C"}, null, false);

        ArrayList<String> headings = new ArrayList<>();
        double[][] output = DataReader.readCSVFile(file, CSVFormat.EXCEL, headings, null);

        assertEquals(List.of("A", "B", "C"), headings);
        assertArrayEquals(input, output);
    }
}
