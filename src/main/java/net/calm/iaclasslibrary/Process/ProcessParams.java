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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Resolved parameter values, keyed by {@link Parameter} slot name. Built from a
 * consumer's raw {@link Properties} through a {@link ParamBinding} (slot →
 * external key) and an optional alias table (legacy label → current key) so
 * saved files remain loadable.
 *
 * <p>Value precedence (per the GIANI agreement):</p>
 * <ul>
 *   <li>bound + present → the consumer's value (an empty string is a value);</li>
 *   <li>bound + missing ({@code null}) → fail loudly;</li>
 *   <li>unbound → the {@link Parameter#defaultValue() library default}.</li>
 * </ul>
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public final class ProcessParams {

    private final Map<String, String> values;
    private final Set<String> bound;

    private ProcessParams(Map<String, String> values, Set<String> bound) {
        this.values = values;
        this.bound = bound;
    }

    /**
     * Resolves values for every bound slot. {@code aliasTable} maps a legacy
     * label to the current external key; when a slot's current key is absent
     * from {@code props}, the legacy labels mapping to that key are tried.
     */
    public static ProcessParams fromProperties(Properties props, ParamBinding binding,
            Map<String, String> aliasTable) {
        Map<String, String> values = new HashMap<>();
        Set<String> bound = new HashSet<>();
        Map<String, List<String>> legacyByKey = new HashMap<>();
        if (aliasTable != null) {
            for (Map.Entry<String, String> alias : aliasTable.entrySet()) {
                legacyByKey.computeIfAbsent(alias.getValue(), k -> new ArrayList<>()).add(alias.getKey());
            }
        }
        for (ParamBinding.Entry entry : binding.entriesBySlotName().values()) {
            bound.add(entry.slotName());
            String value = props.getProperty(entry.key());
            if (value == null) {
                for (String legacy : legacyByKey.getOrDefault(entry.key(), List.of())) {
                    value = props.getProperty(legacy);
                    if (value != null) {
                        break;
                    }
                }
            }
            values.put(entry.slotName(), value);
        }
        return new ProcessParams(values, bound);
    }

    public boolean has(Parameter slot) {
        return bound.contains(slot.name());
    }

    public String getString(Parameter slot) {
        String value = raw(slot);
        return value != null ? value : (String) slot.defaultValue();
    }

    public int getInt(Parameter slot) {
        String value = raw(slot);
        return value != null ? Integer.parseInt(value) : ((Number) slot.defaultValue()).intValue();
    }

    public double getDouble(Parameter slot) {
        String value = raw(slot);
        return value != null ? Double.parseDouble(value) : ((Number) slot.defaultValue()).doubleValue();
    }

    public float getFloat(Parameter slot) {
        String value = raw(slot);
        return value != null ? Float.parseFloat(value) : ((Number) slot.defaultValue()).floatValue();
    }

    public boolean getBoolean(Parameter slot) {
        String value = raw(slot);
        return value != null ? Boolean.parseBoolean(value) : (Boolean) slot.defaultValue();
    }

    private String raw(Parameter slot) {
        if (!bound.contains(slot.name())) {
            return null;
        }
        String value = values.get(slot.name());
        if (value == null) {
            throw new IllegalStateException("Missing value for bound parameter '" + slot.name() + "'");
        }
        return value;
    }
}
