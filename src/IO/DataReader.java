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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class DataReader {

    public static double[][] readFile(File file, CSVFormat format) throws IOException {
        ArrayList<ArrayList<Double>> data = new ArrayList();
        CSVParser parser = CSVParser.parse(new InputStreamReader(new FileInputStream(file), GenVariables.UTF8), format);
        for (CSVRecord record : parser) {
            for (int j = 0; j < record.size(); j++) {
                if (data.size() <= j) {
                    data.add(new ArrayList());
                }
                data.get(j).add(Double.parseDouble(record.get(j)));
            }
        }
        int m = data.size(), n = data.get(0).size();
        double[][] output = new double[m][n];
        for (int j = 0; j < m; j++) {
            ArrayList<Double> current = data.get(j);
            for (int i = 0; i < n; i++) {
                output[j][i] = current.get(i);
            }
        }
        return output;
    }
}
