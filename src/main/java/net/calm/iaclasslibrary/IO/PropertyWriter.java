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
package net.calm.iaclasslibrary.IO;

import net.calm.iaclasslibrary.TimeAndDate.TimeAndDate;
import net.calm.iaclasslibrary.UtilClasses.Utilities;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;

public class PropertyWriter {

    public static String FILENAME = "properties";

    public static void saveProperties(Properties props, String outputDir, String comment, boolean XML) throws IOException {
        if (props == null || outputDir == null) {
            return;
        }
        props.put("Time and Date", TimeAndDate.getCurrentTimeAndDate());
        String filename = XML ? String.format("%s.xml", FILENAME) : String.format("%s.txt", FILENAME);
        File dir = new File(outputDir);
        if (!dir.exists() && !dir.isDirectory()) {
            return;
        }
        File outputFile = new File(String.format("%s%s%s", outputDir, File.separator, filename));
        FileOutputStream stream = new FileOutputStream(outputFile);
        if (XML) {
            props.storeToXML(stream, comment);
        } else {
            props.store(stream, comment);
        }
        stream.close();
    }

    public static void loadProperties(Properties props, String label, File file) throws IOException, InterruptedException, InvocationTargetException {
        if (file == null || file.isDirectory()) {
            file = Utilities.getFile(file, label, false);
        }
        FileInputStream stream = new FileInputStream(file);
        String ext = FilenameUtils.getExtension(file.getName());
        boolean XML = ext.contains("xml") || ext.contains("XML");
        if (XML) {
            props.loadFromXML(stream);
        } else {
            props.load(stream);
        }
    }
}
