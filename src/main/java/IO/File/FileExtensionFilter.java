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
package IO.File;

import java.io.File;
import java.io.FilenameFilter;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class FileExtensionFilter implements FilenameFilter {

    private final String[] exts;

    public FileExtensionFilter(String[] exts) {
        this.exts = exts;
    }

    public boolean accept(File current, String name) {
        String ext = FilenameUtils.getExtension(name);
        for (String e : exts) {
            if (e.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }
}
