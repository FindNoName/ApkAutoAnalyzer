package org.maldroid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts DEX-only grayscale / GLCM features from APK binary data.
 * Features are aggregated across all classes*.dex files and across:
 * distances = {1, 3, 5}
 * angles    = {0°, 45°, 90°, 135°}
 */
public class GlcmFeatureExtractor {

    private static final int WIDTH = 512;
    private static final int LEVELS = 256;
    private static final int[] DISTANCES = {1, 3, 5};
    private static final int[][] OFFSETS = {
            {0, 1},   // 0°
            {1, 1},   // 45°
            {1, 0},   // 90°
            {1, -1}   // 135°
    };

    public static ImageFeatureResult extract(String apkPath) throws IOException {
        List<byte[]> dexFiles = DexExtractor.extractDexFiles(apkPath);
        ImageFeatureResult result = new ImageFeatureResult();
        result.dexCount = dexFiles.size();

        if (dexFiles.isEmpty()) {
            return result;
        }

        FeatureAccumulator contrast = new FeatureAccumulator();
        FeatureAccumulator dissimilarity = new FeatureAccumulator();
        FeatureAccumulator homogeneity = new FeatureAccumulator();
        FeatureAccumulator energy = new FeatureAccumulator();
        FeatureAccumulator correlation = new FeatureAccumulator();
        FeatureAccumulator asm = new FeatureAccumulator();
        FeatureAccumulator glcmEntropy = new FeatureAccumulator();
        FeatureAccumulator byteEntropy = new FeatureAccumulator();
        FeatureAccumulator grayMean = new FeatureAccumulator();
        FeatureAccumulator grayStd = new FeatureAccumulator();
        FeatureAccumulator edgeDensity = new FeatureAccumulator();

        for (byte[] dex : dexFiles) {
            result.dexTotalSize += dex.length;

            byteEntropy.add(computeByteEntropy(dex));
            GrayStats grayStats = computeGrayStats(dex);
            grayMean.add(grayStats.mean);
            grayStd.add(grayStats.std);
            edgeDensity.add(computeEdgeDensity(dex));

            int height = (dex.length + WIDTH - 1) / WIDTH;
            if (height < 1) {
                continue;
            }

            for (int distance : DISTANCES) {
                for (int[] offset : OFFSETS) {
                    GlcmMetrics metrics = computeGlcmMetrics(dex, height, distance, offset[0], offset[1]);
                    if (metrics == null) {
                        continue;
                    }
                    contrast.add(metrics.contrast);
                    dissimilarity.add(metrics.dissimilarity);
                    homogeneity.add(metrics.homogeneity);
                    energy.add(metrics.energy);
                    correlation.add(metrics.correlation);
                    asm.add(metrics.asm);
                    glcmEntropy.add(metrics.entropy);
                }
            }
        }

        result.glcmContrastMean = contrast.mean();
        result.glcmContrastStd = contrast.std();
        result.glcmDissimilarityMean = dissimilarity.mean();
        result.glcmDissimilarityStd = dissimilarity.std();
        result.glcmHomogeneityMean = homogeneity.mean();
        result.glcmHomogeneityStd = homogeneity.std();
        result.glcmEnergyMean = energy.mean();
        result.glcmEnergyStd = energy.std();
        result.glcmCorrelationMean = correlation.mean();
        result.glcmCorrelationStd = correlation.std();
        result.glcmAsmMean = asm.mean();
        result.glcmAsmStd = asm.std();
        result.glcmEntropyMean = glcmEntropy.mean();
        result.glcmEntropyStd = glcmEntropy.std();
        result.byteEntropyMean = byteEntropy.mean();
        result.byteEntropyStd = byteEntropy.std();
        result.grayMean = grayMean.mean();
        result.grayStd = grayStd.mean();
        result.edgeDensityMean = edgeDensity.mean();
        result.edgeDensityStd = edgeDensity.std();

        return result;
    }

