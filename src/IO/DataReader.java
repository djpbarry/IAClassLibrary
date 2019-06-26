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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class DataReader {

    public static double[][] readCSVFile(File file, CSVFormat format, ArrayList<String> colHeadings, ArrayList<String> rowLabels) throws IOException {
        ArrayList<ArrayList<Double>> data = new ArrayList();
        CSVParser parser = CSVParser.parse(file, GenVariables.UTF8, format);
//        ArrayList<String> headings = new ArrayList();
//        ArrayList<String> rows = new ArrayList();
        int maxM = 0;
        for (CSVRecord record : parser) {
            int lineNumber = (int) parser.getCurrentLineNumber() - 1;
            int line = colHeadings == null ? lineNumber : lineNumber - 1;
            if (record.getRecordNumber() == 1 && colHeadings != null) {
                for (int j = rowLabels == null ? 0 : 1; j < record.size(); j++) {
                    colHeadings.add(record.get(j));
                }
            } else {
                int j = 0;
                if (rowLabels != null) {
                    rowLabels.add(record.get(0));
                    j++;
                }
                if (data.size() <= line) {
                    data.add(new ArrayList());
                }
                for (; j < record.size(); j++) {
                    double d;
                    try {
                        d = Double.parseDouble(record.get(j));
                    } catch (NumberFormatException e) {
                        d = Double.NaN;
                    }
                    data.get(line).add(d);
                }
                if (j > maxM) {
                    maxM = j;
                }
            }
        }
        if (rowLabels != null) {
            maxM--;
        }
//        colHeadings = headings.toArray(colHeadings);
//        rowLabels = rows.toArray(rowLabels);
        int m = data.size();
        double[][] output = new double[m][maxM];
        for (int j = 0; j < m; j++) {
            ArrayList<Double> current = data.get(j);
            int n = current.size();
            for (int i = 0; i < n; i++) {
                output[j][i] = current.get(i);
            }
        }
        return output;
    }

    public static double[][][] readTabbedFile(File file) throws IOException {
        ArrayList<ArrayList<ArrayList<Double>>> data = new ArrayList();
        Scanner scan = new Scanner(file, GenVariables.UTF8_NAME);
        int maxM = 0;
        int lineCount = 0;
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            Scanner lineScan = new Scanner(line);
            lineScan.useDelimiter("\t");
            ArrayList<ArrayList<Double>> currentRecord;
            if (data.size() <= lineCount) {
                currentRecord = new ArrayList();
                data.add(currentRecord);
            } else {
                currentRecord = data.get(lineCount);
            }
            ArrayList<Double> lineData = new ArrayList();
            while (lineScan.hasNextDouble()) {
                double d = lineScan.nextDouble();
                lineData.add(d);
            }
            if (lineData.size() > 0) {
                currentRecord.add(lineData);
            } else if (currentRecord.size() > 0) {
                lineCount++;
                if (data.get(lineCount - 1).size() > maxM) {
                    maxM = data.get(lineCount - 1).size();
                }
            }
        }
        int m = data.size();
        double[][][] output = new double[m][][];
        for (int k = 0; k < m; k++) {
            ArrayList<ArrayList<Double>> record = data.get(k);
            int size1 = record.size();
            output[k] = new double[size1][];
            for (int j = 0; j < size1; j++) {
                ArrayList<Double> current = record.get(j);
                int size2 = current.size();
                output[k][j] = new double[size2];
                for (int i = 0; i < size2; i++) {
                    output[k][j][i] = current.get(i);
                }
            }
        }
        return output;
    }

    public static void readFileHeadings(File file, CSVFormat format, ArrayList<String> colHeadings, boolean labelled) throws IOException {
        CSVParser parser = CSVParser.parse(file, GenVariables.UTF8, format);
        CSVRecord record = parser.getRecords().get(0);
        for (int j = labelled ? 1 : 0; j < record.size(); j++) {
            colHeadings.add(record.get(j));
        }
    }

}
