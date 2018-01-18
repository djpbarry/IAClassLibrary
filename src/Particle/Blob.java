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
package Particle;

import ij.process.ImageProcessor;

/**
 *
 * @author Dave Barry <david.barry at crick.ac.uk>
 */
public class Blob extends Particle {

    private final ImageProcessor image;
    private final int blobRadius;
    private final double res;

    public Blob(int t, double x, double y, double magnitude, ImageProcessor image, int blobRadius, double res) {
        super(t, x, y, magnitude);
        this.image = image;
        this.blobRadius = blobRadius;
        this.res=res;
        refineCentroid();
    }

    private void refineCentroid() {
        int x0 = (int) Math.round(this.x / res) - blobRadius;
        int y0 = (int) Math.round(this.y  /res) - blobRadius;
        int width = blobRadius * 2;
        double sumX = 0.0, sumY = 0.0, sum = 0.0;
        for (int y = y0; y <= y0 + width; y++) {
            for (int x = x0; x <= x0 + width; x++) {
                double p = image.getPixelValue(x, y);
                sumX += p * x;
                sumY += p * y;
                sum += p;
            }
        }
        this.x = sumX*res / sum;
        this.y = sumY*res / sum;
    }

}
