package com.exoticworlds.compat.reterraforged;

import net.minecraft.core.Direction;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;

// A value that holds for a whole lattice cell is computed from the cell's folded index, never from the point that
// happened to find the cell: two copies one lap apart then hand ReTerraForged the same number, bit for bit.
public final class CellCenters {
    private static final float CENTER_CORRECTION = 0.35F;

    private static final int NEIGHBORS = 8;

    private static final float VERTEX_DETERMINANT_FLOOR = 1.0E-5F;

    private static final float VERTEX_SLACK = 1.0E-4F;

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
        },
        CENTROID {
            @Override
            float x(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return centroidX(frame, seed, cellX, cellY, jitter);
            }

            @Override
            float z(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
                return centroidZ(frame, seed, cellX, cellY, jitter);
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

    // The centroid of the Voronoi vertices around the cell, as UPLIFT's smooth gradient finds it.
    public static float centroidX(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        return centroid(frame, seed, cellX, cellY, jitter, true);
    }

    public static float centroidZ(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter) {
        return centroid(frame, seed, cellX, cellY, jitter, false);
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

    private static float centroid(RtfLap.Frame frame, int seed, int cellX, int cellY, float jitter, boolean x) {
        int canonicalX = frame.foldX(cellX, cellY);
        int canonicalY = frame.foldZ(cellY);
        NoiseUtil.Vec2f own = NoiseUtil.cell(seed, canonicalX, canonicalY);
        float pointX = canonicalX + own.x() * jitter;
        float pointY = canonicalY + own.y() * jitter;
        float[] neighborX = new float[NEIGHBORS];
        float[] neighborY = new float[NEIGHBORS];
        int index = 0;
        for (int cy = canonicalY - 1; cy <= canonicalY + 1; cy++) {
            for (int cx = canonicalX - 1; cx <= canonicalX + 1; cx++) {
                if (cx != canonicalX || cy != canonicalY) {
                    NoiseUtil.Vec2f neighbor = NoiseUtil.cell(seed, cx, cy);
                    neighborX[index] = cx + neighbor.x() * jitter;
                    neighborY[index] = cy + neighbor.y() * jitter;
                    index++;
                }
            }
        }

        float sumX = 0.0F;
        float sumY = 0.0F;
        int vertices = 0;
        float ownSquare = pointX * pointX + pointY * pointY;
        for (int i = 0; i < NEIGHBORS; i++) {
            float dx1 = neighborX[i] - pointX;
            float dy1 = neighborY[i] - pointY;
            float b1 = 0.5F * (neighborX[i] * neighborX[i] + neighborY[i] * neighborY[i] - ownSquare);
            for (int j = i + 1; j < NEIGHBORS; j++) {
                float dx2 = neighborX[j] - pointX;
                float dy2 = neighborY[j] - pointY;
                float b2 = 0.5F * (neighborX[j] * neighborX[j] + neighborY[j] * neighborY[j] - ownSquare);
                float determinant = dx1 * dy2 - dy1 * dx2;
                if (Math.abs(determinant) < VERTEX_DETERMINANT_FLOOR) {
                    continue;
                }

                float vx = (b1 * dy2 - b2 * dy1) / determinant;
                float vy = (dx1 * b2 - dx2 * b1) / determinant;
                float ownDistance = (vx - pointX) * (vx - pointX) + (vy - pointY) * (vy - pointY);
                if (isVertex(neighborX, neighborY, i, j, vx, vy, ownDistance)) {
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

    private static boolean isVertex(float[] neighborX, float[] neighborY, int i, int j, float vx, float vy,
            float ownDistance) {
        for (int k = 0; k < NEIGHBORS; k++) {
            if (k == i || k == j) {
                continue;
            }

            float distance = (vx - neighborX[k]) * (vx - neighborX[k]) + (vy - neighborY[k]) * (vy - neighborY[k]);
            if (distance < ownDistance - VERTEX_SLACK) {
                return false;
            }
        }

        return true;
    }

    private CellCenters() {
    }
}
