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
package net.calm.iaclasslibrary.Process;

import java.util.Properties;

/**
 * Typed facade over the legacy {@code Properties} + {@code String[] propLabels}
 * configuration pair. The external contract is unchanged: values are keyed by
 * the label strings in {@code propLabels}, and a parameter is addressed by its
 * positional slot (the named {@code int} constants on each process class).
 * Parsing happens in one place here instead of at every read site.
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public class ProcessParams {

    private final Properties props;
    private final String[] propLabels;

    public ProcessParams(Properties props, String[] propLabels) {
        this.props = props;
        this.propLabels = propLabels;
    }

    public String getString(int slot) {
        return getString(slot, null);
    }

    public String getString(int slot, String defaultValue) {
        String value = getRaw(slot);
        return value != null ? value : defaultValue;
    }

    public int getInt(int slot) {
        return Integer.parseInt(getRaw(slot));
    }

    public int getInt(int slot, int defaultValue) {
        String value = getRaw(slot);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(int slot) {
        return Double.parseDouble(getRaw(slot));
    }

    public double getDouble(int slot, double defaultValue) {
        String value = getRaw(slot);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public float getFloat(int slot) {
        return Float.parseFloat(getRaw(slot));
    }

    public float getFloat(int slot, float defaultValue) {
        String value = getRaw(slot);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(int slot) {
        return Boolean.parseBoolean(getRaw(slot));
    }

    public boolean getBoolean(int slot, boolean defaultValue) {
        String value = getRaw(slot);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Reports whether the slot is present in the label array, i.e. the consumer
     * supplied a label for it. Optional parameters (e.g. StarDist / ilastik)
     * are declared but left {@code null} in {@code propLabels} when unused.
     */
    public boolean has(int slot) {
        return slot >= 0 && slot < propLabels.length && propLabels[slot] != null;
    }

    private String getRaw(int slot) {
        return props.getProperty(propLabels[slot]);
    }
}
