/*
 * Copyright (C) 2017 Dave Barry <david.barry at crick.ac.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package IO;

import UtilClasses.GenVariables;
import ij.text.TextWindow;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Scanner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class DataWriter {

    /**
     *
     * @param tw
     * @throws IOException
     */
    public static void saveTextWindow(TextWindow tw, File dataFile, String headings) throws IOException {
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(dataFile), GenVariables.ISO), CSVFormat.EXCEL);
        int L = tw.getTextPanel().getLineCount();
        printLine(printer, headings);
        for (int l = 0; l < L; l++) {
            printLine(printer, tw.getTextPanel().getLine(l));
        }
        printer.close();
    }

    static void printLine(CSVPrinter printer, String line) throws IOException {
        Scanner scan = new Scanner(line).useDelimiter("\t");
        while (scan.hasNext()) {
            printer.print(scan.next());
        }
        printer.println();
    }

    public static void saveValues(double[] vals, File dataFile, String[] headings) throws IOException {
        saveValues(new double[][]{vals}, dataFile, headings);
    }

    public static void saveValues(double[][] vals, File dataFile, String[] headings) throws IOException {
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(dataFile), GenVariables.ISO), CSVFormat.EXCEL);
        int L = vals.length;
        if (headings != null) {
            printer.printRecord((Object[]) headings);
        }
        for (int l = 0; l < L; l++) {
            if (vals[l] != null) {
                for (double v : vals[l]) {
                    printer.print(v);
                }
                printer.println();
            }
        }
        printer.close();
    }

    public static Double[] getAverageValues(double[][] vals, int N) {
        int L = vals.length;
        DescriptiveStatistics[] ds = new DescriptiveStatistics[N];
        for (int l = 0; l < L; l++) {
            if (vals[l] != null) {
                for (int i = 0; i < N; i++) {
                    if (ds[i] == null) {
                        ds[i] = new DescriptiveStatistics();
                    }
                    ds[i].addValue(vals[l][i]);
                }
            }
        }
        Double[] meanVals = new Double[N + 1];
        meanVals[0] = new Double(ds[0].getN());
        for (int j = 0; j < N; j++) {
            meanVals[j + 1] = ds[j].getMean();
        }
        return meanVals;
    }

    public static String convertArrayToString(String seed, Object[] input, String delimiter) {
        String output = seed;
        if (output == null) {
            output = new String();
        }
        for (Object s : input) {
            output = (output.concat(String.valueOf(s))).concat(delimiter);
        }
        return output;
    }
}
