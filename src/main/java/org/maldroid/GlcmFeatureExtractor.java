package org.maldroid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Extracts GLCM (Gray-Level Co-occurrence Matrix) texture features from APK binary data.
 * Replicates the Python scikit-image graycomatrix/graycoprops logic:
 *   distance=5, angle=0° (horizontal), levels=256, symmetric=true, normed=true
 */
public class GlcmFeatureExtractor {

    private static final int WIDTH = 2048;
    private static final int LEVELS = 256;
    private static final int DISTANCE = 5;

    /**
     * @return double[]{contrast, dissimilarity, homogeneity, energy, correlation, asm}
     *         or null on failure
     */
    public static double[] extract(String apkPath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(apkPath));
        int height = bytes.length / WIDTH;
        if (height < 1) return new double[6];

        // Build symmetric GLCM
        long[][] glcm = new long[LEVELS][LEVELS];
        long total = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < WIDTH - DISTANCE; c++) {
                int i = bytes[r * WIDTH + c] & 0xFF;
                int j = bytes[r * WIDTH + c + DISTANCE] & 0xFF;
                glcm[i][j]++;
                glcm[j][i]++;
                total += 2;
            }
        }

        if (total == 0) return new double[6];

        // Normalize
        double[][] p = new double[LEVELS][LEVELS];
        for (int i = 0; i < LEVELS; i++)
            for (int j = 0; j < LEVELS; j++)
                p[i][j] = (double) glcm[i][j] / total;

        // Compute marginal means and std for correlation
        double[] px = new double[LEVELS];
        double[] py = new double[LEVELS];
        for (int i = 0; i < LEVELS; i++)
            for (int j = 0; j < LEVELS; j++) {
                px[i] += p[i][j];
                py[j] += p[i][j];
            }

        double muX = 0, muY = 0;
        for (int i = 0; i < LEVELS; i++) {
            muX += i * px[i];
            muY += i * py[i];
        }
        double sigX = 0, sigY = 0;
        for (int i = 0; i < LEVELS; i++) {
            sigX += (i - muX) * (i - muX) * px[i];
            sigY += (i - muY) * (i - muY) * py[i];
        }
        sigX = Math.sqrt(sigX);
        sigY = Math.sqrt(sigY);

        // Compute features
        double contrast = 0, dissimilarity = 0, homogeneity = 0, asm = 0, correlation = 0;
        for (int i = 0; i < LEVELS; i++) {
            for (int j = 0; j < LEVELS; j++) {
                double pij = p[i][j];
                if (pij == 0) continue;
                int diff = Math.abs(i - j);
                contrast += (double) diff * diff * pij;
                dissimilarity += diff * pij;
                homogeneity += pij / (1.0 + diff);
                asm += pij * pij;
                if (sigX > 0 && sigY > 0)
                    correlation += (i - muX) * (j - muY) * pij / (sigX * sigY);
            }
        }
        double energy = Math.sqrt(asm);

        return new double[]{contrast, dissimilarity, homogeneity, energy, correlation, asm};
    }
}
