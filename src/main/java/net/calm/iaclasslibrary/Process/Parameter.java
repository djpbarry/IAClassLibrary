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

import java.util.Objects;

/**
 * A library-declared, consumer-neutral parameter slot. Each process class
 * declares the parameters it consumes as {@code Parameter} constants carrying a
 * stable slot {@link #name()} (decoupled from any consumer's display label), a
 * {@link #type()}, a {@link #defaultValue()}, and a generic
 * {@link #defaultLabel()}. Consumers bind each slot to their own key/label/
 * tooltip via {@link ParamBinding}; the library never hard-codes a consumer's
 * vocabulary.
 *
 * <p>{@link #forChannel(int)} derives a per-channel slot for indexed parameters
 * (0-based), e.g. {@code SPOT_RADIUS.forChannel(3)} → name
 * {@code "spot_radius_channel_3"}.</p>
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
public final class Parameter {

    public enum Type {INT, DOUBLE, FLOAT, BOOLEAN, STRING}

    private final String name;
    private final Type type;
    private final Object defaultValue;
    private final String defaultLabel;

    public Parameter(String name, Type type, Object defaultValue, String defaultLabel) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.defaultLabel = defaultLabel;
    }

    public String name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public Object defaultValue() {
        return defaultValue;
    }

    public String defaultLabel() {
        return defaultLabel;
    }

    /**
     * Returns a per-channel variant of this slot. The channel index is 0-based
     * and is appended as {@code _channel_<i>} to the name; the default label is
     * only a fallback (consumers typically supply an explicit label via
     * {@link ParamBinding#bind(Parameter, String, String, String)}).
     */
    public Parameter forChannel(int channel) {
        return new Parameter(name + "_channel_" + channel, type, defaultValue,
                defaultLabel + " in Channel " + channel);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Parameter other)) {
            return false;
        }
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
