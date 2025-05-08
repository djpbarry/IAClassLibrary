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
package net.calm.iaclasslibrary.IO.BioFormats;

import loci.formats.FormatException;
import loci.formats.ImageReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class BioFormatsFileLister {

    public static ArrayList<String> obtainValidFileList(File directory) {
        ArrayList<String> fileNames = new ArrayList();
        ImageReader reader = new ImageReader();
        File[] files = directory.listFiles();
        for (File f : files) {
            String id = f.getAbsolutePath();
            try {
                // Attempt to set the file to be read
                reader.setId(id);

                fileNames.add(f.getName());

                // Close the reader
                reader.close();
            } catch (FormatException | IOException e) {

            }
        }
        return fileNames;
    }
}
