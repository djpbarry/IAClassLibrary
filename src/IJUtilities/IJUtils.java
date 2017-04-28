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
package IJUtilities;

import ij.IJ;
import ij.WindowManager;
import ij.plugin.frame.RoiManager;

public class IJUtils {

    public static void resetRoiManager() {
        RoiManager instance = RoiManager.getInstance();
        if (instance != null) {
            instance.reset();
        }
    }

    public static void hideAllImages() {
        if (IJ.getInstance() == null) {
            return;
        }
        int[] ids = WindowManager.getIDList();
        for (int i : ids) {
            WindowManager.getImage(i).hide();
        }
    }

    public static void closeAllOpenImages() {
        if (IJ.getInstance() == null) {
            return;
        }
        int[] ids = WindowManager.getIDList();
        for (int i : ids) {
            WindowManager.getImage(i).hide();
        }
    }
}
