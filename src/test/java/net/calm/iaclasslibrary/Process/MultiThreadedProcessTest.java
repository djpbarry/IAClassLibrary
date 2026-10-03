package net.calm.iaclasslibrary.Process;

import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

    private static class TestProcess extends MultiThreadedProcess {
        TestProcess(MultiThreadedProcess[] inputs) {
            super(inputs);
        }

        @Override
        public void setup(BioFormatsImg img, Properties props, String[] propLabels) {
        }

        @Override
        public void run() {
            output = new ImagePlus("title", new ByteProcessor(4, 4));
        }

        @Override
        public MultiThreadedProcess duplicate() {
            TestProcess p = new TestProcess(inputs);
            updateOutputDests(p);
            return p;
        }
    }
}
