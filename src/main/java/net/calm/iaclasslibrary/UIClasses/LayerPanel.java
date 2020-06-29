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
package net.calm.iaclasslibrary.UIClasses;

import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import java.awt.Container;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import javax.swing.JPanel;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public abstract class LayerPanel extends JPanel implements GUIMethods {

    protected ExecutorService exec;
    protected final BioFormatsImg img;
    protected final Properties props;
    protected MultiThreadedProcess process;
    protected String[] propLabels = new String[]{"", "", "", "", "", "", "", "", "", ""};
    protected URI helpURI;

    public LayerPanel() {
        this(null, null, null, null, null);
    }

    public LayerPanel(Properties props, BioFormatsImg img, MultiThreadedProcess process, String[] propLabels, URI helpURI) {
        super();
        if (props != null) {
            this.props = props;
        } else {
            this.props = new Properties();
        }
        this.img = img;
        this.process = process;
        if (propLabels != null) {
            this.propLabels = propLabels;
        }
        this.helpURI = helpURI;
    }

    public void setProperties(Properties p, Container container) {
        PropertyExtractor.setProperties(p, container, PropertyExtractor.WRITE);
    }

    public boolean setVariables() {
        setProperties(props, this);
        setupProcess();
        return true;
    }

    public abstract void setupProcess();

    protected boolean openHelpPage(String errorMessage) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(helpURI);
            }
        } catch (IOException e) {
            GenUtils.error(errorMessage);
            return false;
        }
        return true;
    }

    public MultiThreadedProcess getProcess() {
        return process;
    }

    protected void previewButtonFocusLost(java.awt.event.FocusEvent evt) {
        if (process != null) {
            process.interrupt();
        }
    }

    protected void restartProcess() {
        this.process = process.duplicate();
    }
}
