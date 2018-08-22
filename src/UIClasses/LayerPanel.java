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
package UIClasses;

import IO.BioFormats.BioFormatsImg;
import Process.MultiThreadedProcess;
import java.awt.Container;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import javax.swing.JPanel;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class LayerPanel extends JPanel implements GUIMethods {

    protected final ExecutorService exec;
    protected final BioFormatsImg img;
    protected final Properties props;
    protected MultiThreadedProcess process;

    public LayerPanel() {
        this(null, null, null);
    }

    public LayerPanel(Properties props, BioFormatsImg img, ExecutorService exec) {
        super();
        this.props = props;
        this.img = img;
        this.exec = exec;
    }

    public void setProperties(Properties p, Container container) {
        PropertyExtractor.setProperties(p, container);
    }

    public boolean setVariables() {
        setProperties(props, this);
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

}
