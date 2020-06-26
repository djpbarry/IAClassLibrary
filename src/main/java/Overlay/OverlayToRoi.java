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
package Overlay;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Overlay;
import ij.plugin.frame.RoiManager;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class OverlayToRoi {

    /*
    Copied from OverlayCommands
    */
    public static void toRoi(ImagePlus imp) {
        Overlay overlay = imp.getOverlay();
        if (overlay == null) {
            return;
        }
        RoiManager rm = RoiManager.getInstance2();
        if (rm == null) {
            rm = new RoiManager();
        }
        if (overlay.size() >= 4 && overlay.get(3).getPosition() != 0) {
            Prefs.showAllSliceOnly = true;
        }
        rm.runCommand("reset");
        rm.setEditMode(imp, false);
        for (int i = 0; i < overlay.size(); i++) {
            rm.add(imp, overlay.get(i), i + 1);
        }
        rm.setEditMode(imp, true);
        rm.runCommand("show all");
        imp.setOverlay(null);
    }
}
