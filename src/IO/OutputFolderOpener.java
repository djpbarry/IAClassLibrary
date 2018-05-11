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

import UtilClasses.GenUtils;
import ij.IJ;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.JFileChooser;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class OutputFolderOpener implements Runnable {

    private String title;
    private File currentDirectory;
    boolean addExitOption;
    private File newDirectory;

    public OutputFolderOpener(String title, File currentDirectory, boolean addExitOption) {
        this.title = title;
        this.currentDirectory = currentDirectory;
        this.addExitOption = addExitOption;
    }

    public void run() {
        openDirectory();
    }

    public void openDirectory() {
        boolean validDirectory = false;
        if (title == null) {
            title = "Select Directory";
        }
        while (!validDirectory) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(title);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            if (currentDirectory != null) {
                fileChooser.setCurrentDirectory(currentDirectory);
            }
            int result;
            try {
                result = fileChooser.showDialog(IJ.getInstance(), "Ok");
            } catch (HeadlessException e) {
                GenUtils.error(e.toString());
                result = JFileChooser.ERROR_OPTION;
            }
            switch (result) {
                case JFileChooser.CANCEL_OPTION:
                    Toolkit.getDefaultToolkit().beep();
                    boolean exit = addExitOption ? IJ.showMessageWithCancel("Exit", "Do you wish to exit?") : true;
                    if (exit) {
                        return;
                    }
                    break;
                case JFileChooser.APPROVE_OPTION:
                    newDirectory = fileChooser.getSelectedFile();
                    if (!(newDirectory.isDirectory() && newDirectory.exists())) {
                        IJ.showMessage("Invalid Directory!");
                        validDirectory = false;
                    } else {
                        validDirectory = true;
                    }
                    break;
                default:
                    GenUtils.error("Error opening output directory.");
                    return;
            }
        }
    }

    public File getNewDirectory() {
        return newDirectory;
    }

}
