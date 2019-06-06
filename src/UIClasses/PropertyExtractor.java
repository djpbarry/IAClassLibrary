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

import java.awt.Component;
import java.awt.Container;
import java.util.Properties;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListModel;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class PropertyExtractor {

    public static final int READ = 0, WRITE = 1;

    public static void setProperties(final Properties props, Container container, int readWrite) {
        Component[] comps = container.getComponents();
        for (Component c : comps) {
            if (c instanceof Container) {
                if (readWrite == PropertyExtractor.WRITE && c instanceof GUIMethods) {
                    ((GUIMethods) c).setVariables();
                } else if (readWrite == PropertyExtractor.READ && c instanceof Updateable) {
                    ((Updateable) c).update();
                }
                setProperties(props, (Container) c, readWrite);
            }
//            if (!c.isEnabled()) {
//                continue;
//            }
            if (c instanceof JLabel) {
                JLabel label = ((JLabel) c);
                Component currentComponent = label.getLabelFor();
                if (currentComponent instanceof JTextField) {
                    if (readWrite == PropertyExtractor.WRITE) {
                        props.setProperty(label.getText(), ((JTextField) currentComponent).getText());
                    } else if (readWrite == PropertyExtractor.READ) {
                        ((JTextField) currentComponent).setText(props.getProperty(label.getText()));
                    }
                } else if (currentComponent instanceof JComboBox) {
                    if (readWrite == PropertyExtractor.WRITE) {
                        Object selectedItem = ((JComboBox) currentComponent).getSelectedItem();
                        if (selectedItem != null) {
                            props.setProperty(label.getText(), selectedItem.toString());
                        }
                    } else if (readWrite == PropertyExtractor.READ) {
                        ((JComboBox) currentComponent).setSelectedItem(props.getProperty(label.getText()));
                    }
                } else if (currentComponent instanceof JScrollPane) {
                    //TODO: Extract selections from property file
                    Component[] scrollPaneComps = ((JScrollPane) currentComponent).getViewport().getComponents();
                    if (!(scrollPaneComps.length > 1)) {
                        if (scrollPaneComps[0] instanceof JList) {
                            JList list = ((JList) scrollPaneComps[0]);
                            ListModel listModel = ((JList) scrollPaneComps[0]).getModel();
                            if (readWrite == PropertyExtractor.WRITE) {
                                int n = listModel.getSize();
                                int p = 0;
                                for (int i = 0; i < n; i++) {
                                    if (list.isSelectedIndex(i)) {
                                        p += (int) Math.pow(2, i);
                                    }
                                }
                                props.setProperty(label.getText(), String.valueOf(p));
                            }
                        }
                    }
                }
            } else if (c instanceof JToggleButton) {
                if (readWrite == PropertyExtractor.WRITE) {
                    props.setProperty(((JToggleButton) c).getText(), String.format("%b", ((JToggleButton) c).isSelected()));
                } else if (readWrite == PropertyExtractor.READ) {
                    ((JToggleButton) c).setSelected(Boolean.parseBoolean(props.getProperty(((JToggleButton) c).getText())));
                }
            }
        }
    }
}
