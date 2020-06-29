/*
 * Copyright (C) 2017 Dave Barry <david.barry at crick.ac.uk>
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
package net.calm.iaclasslibrary.ParticleWriter;

import net.calm.iaclasslibrary.IAClasses.Utils;
import net.calm.iaclasslibrary.Particle.Blob;
import net.calm.iaclasslibrary.Particle.IsoGaussian;
import net.calm.iaclasslibrary.Particle.Particle;
import ij.gui.Overlay;
import ij.gui.TextRoi;
import ij.process.ImageProcessor;
import java.util.ArrayList;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class ParticleWriter {

    public static void drawParticle(ImageProcessor image, Particle p, boolean preview, double blobSize, double spatialRes, int label, Overlay overlay) {
        int radius = (int) Math.round(blobSize / spatialRes);
        int x = (int) Math.round(p.getX() / spatialRes);
        int y = (int) Math.round(p.getY() / spatialRes);
        if (p instanceof Blob) {
            image.drawOval((x - radius), (y - radius), 2 * radius, 2 * radius);
        } else if (p instanceof IsoGaussian) {
            if (preview) {
                radius = (int) Math.round(2.0 * ((IsoGaussian) p).getXSigma());
                image.drawOval((x - radius), (y - radius), 2 * radius, 2 * radius);
            } else {
                Utils.draw2DGaussian(image, (IsoGaussian) p, 0.0, spatialRes, false);
            }
        } else {
            image.drawLine(x, y - radius, x, y + radius);
            image.drawLine(x + radius, y, x - radius, y);
        }
        if (label >= 0 && overlay != null) {
            overlay.add(new TextRoi(x, y, String.format("%d", label)));
        }
    }

    public static void drawDetections(ArrayList<Particle> detections, ImageProcessor output, boolean preview, double blobSize, double spatialRes, boolean label, Overlay overlay) {
        for (int i = 0; i < detections.size(); i++) {
            drawParticle(output, detections.get(i), preview, blobSize, spatialRes, label ? i + 1 : -1, overlay);
        }
    }

}
