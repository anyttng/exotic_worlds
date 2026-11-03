package com.exoticworlds.compat.terraforged;

import net.minecraft.core.Direction;

// A cell-wide value comes from the folded cell index, never the point that found it, or two copies part.
public final class CellCenters {
    private static final float CENTER_CORRECTION = 0.35F;

    private static final int NEIGHBORS = 8;

    public enum Center {
        JITTERED {
            @Override
            float x(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return jitteredX(noise, frame, seed, cellX, cellY, jitter);
            }

            @Override
            float z(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return jitteredZ(noise, frame, seed, cellX, cellY, jitter);
            }
        },
        CORRECTED {
            @Override
            float x(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return correctedX(noise, frame, seed, cellX, cellY, jitter);
            }

            @Override
            float z(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return correctedZ(noise, frame, seed, cellX, cellY, jitter);
            }
        };

        abstract float x(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter);

        abstract float z(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter);
    }

    public static float blockX(TerraForgedNoise noise, RtfLap.Frame frame, Center center, int seed, int cellX,
            int cellY, float jitter, float frequency) {
        float x = center.x(noise, frame, seed, cellX, cellY, jitter)
                / frame.snappedFrequency(Direction.Axis.X, frequency);
        return frame.sheared()
                ? frame.outerX(x, blockZ(noise, frame, center, seed, cellX, cellY, jitter, frequency))
                : x;
    }

    public static float blockZ(TerraForgedNoise noise, RtfLap.Frame frame, Center center, int seed, int cellX,
            int cellY, float jitter, float frequency) {
        return center.z(noise, frame, seed, cellX, cellY, jitter) / frame.snappedFrequency(Direction.Axis.Z, frequency);
    }

    public static float jitteredX(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY,
            float jitter) {
        int canonicalX = frame.foldX(cellX, cellY);
        return canonicalX + noise.jitterX(seed, canonicalX, frame.foldZ(cellY)) * jitter;
    }

    public static float jitteredZ(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY,
            float jitter) {
        int canonicalY = frame.foldZ(cellY);
        return canonicalY + noise.jitterZ(seed, frame.foldX(cellX, cellY), canonicalY) * jitter;
    }

    public static float correctedX(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY,
            float jitter) {
        return corrected(noise, frame, seed, cellX, cellY, jitter, true);
    }

    public static float correctedZ(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY,
            float jitter) {
        return corrected(noise, frame, seed, cellX, cellY, jitter, false);
    }

    private static float corrected(TerraForgedNoise noise, RtfLap.Frame frame, int seed, int cellX, int cellY,
            float jitter, boolean x) {
        int canonicalX = frame.foldX(cellX, cellY);
        int canonicalY = frame.foldZ(cellY);
        float point = x
                ? canonicalX + noise.jitterX(seed, canonicalX, canonicalY) * jitter
                : canonicalY + noise.jitterZ(seed, canonicalX, canonicalY) * jitter;
        float sum = 0.0F;
        for (int cy = canonicalY - 1; cy <= canonicalY + 1; cy++) {
            for (int cx = canonicalX - 1; cx <= canonicalX + 1; cx++) {
                if (cx != canonicalX || cy != canonicalY) {
                    sum += x ? cx + noise.jitterX(seed, cx, cy) * jitter : cy + noise.jitterZ(seed, cx, cy) * jitter;
                }
            }
        }

        return noise.lerp(point, sum / NEIGHBORS, CENTER_CORRECTION);
    }

    private CellCenters() {
    }
}