    private static GlcmMetrics computeGlcmMetrics(byte[] bytes, int height, int distance, int rowStep, int colStep) {
        long[][] glcm = new long[LEVELS][LEVELS];
        long total = 0;

        int dr = rowStep * distance;
        int dc = colStep * distance;

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < WIDTH; c++) {
                int r2 = r + dr;
                int c2 = c + dc;
                if (r2 < 0 || r2 >= height || c2 < 0 || c2 >= WIDTH) {
                    continue;
                }

                int idx1 = r * WIDTH + c;
                int idx2 = r2 * WIDTH + c2;
                if (idx1 >= bytes.length || idx2 >= bytes.length) {
                    continue;
                }

                int i = bytes[idx1] & 0xFF;
                int j = bytes[idx2] & 0xFF;
                glcm[i][j]++;
                glcm[j][i]++;
                total += 2;
            }
        }

        if (total == 0) {
            return null;
        }

        double[][] p = new double[LEVELS][LEVELS];
        for (int i = 0; i < LEVELS; i++) {
            for (int j = 0; j < LEVELS; j++) {
                p[i][j] = (double) glcm[i][j] / total;
            }
        }

        double[] px = new double[LEVELS];
        double[] py = new double[LEVELS];
        for (int i = 0; i < LEVELS; i++) {
            for (int j = 0; j < LEVELS; j++) {
                px[i] += p[i][j];
                py[j] += p[i][j];
            }
        }

        double muX = 0;
        double muY = 0;
        for (int i = 0; i < LEVELS; i++) {
            muX += i * px[i];
            muY += i * py[i];
        }

        double sigX = 0;
        double sigY = 0;
        for (int i = 0; i < LEVELS; i++) {
            sigX += (i - muX) * (i - muX) * px[i];
            sigY += (i - muY) * (i - muY) * py[i];
        }
        sigX = Math.sqrt(sigX);
        sigY = Math.sqrt(sigY);

        GlcmMetrics metrics = new GlcmMetrics();
        for (int i = 0; i < LEVELS; i++) {
            for (int j = 0; j < LEVELS; j++) {
                double pij = p[i][j];
                if (pij == 0) {
                    continue;
                }
                int diff = Math.abs(i - j);
                metrics.contrast += (double) diff * diff * pij;
                metrics.dissimilarity += diff * pij;
                metrics.homogeneity += pij / (1.0 + diff);
                metrics.asm += pij * pij;
                metrics.entropy -= pij * (Math.log(pij) / Math.log(2));
                if (sigX > 0 && sigY > 0) {
                    metrics.correlation += (i - muX) * (j - muY) * pij / (sigX * sigY);
                }
            }
        }
        metrics.energy = Math.sqrt(metrics.asm);
        return metrics;
    }

    private static double computeByteEntropy(byte[] bytes) {
        if (bytes.length == 0) {
            return 0.0;
        }

        int[] histogram = new int[LEVELS];
        for (byte b : bytes) {
            histogram[b & 0xFF]++;
        }

        double entropy = 0.0;
        for (int count : histogram) {
            if (count == 0) {
                continue;
            }
            double p = (double) count / bytes.length;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    private static GrayStats computeGrayStats(byte[] bytes) {
        GrayStats stats = new GrayStats();
        if (bytes.length == 0) {
            return stats;
        }

        double sum = 0.0;
        for (byte b : bytes) {
            sum += b & 0xFF;
        }
        stats.mean = sum / bytes.length;

        double variance = 0.0;
        for (byte b : bytes) {
            double v = (b & 0xFF) - stats.mean;
            variance += v * v;
        }
        stats.std = Math.sqrt(variance / bytes.length);
        return stats;
    }

    private static double computeEdgeDensity(byte[] bytes) {
        int height = (bytes.length + WIDTH - 1) / WIDTH;
        if (height < 1) {
            return 0.0;
        }

        long comparisons = 0;
        long strongEdges = 0;
        final int threshold = 32;

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < WIDTH; c++) {
                int idx = r * WIDTH + c;
                if (idx >= bytes.length) {
                    continue;
                }
                int value = bytes[idx] & 0xFF;

                if (c + 1 < WIDTH) {
                    int rightIdx = idx + 1;
                    if (rightIdx < bytes.length) {
                        comparisons++;
                        if (Math.abs(value - (bytes[rightIdx] & 0xFF)) >= threshold) {
                            strongEdges++;
                        }
                    }
                }

                int downIdx = idx + WIDTH;
                if (r + 1 < height && downIdx < bytes.length) {
                    comparisons++;
                    if (Math.abs(value - (bytes[downIdx] & 0xFF)) >= threshold) {
                        strongEdges++;
                    }
                }
            }
        }

        return comparisons > 0 ? (double) strongEdges / comparisons : 0.0;
    }

    private static final class GlcmMetrics {
        private double contrast;
        private double dissimilarity;
        private double homogeneity;
        private double energy;
        private double correlation;
        private double asm;
        private double entropy;
    }

    private static final class GrayStats {
        private double mean;
        private double std;
    }

    private static final class FeatureAccumulator {
        private final List<Double> values = new ArrayList<>();

        private void add(double value) {
            values.add(value);
        }

        private double mean() {
            if (values.isEmpty()) {
                return 0.0;
            }
            double sum = 0.0;
            for (double value : values) {
                sum += value;
            }
            return sum / values.size();
        }

        private double std() {
            if (values.isEmpty()) {
                return 0.0;
            }
            double mean = mean();
            double variance = 0.0;
            for (double value : values) {
                double diff = value - mean;
                variance += diff * diff;
            }
            return Math.sqrt(variance / values.size());
        }
    }
}
