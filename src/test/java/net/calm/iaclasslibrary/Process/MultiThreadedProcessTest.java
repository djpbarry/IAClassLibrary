package net.calm.iaclasslibrary.Process;

import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiThreadedProcessTest {

    @Test
    public void testConstructOutputName() {
        assertEquals("foo_baz", MultiThreadedProcess.constructOutputName("foo_bar", "baz"));
    }

    @Test
    public void testGetOutputReturnsDuplicate() {
        TestProcess p = new TestProcess(null);
        p.output = new ImagePlus("original", new ByteProcessor(4, 4));
        ImagePlus out = p.getOutput();
        assertNotSame(p.output, out);
        assertEquals("original", out.getTitle());
    }

    @Test
    public void testOutputDestsWiring() {
        TestProcess input = new TestProcess(null);
        TestProcess output = new TestProcess(new TestProcess[]{input});
        assertTrue(input.outputDests.contains(output));
    }

    @Test
    public void testRunWorkersExecutesAllTasks() {
        TestProcess p = new TestProcess(null);
        final int[] counts = new int[8];
        p.exec = Executors.newFixedThreadPool(4);
        Runnable[] workers = new Runnable[8];
        for (int i = 0; i < workers.length; i++) {
            final int idx = i;
            workers[i] = () -> counts[idx] = 1;
        }
        p.runWorkers(workers, "test");
        for (int count : counts) {
            assertEquals(1, count);
        }
    }

    @Test
    public void testDimensionsDefaultTimeToZero() {
        TestProcess p = new TestProcess(null);
        assertEquals(0, p.series);
        assertEquals(0, p.channel);
        assertEquals(0, p.time);
    }

    @Test
    public void testDimensionAccessors() {
        TestProcess p = new TestProcess(null);
        p.setSeries(2);
        p.setChannel(3);
        p.setTime(4);
        assertEquals(2, p.series);
        assertEquals(3, p.channel);
        assertEquals(4, p.getTime());
    }

    @Test
    public void testTypedGettersResolveThroughParams() {
        TestProcess p = new TestProcess(null);
        Parameter series = new Parameter("series", Parameter.Type.INT, 0, "Series");
        Parameter radius = new Parameter("radius", Parameter.Type.DOUBLE, 1.0, "Radius");
        Parameter enabled = new Parameter("enabled", Parameter.Type.BOOLEAN, false, "Enabled");
        ParamBinding binding = new ParamBinding()
                .bind(series, "series")
                .bind(radius, "radius")
                .bind(enabled, "enabled");
        Properties props = new Properties();
        props.setProperty("series", "3");
        props.setProperty("radius", "2.5");
        props.setProperty("enabled", "true");
        p.initParams(null, ProcessParams.fromProperties(props, binding, null));
        assertEquals(3, p.getInt(series));
        assertEquals(2.5, p.getDouble(radius), 1e-9);
        assertTrue(p.getBoolean(enabled));
    }

    @Test
    public void testRebuildProducesFreshPipeline() {
        TestProcess source = new TestProcess(null);
        TestProcess transform = new TestProcess(new TestProcess[]{source});
        ProcessPipeline pipeline = new ProcessPipeline();
        pipeline.addProcess(source);
        pipeline.addProcess(transform);
        ProcessPipeline rebuilt = pipeline.rebuild();
        assertEquals(2, rebuilt.getSize());
        assertNotSame(source, rebuilt.getProcess(0));
        assertNotSame(transform, rebuilt.getProcess(1));
        assertSame(rebuilt.getProcess(0), rebuilt.getProcess(1).inputs[0]);
    }

    @Test
    public void testTransformProcessRejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> new TestTransform(null));
    }

    private static class TestProcess extends MultiThreadedProcess {
        TestProcess(MultiThreadedProcess[] inputs) {
            super(inputs);
        }

        @Override
        public void setup(BioFormatsImg img, ProcessParams params) {
        }

        @Override
        public void run() {
            output = new ImagePlus("title", new ByteProcessor(4, 4));
        }

        @Override
        protected MultiThreadedProcess copy() {
            return new TestProcess(inputs);
        }
    }

    private static class TestTransform extends TransformProcess {
        TestTransform(MultiThreadedProcess[] inputs) {
            super(inputs);
        }

        @Override
        public void setup(BioFormatsImg img, ProcessParams params) {
        }

        @Override
        public void run() {
        }

        @Override
        protected MultiThreadedProcess copy() {
            return new TestTransform(inputs);
        }
    }
}
