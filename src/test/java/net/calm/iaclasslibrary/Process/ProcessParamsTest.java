package net.calm.iaclasslibrary.Process;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessParamsTest {

    private static final Parameter SERIES = new Parameter("series", Parameter.Type.INT, 0, "Series");
    private static final Parameter CHANNEL = new Parameter("channel", Parameter.Type.INT, 0, "Channel");
    private static final Parameter RADIUS = new Parameter("radius", Parameter.Type.DOUBLE, 1.0, "Radius");
    private static final Parameter ENABLED = new Parameter("enabled", Parameter.Type.BOOLEAN, false, "Enabled");
    private static final Parameter NAME = new Parameter("name", Parameter.Type.STRING, "dflt", "Name");

    private static ParamBinding binding() {
        return new ParamBinding()
                .bind(SERIES, "Preview Series")
                .bind(CHANNEL, "Nuclear Channel")
                .bind(RADIUS, "Filter Radius")
                .bind(ENABLED, "Enable Filter")
                .bind(NAME, "Output Name");
    }

    @Test
    public void testFromPropertiesResolvesThroughBinding() {
        Properties props = new Properties();
        props.setProperty("Preview Series", "2");
        props.setProperty("Nuclear Channel", "3");
        ProcessParams params = ProcessParams.fromProperties(props, binding(), null);
        assertEquals(2, params.getInt(SERIES));
        assertEquals(3, params.getInt(CHANNEL));
    }

    @Test
    public void testTypedGetters() {
        Properties props = new Properties();
        props.setProperty("Filter Radius", "2.5");
        props.setProperty("Enable Filter", "true");
        props.setProperty("Output Name", "result");
        ProcessParams params = ProcessParams.fromProperties(props, binding(), null);
        assertEquals(2.5, params.getDouble(RADIUS), 1e-9);
        assertTrue(params.getBoolean(ENABLED));
        assertEquals("result", params.getString(NAME));
    }

    @Test
    public void testUnboundSlotUsesDefault() {
        Parameter unbound = new Parameter("unbound", Parameter.Type.INT, 42, "Unbound");
        ProcessParams params = ProcessParams.fromProperties(new Properties(), binding(), null);
        assertEquals(42, params.getInt(unbound));
    }

    @Test
    public void testBoundButMissingFailsLoudly() {
        ProcessParams params = ProcessParams.fromProperties(new Properties(), binding(), null);
        assertThrows(IllegalStateException.class, () -> params.getInt(SERIES));
    }

    @Test
    public void testEmptyStringIsAValue() {
        Properties props = new Properties();
        props.setProperty("Output Name", "");
        ProcessParams params = ProcessParams.fromProperties(props, binding(), null);
        assertEquals("", params.getString(NAME));
    }

    @Test
    public void testHas() {
        ProcessParams params = ProcessParams.fromProperties(new Properties(), binding(), null);
        assertTrue(params.has(SERIES));
        assertFalse(params.has(new Parameter("other", Parameter.Type.INT, 0, "Other")));
    }

    @Test
    public void testAliasResolution() {
        Properties props = new Properties();
        props.setProperty("Old Series Label", "5");
        Map<String, String> aliasTable = Map.of("Old Series Label", "Preview Series");
        ProcessParams params = ProcessParams.fromProperties(props, binding(), aliasTable);
        assertEquals(5, params.getInt(SERIES));
    }

    @Test
    public void testForChannel() {
        Parameter spotRadius = new Parameter("spot_radius", Parameter.Type.DOUBLE, 1.0, "Spot radius");
        assertEquals("spot_radius_channel_3", spotRadius.forChannel(3).name());
    }
}
