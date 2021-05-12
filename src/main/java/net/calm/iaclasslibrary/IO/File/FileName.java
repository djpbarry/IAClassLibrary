/*
 * Copyright (C) 2018 David Barry <david.barry at crick dot ac dot uk>
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
package net.calm.iaclasslibrary.IO.File;

import java.io.File;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class FileName {

    public static String makeValidFileName(String name) {
        String output = name;
        if (name == null) {
            return "net/calm/iaclasslibrary/Image";
        }
        int period = name.indexOf('.');
        if (period > -1) {
            output = name.substring(0, period);
        }
        return output.replaceAll("[\\\\/:*?\"<>|]", "");
    }

    public static String uniqueFileName(String path, String filename, String ext) {
        File output = new File(path, String.format("%s.%s", filename, ext));
        if (!output.exists()) return output.getAbsolutePath();
        int index = 1;
        File newOutput = output;
        while (newOutput.exists()) {
            newOutput = new File(String.format("%s%s%s_%d.%s", path, File.separator, filename, index, ext));
            index++;
        }
        return newOutput.getAbsolutePath();
    }
}
