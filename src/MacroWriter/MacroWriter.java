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
package MacroWriter;

import UtilClasses.GenUtils;
import ij.IJ;
import java.io.File;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class MacroWriter {

    public static void write() {
        String imagej = null;
        if (IJ.getInstance() == null) {
            imagej = "C:/users/barryd/fiji-nojre/Fiji.app/";
        } else {
            imagej = IJ.getDirectory("imagej");
        }
        File dir = GenUtils.createDirectory(imagej + "plugins/Scripts/Plugins/AutoRun", true);
        String content = "filename = \"./plugins/Scripts/CellMotilityWarningIssued.txt\";\n"
                + "if(!File.exists(filename)){\n"
                + "	showMessage(\"<html><h1>Attention: Action Required</h1><p>Dear Valued CellMotility PlugIn User,</p>\"\n"
                + "	+\"<p>In the near future, all plugins associated with the CellMotility update site will be migrated over to a new update site called CALM, to reflect my new position at Crick Advanced Light Microscopy.</p>\"\n"
                + "	+\"<p>In order to ensure that you always have the latest version of plugins such as <a href=\\\"https://bitbucket.org/djpbarry/adapt/\\\">ADAPT</a> and <a href=\\\"https://bitbucket.org/djpbarry/particletracker/\\\">Particle Tracker</a>, please</p>\"\n"
                + "	+\"ensure that you add CALM to your list of subscribed update sites by going to <i>Help > Update > Manage Update Sites</i> and ticking the box next to CALM.</p>\"\n"
                + "	+\"<p>Should you have any questions, please do not hesitate to contact me.</p>\"\n"
                + "	+\"<p>Thanks!</p>\"\n"
                + "	+\"<p>Dave.</p><br>\"\n"
                + "	+\"<p><i>Dave Barry,<br>Image Analyst,<br>Crick Advanced Light Microscopy Facility (CALM),<br>The Francis Crick Institute,<br>London NW1 1AT,<br>The non-Brexity part of the UK.</i></p><p><i>david.barry@crick.ac.uk</i></p>\");\n"
                + "	File.saveString(\"Cell Motility Warning Issued\",filename);\n"
                + "}";
        String path = dir.getAbsolutePath() + "/CellMigration.ijm";
        IJ.saveString(content, path);
    }
}
