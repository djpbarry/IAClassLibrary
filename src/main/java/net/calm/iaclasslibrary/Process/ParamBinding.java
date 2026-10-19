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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consumer-owned mapping from a library {@link Parameter} slot to the
 * consumer's external key, label and tooltip. One binding is built per process
 * instance, so the same slot (e.g. a filter radius) can resolve to different
 * keys on different instances (nuclei vs cells). Shared slots are registered
 * with {@link #bindShared} on a base binding, then {@link #merge(ParamBinding)}
 * attaches that base to each instance binding so the shared key is looked up by
 * name rather than copied.
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public final class ParamBinding {

    /**
     * One bound slot: the library slot name plus the consumer's external key,
     * display label and tooltip.
     */
    public static final class Entry {
        private final String slotName;
        private final String key;
        private final String label;
        private final String tooltip;
        private final boolean shared;

        private Entry(String slotName, String key, String label, String tooltip, boolean shared) {
            this.slotName = slotName;
            this.key = key;
            this.label = label;
            this.tooltip = tooltip;
            this.shared = shared;
        }

        public String slotName() {
            return slotName;
        }

        public String key() {
            return key;
        }

        public String label() {
            return label;
        }

        public String tooltip() {
            return tooltip;
        }

        public boolean isShared() {
            return shared;
        }
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private ParamBinding parent;

    public ParamBinding bind(Parameter slot, String key) {
        return bind(slot, key, slot.defaultLabel(), null);
    }

    public ParamBinding bind(Parameter slot, String key, String label, String tooltip) {
        entries.put(slot.name(), new Entry(slot.name(), key, label, tooltip, false));
        return this;
    }

    public ParamBinding bindShared(Parameter slot, String key) {
        return bindShared(slot, key, slot.defaultLabel(), null);
    }

    public ParamBinding bindShared(Parameter slot, String key, String label, String tooltip) {
        entries.put(slot.name(), new Entry(slot.name(), key, label, tooltip, true));
        return this;
    }

    /**
     * Attaches a base binding (typically holding the shared slots) so lookups
     * fall through to it when a slot is not bound locally.
     */
    public ParamBinding merge(ParamBinding base) {
        this.parent = base;
        return this;
    }

    public String key(Parameter slot) {
        Entry entry = entry(slot.name());
        return entry == null ? null : entry.key();
    }

    public boolean isBound(Parameter slot) {
        return entry(slot.name()) != null;
    }

    /**
     * Returns the merged view of this binding and its parent, keyed by slot
     * name; local entries override the parent's.
     */
    public Map<String, Entry> entriesBySlotName() {
        Map<String, Entry> merged = new LinkedHashMap<>();
        if (parent != null) {
            merged.putAll(parent.entriesBySlotName());
        }
        merged.putAll(entries);
        return merged;
    }

    private Entry entry(String slotName) {
        Entry entry = entries.get(slotName);
        if (entry == null && parent != null) {
            entry = parent.entry(slotName);
        }
        return entry;
    }
}
