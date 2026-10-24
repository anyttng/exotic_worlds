package com.exoticworlds.compat.ultraterraforged;

import com.exoticworlds.compat.terraforged.RtfLap;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;

import net.minecraft.core.Direction;

// A cell-wide value comes from the folded cell index, never the point that found it, or two copies part.
public final class CellCenters {
    private static final float CENTER_CORRECTION = 0.35F;

    private static final int NEIGHBORS = 8;

    public enum Center {
        JITTERED {
            @Override
            float x(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return jitteredX(frame, seed, cellX, cellY, jitter);
            }

            @Override
            float z(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return jitteredZ(frame, seed, cellX, cellY, jitter);
            }
        },
        CORRECTED {
            @Override
            float x(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return correctedX(frame, seed, cellX, cellY, jitter);
            }

            @Override
            float z(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return correctedZ(frame, seed, cellX, cellY, jitter);
            }
        };

        abstract float x(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter);

        abstract float z(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter);
    }

    public static float blockX(RtfLap.Frame frame, Center center, int seed, int cellX, int cellY, float jitter,
            float frequency) {
        float x = center.x(frame, seed, cellX, cellY, jitter) / frame.snappedFrequency(Direction.Axis.X, frequency);
        return frame.sheared() ? frame.outerX(x, blockZ(frame, center, seed, cellX, cellY, jitter, frequency)) : x;
    }

    public static float blockZ(RtfLap.Frame frame, Center center, int seed, int cellX, int cellY, float jitter,
            float frequency) {
        return center.z(frame, seed, cellX, cellY, jitter) / frame.snappedFrequency(Direction.Axis.Z, frequency);
    }

    public static float jitteredX(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        int canonicalX = frame.foldX(cellX, cellY);
        return canonicalX + NoiseUtil.cell(seed, canonicalX, frame.foldZ(cellY)).x() * jitter;
    }

    public static float jitteredZ(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        int canonicalY = frame.foldZ(cellY);
        return canonicalY + NoiseUtil.cell(seed, frame.foldX(cellX, cellY), canonicalY).y() * jitter;
    }

    public static float correctedX(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        return corrected(frame, seed, cellX, cellY, jitter, true);
    }

    public static float correctedZ(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        return corrected(frame, seed, cellX, cellY, jitter, false);
    }

    private static float corrected(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter, boolean x) {
        int canonicalX = frame.foldX(cellX, cellY);
        int canonicalY = frame.foldZ(cellY);
        NoiseUtil.Vec2f own = NoiseUtil.cell(seed, canonicalX, canonicalY);
        float point = x ? canonicalX + own.x() * jitter : canonicalY + own.y() * jitter;
        float sum = 0.0F;
        for (int cy = canonicalY - 1; cy <= canonicalY + 1; cy++) {
            for (int cx = canonicalX - 1; cx <= canonicalX + 1; cx++) {
                if (cx != canonicalX || cy != canonicalY) {
                    NoiseUtil.Vec2f neighbor = NoiseUtil.cell(seed, cx, cy);
                    sum += x ? cx + neighbor.x() * jitter : cy + neighbor.y() * jitter;
                }
            }
        }

        return NoiseUtil.lerp(point, sum / NEIGHBORS, CENTER_CORRECTION);
    }

    private CellCenters() {
    }
}
