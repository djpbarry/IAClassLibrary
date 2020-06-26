/*
 * Copyright (C) 2014 David Barry <david.barry at cancer.org.uk>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package Math.Optimisation;

import Particle.IsoGaussian;
import java.util.ArrayList;
import java.util.Arrays;

public class FloatingMultiGaussFitter extends MultiGaussFitter {

    double sigmaStepSize;
    double SIGMA_STEP_SIZE = 0.01;
    double sigma[][];

    public FloatingMultiGaussFitter(int N_MAX, int FIT_RADIUS, int FIT_SIZE) {
        super(N_MAX, FIT_RADIUS, FIT_SIZE);
    }

    public void fit(double[][] A, double sigEst) {
        this.sigEst = sigEst;
        xyStepSize = XY_STEP_SIZE / STEP_TOL;
        magStepSize = MAG_STEP_SIZE / STEP_TOL;
        bgStepSize = BG_STEP_SIZE / STEP_TOL;
        sigmaStepSize = SIGMA_STEP_SIZE / STEP_TOL;
        xe = new double[N_MAX][N_MAX];
        ye = new double[N_MAX][N_MAX];
        mag = new double[N_MAX][N_MAX];
        bg = new double[N_MAX][N_MAX];
        sigma = new double[N_MAX][N_MAX];
        r = new double[N_MAX];
        Arrays.fill(r, -Double.MAX_VALUE);
        initialiseFitting(A, FIT_RADIUS, xe, ye, mag, bg, sigma, r);
    }

    void initialiseFitting(double[][] image, int index, double[][] xe, double[][] ye, double[][] mag, double[][] bg, double[][] sigma, double[] r) {
        centreOfMass(xe, ye, bg, index, image);
        mag[0][0] = image[FIT_RADIUS + 1][FIT_RADIUS + 1];
        sigma[0][0] = sigEst;
        doMultiFit(image, index, 0, xe, ye, mag, bg, sigma, r);
        for (int n = 1; n < N_MAX; n++) {
            mag[n][n] = 0.0;
            bg[n][n] = 0.0;
            xe[n][n] = 0.0;
            ye[n][n] = 0.0;
            sigma[n][n] = 0.0;
            for (int j = 0; j < FIT_SIZE; j++) {
                for (int i = index - FIT_RADIUS; i < index - FIT_RADIUS + FIT_SIZE; i++) {
                    double residual = image[i][j];
                    for (int m = 0; m < n; m++) {
                        xe[n][m] = xe[n - 1][m];
                        ye[n][m] = ye[n - 1][m];
                        mag[n][m] = mag[n - 1][m];
                        bg[n][m] = bg[n - 1][m];
                        sigma[n][m] = sigma[n - 1][m];
                        residual -= multiEvaluate(xe[n][m], ye[n][m], mag[n][m], bg[n][m], i, j, sigma[n][m]);
                    }
                    if (residual > mag[n][n]) {
                        mag[n][n] = residual;
                        bg[n][n] = 0.0f;
                        xe[n][n] = i;
                        ye[n][n] = j;
                        sigma[n][n] = sigma[n][n - 1];
                    }
                }
            }
            doMultiFit(image, index, n, xe, ye, mag, bg, sigma, r);
        }
        getBestModel();
    }

    void doMultiFit(double[][] M, int x0, int N, double xe[][], double[][] ye, double[][] mag, double[][] bg, double[][] sigma, double[] r) {
        for (int i = 0; i < ITERATIONS; i++) {
            for (int j = 0; j <= N; j++) {
                float r1 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, -XY_STEP_SIZE, 0.0f, 0.0f, 0.0f, 0.0f, j, N);
                float r2 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, XY_STEP_SIZE, 0.0f, 0.0f, 0.0f, 0.0f, j, N);
                float r3 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, 0.0f, -XY_STEP_SIZE, 0.0f, 0.0f, 0.0f, j, N);
                float r4 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, 0.0f, XY_STEP_SIZE, 0.0f, 0.0f, 0.0f, j, N);
                float r5 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, 0.0f, 0.0f, -MAG_STEP_SIZE, 0.0f, 0.0f, j, N);
                float r6 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, 0.0f, 0.0f, MAG_STEP_SIZE, 0.0f, 0.0f, j, N);
                float r7 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, 0.0f, 0.0f, 0.0f, -BG_STEP_SIZE, 0.0f, j, N);
                float r8 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, 0.0f, 0.0f, 0.0f, BG_STEP_SIZE, 0.0f, j, N);
                float r9 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, 0.0f, 0.0f, 0.0f, 0.0f, -SIGMA_STEP_SIZE, j, N);
                float r10 = sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, 0.0f, 0.0f, 0.0f, 0.0f, SIGMA_STEP_SIZE, j, N);
                xe[N][j] -= (r2 - r1) * xyStepSize;
                ye[N][j] -= (r4 - r3) * xyStepSize;
                mag[N][j] -= (r6 - r5) * magStepSize;
                bg[N][j] -= (r8 - r7) * bgStepSize;
                sigma[N][j] -= (r10 - r9) * sigmaStepSize;
//                if (mag[N][j] < 0.0f) {
//                    mag[N][j] = 0.0f;
//                }
//                if (bg[N][j] < 0.0f) {
//                    bg[N][j] = 0.0f;
//                }
                if (bg[N][j] > mag[N][j]) {
                    bg[N][j] = mag[N][j];
                }
            }
        }
        r[N] = getRSquared(x0, sumMultiResiduals(x0, xe, ye, mag, bg, sigma, M, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, N), M);
    }

    float sumMultiResiduals(int x0, double[][] xe, double[][] ye, double[][] mag, double[][] bg, double[][] sigma, double[][] M, double xinc, double yinc, double minc, double bginc, double sinc, int index, int N) {
        float residuals = 0.0f;
        for (int j = 0; j < FIT_SIZE; j++) {
            for (int i = x0 - FIT_RADIUS; i <= x0 + FIT_RADIUS; i++) {
                float res = 0.0f;
                int k;
                for (k = 0; k < index; k++) {
                    res += multiEvaluate(xe[N][k], ye[N][k], mag[N][k], bg[N][k], i, j, sigma[N][k]);
                }
                res += multiEvaluate(xe[N][k] + xinc, ye[N][k] + yinc, mag[N][k] + minc, bg[N][k] + bginc, i, j, sigma[N][k] + sinc);
                for (k = index + 1; k <= N; k++) {
                    res += multiEvaluate(xe[N][k], ye[N][k], mag[N][k], bg[N][k], i, j, sigma[N][k]);
                }
                double e = res - M[i][j];
                residuals += e * e;
            }
        }
        return residuals;
    }

    double multiEvaluate(double x0, double y0, double mag, double bg, int x, int y, double sigma) {
        return mag * Math.exp(-(((x - x0) * (x - x0) + (y - y0) * (y - y0)) / (sigma * sigma))) + bg;
    }

    public ArrayList<IsoGaussian> getFits(double spatialRes, double xoffset, double yoffset, double magThresh, double fitThresh) {
        getBestModel();
        if (best < 0) {
            return null;
        }
        for (int i = 0; i <= best; i++) {
            if (!(xe[best][i] > 0.0 && ye[best][i] > 0.0 && xe[best][i] < FIT_SIZE - 1.0
                    && ye[best][i] < FIT_SIZE - 1.0)) {
                r[best] = -Double.MAX_VALUE;
                return getFits(spatialRes, xoffset, yoffset, magThresh, fitThresh);
            }
        }
        ArrayList<IsoGaussian> fits = new ArrayList<IsoGaussian>();
        for (int i = 0; i <= best; i++) {
            if (mag[best][i] > magThresh && r[i] > fitThresh) {
                fits.add(new IsoGaussian((xe[best][i] + xoffset) * spatialRes,
                        (ye[best][i] + yoffset) * spatialRes, mag[best][i],
                        sigma[best][i], sigma[best][i], r[best]));
            }
        }
        return fits;
    }
}
