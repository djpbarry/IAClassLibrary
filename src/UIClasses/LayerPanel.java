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
import java.awt.Container;
import java.util.Properties;
import javax.swing.JPanel;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class LayerPanel extends JPanel implements GUIMethods {

    protected BioFormatsImg img;
    protected Properties props;

    public LayerPanel() {
        super();
    }

    public BioFormatsImg getImg() {
        return img;
    }

    public void setImg(BioFormatsImg img) {
        this.img = img;
    }

    public void setProperties(Properties p, Container container) {
        props = PropertyExtractor.setProperties(new Properties(), container);
    }

    public boolean setVariables() {
        setProperties(props, this);
        return true;
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    public Properties getProps() {
        return props;
    }
    
    
}
