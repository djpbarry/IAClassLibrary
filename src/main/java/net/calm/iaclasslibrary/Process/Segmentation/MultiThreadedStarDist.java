package net.calm.iaclasslibrary.Process.Segmentation;

import ij.IJ;
import ij.ImagePlus;
import net.calm.iaclasslibrary.IO.BioFormats.LocationAgnosticBioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MultiThreadedStarDist extends MultiThreadedProcess {
    public static int CHANNEL_SELECT = 0;
    public static int SERIES_SELECT = 7;

    private int series;
    private int channel;

    public MultiThreadedStarDist(MultiThreadedProcess[] inputs) {
        super(inputs);
    }

    public void setup(LocationAgnosticBioFormatsImg img, Properties props, String[] propLabels) {
        this.img = img;
        this.propLabels = propLabels;
        this.props = props;
    }

    public void run() {
        series = Integer.parseInt(props.getProperty(propLabels[SERIES_SELECT]));
        channel = Integer.parseInt(props.getProperty(propLabels[CHANNEL_SELECT]));
        //calibration = getCalibration(series);
        img.loadPixelData(series, channel, channel, null);
        String tempDir = "E:/Debug/Giani/pipeline_test/";
        String tempImage = "stardist_temp.tif";
        String starDistOutput = FilenameUtils.getBaseName(tempImage) + ".stardist." + FilenameUtils.getExtension(tempImage);
        ImagePlus imp = img.getLoadedImage();
        IJ.saveAs(imp, "TIF", (new File(tempDir, tempImage).getAbsolutePath()));

        List<String> cmd = new ArrayList<>();
        cmd.add("cmd.exe");
        cmd.add("/C");
        cmd.add("cd C:/Users/davej/GitRepos/Python/stardist/venv/Scripts/");
        cmd.add("&");
        cmd.add("activate.bat");
        cmd.add("&");
        cmd.add("cd");
        cmd.add("../..");
        cmd.add("&");
        cmd.add("python");
        cmd.add("stardist/scripts/predict3d.py");
        cmd.add("-i");
        cmd.add((new File(tempDir, tempImage).getAbsolutePath()));
        cmd.add("-m");
        cmd.add("3D_demo");
        cmd.add("-o");
        cmd.add(tempDir);

        System.out.println(cmd.toString().replace(",", ""));

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);

            Process p = pb.start();

            Thread t = new Thread(Thread.currentThread().getName() + "-" + p.hashCode()) {
                @Override
                public void run() {
                    BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    try {
                        for (String line = stdIn.readLine(); line != null; ) {
                            System.out.println(line);
                            line = stdIn.readLine();// you don't want to remove or comment that line! no you don't :P
                        }
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            };
            t.setDaemon(true);
            t.start();

            p.waitFor();

            int exitValue = p.exitValue();

            if (exitValue != 0) {
                System.out.println("Exited with value " + exitValue + ". Please check output above for indications of the problem.");
            } else {
                System.out.println("Run finished");
            }
        } catch (InterruptedException | IOException e) {

        }
        output = IJ.openImage((new File(tempDir, starDistOutput).getAbsolutePath()));
    }

    public MultiThreadedStarDist duplicate() {
        MultiThreadedStarDist newProcess = new MultiThreadedStarDist(inputs);
        this.updateOutputDests(newProcess);
        return newProcess;
    }
}

