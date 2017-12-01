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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class DataReader {

    public static double[][] readFile(File file, CSVFormat format, String[] colHeadings, String[] rowLabels) throws IOException {
        ArrayList<ArrayList<Double>> data = new ArrayList();
        CSVParser parser = CSVParser.parse(file, GenVariables.UTF8, format);
        ArrayList<String> headings = new ArrayList();
        ArrayList<String> rows = new ArrayList();
        int maxM = 0;
        for (CSVRecord record : parser) {
            int lineNumber = (int) parser.getCurrentLineNumber() - 1;
            int line = colHeadings == null ? lineNumber : lineNumber - 1;
            if (record.getRecordNumber() == 1 && colHeadings != null) {
                for (int j = 0; j < record.size(); j++) {
                    headings.add(record.get(j));
                }
            } else {
                int j = 0;
                if (rowLabels != null) {
                    rows.add(record.get(0));
                    j++;
                }
                if (data.size() <= line) {
                    data.add(new ArrayList());
                }
                for (; j < record.size(); j++) {
                    data.get(line).add(Double.parseDouble(record.get(j)));
                }
                if (j > maxM) {
                    maxM = j;
                }
            }
        }
        colHeadings = headings.toArray(colHeadings);
        rowLabels = rows.toArray(rowLabels);
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
}
