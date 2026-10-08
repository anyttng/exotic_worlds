package com.exoticworlds.compat.reterraforged;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

// A value that holds for a whole lattice cell is computed from the cell's folded index, never from the point that
// happened to find the cell: two copies one lap apart then hand ReTerraForged the same number, bit for bit.
public final class CellCenters {
    private static final float CENTER_CORRECTION = 0.35F;

    private static final int NEIGHBOURS = 8;

    private static final float VERTEX_DETERMINANT_FLOOR = 1.0E-5F;

    private static final float VERTEX_SLACK = 1.0E-4F;

    public static float jitteredX(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        int canonicalX = frame.fold(Direction.Axis.X, cellX);
        return canonicalX + NoiseUtil.cell(seed, canonicalX, frame.fold(Direction.Axis.Z, cellY)).x() * jitter;
    }

    public static float jitteredZ(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        int canonicalY = frame.fold(Direction.Axis.Z, cellY);
        return canonicalY + NoiseUtil.cell(seed, frame.fold(Direction.Axis.X, cellX), canonicalY).y() * jitter;
    }

    public static float correctedX(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        return corrected(frame, seed, cellX, cellY, jitter, true);
    }

    public static float correctedZ(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        return corrected(frame, seed, cellX, cellY, jitter, false);
    }

    // The centroid of the Voronoi vertices around the cell, as UPLIFT's smooth gradient finds it.
    public static float centroidX(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        return centroid(frame, seed, cellX, cellY, jitter, true);
    }

    public static float centroidZ(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        return centroid(frame, seed, cellX, cellY, jitter, false);
    }

    private static float corrected(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter, boolean x) {
        int canonicalX = frame.fold(Direction.Axis.X, cellX);
        int canonicalY = frame.fold(Direction.Axis.Z, cellY);
        NoiseUtil.Vec2f own = NoiseUtil.cell(seed, canonicalX, canonicalY);
        float point = x ? canonicalX + own.x() * jitter : canonicalY + own.y() * jitter;
        float sum = 0.0F;
        for (int cy = canonicalY - 1; cy <= canonicalY + 1; cy++) {
            for (int cx = canonicalX - 1; cx <= canonicalX + 1; cx++) {
                if (cx != canonicalX || cy != canonicalY) {
                    NoiseUtil.Vec2f neighbour = NoiseUtil.cell(seed, cx, cy);
                    sum += x ? cx + neighbour.x() * jitter : cy + neighbour.y() * jitter;
                }
            }
        }

        return NoiseUtil.lerp(point, sum / NEIGHBOURS, CENTER_CORRECTION);
    }

    private static float centroid(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter, boolean x) {
        int canonicalX = frame.fold(Direction.Axis.X, cellX);
        int canonicalY = frame.fold(Direction.Axis.Z, cellY);
        NoiseUtil.Vec2f own = NoiseUtil.cell(seed, canonicalX, canonicalY);
        float pointX = canonicalX + own.x() * jitter;
        float pointY = canonicalY + own.y() * jitter;
        float[] neighbourX = new float[NEIGHBOURS];
        float[] neighbourY = new float[NEIGHBOURS];
        int index = 0;
        for (int cy = canonicalY - 1; cy <= canonicalY + 1; cy++) {
            for (int cx = canonicalX - 1; cx <= canonicalX + 1; cx++) {
                if (cx != canonicalX || cy != canonicalY) {
                    NoiseUtil.Vec2f neighbour = NoiseUtil.cell(seed, cx, cy);
                    neighbourX[index] = cx + neighbour.x() * jitter;
                    neighbourY[index] = cy + neighbour.y() * jitter;
                    index++;
                }
            }
        }

        float sumX = 0.0F;
        float sumY = 0.0F;
        int vertices = 0;
        float ownSquare = pointX * pointX + pointY * pointY;
        for (int i = 0; i < NEIGHBOURS; i++) {
            float dx1 = neighbourX[i] - pointX;
            float dy1 = neighbourY[i] - pointY;
            float b1 = 0.5F * (neighbourX[i] * neighbourX[i] + neighbourY[i] * neighbourY[i] - ownSquare);
            for (int j = i + 1; j < NEIGHBOURS; j++) {
                float dx2 = neighbourX[j] - pointX;
                float dy2 = neighbourY[j] - pointY;
                float b2 = 0.5F * (neighbourX[j] * neighbourX[j] + neighbourY[j] * neighbourY[j] - ownSquare);
                float determinant = dx1 * dy2 - dy1 * dx2;
                if (Math.abs(determinant) < VERTEX_DETERMINANT_FLOOR) {
                    continue;
                }

                float vx = (b1 * dy2 - b2 * dy1) / determinant;
                float vy = (dx1 * b2 - dx2 * b1) / determinant;
                float ownDistance = (vx - pointX) * (vx - pointX) + (vy - pointY) * (vy - pointY);
                if (isVertex(neighbourX, neighbourY, i, j, vx, vy, ownDistance)) {
                    sumX += vx;
                    sumY += vy;
                    vertices++;
                }
            }
        }

        if (vertices == 0) {
            return x ? pointX : pointY;
        }

        return x ? sumX / vertices : sumY / vertices;
    }

    private static boolean isVertex(float[] neighbourX, float[] neighbourY, int i, int j, float vx, float vy,
            float ownDistance) {
        for (int k = 0; k < NEIGHBOURS; k++) {
            if (k == i || k == j) {
                continue;
            }

            float distance = (vx - neighbourX[k]) * (vx - neighbourX[k]) + (vy - neighbourY[k]) * (vy - neighbourY[k]);
            if (distance < ownDistance - VERTEX_SLACK) {
                return false;
            }
        }

        return true;
    }

    private CellCenters() {
    }
}
