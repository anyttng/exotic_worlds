package com.exoticworlds.engine.noise.cuboid;

import java.util.List;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.engine.noise.cuboid.CuboidCrossings.Crossing;

import net.minecraft.util.Mth;

public final class CuboidWarp {
    private static final double INSIDE = 2.0;

    private static final int MOST_CROSSINGS = 64;

    private final List<CuboidFace> faces;

    private final CuboidCrossings crossings;

    private final CuboidOctaves shift;

    private final double shiftBlocks;

    private final double largestBlocks;

    private CuboidWarp(List<CuboidFace> faces, CuboidCrossings crossings, CuboidOctaves shift, double shiftBlocks,
            double largestBlocks) {
        this.faces = faces;
        this.crossings = crossings;
        this.shift = shift;
        this.shiftBlocks = shiftBlocks;
        this.largestBlocks = largestBlocks;
    }

    public static CuboidWarp of(CuboidAtlas atlas, CuboidOctaves shift, double shiftBlocks, double largestBlocks) {
        return new CuboidWarp(atlas.faces(), CuboidCrossings.of(atlas), shift, shiftBlocks, largestBlocks);
    }

    public float sample(CuboidOctaves field, int blockX, double y, int blockZ) {
        int atlasFace = this.shift.faceAt(blockX, blockZ);
        CuboidFace face = this.faces.get(atlasFace);
        return sampleFrom(field, atlasFace, blockX - face.minBlockX(), y, blockZ - face.minBlockZ());
    }

    float sampleFrom(CuboidOctaves field, int atlasFace, double localX, double y, double localZ) {
        CuboidFace face = this.faces.get(atlasFace);
        double warpX = this.shift.warpX(atlasFace, localX, localZ) * this.shiftBlocks;
        double warpZ = this.shift.warpZ(atlasFace, localX, localZ) * this.shiftBlocks;
        double held = held(face, localX, localZ, Math.hypot(warpX, warpZ));
        double startX = face.minBlockX() + localX;
        double startZ = face.minBlockZ() + localZ;
        double endX = startX + warpX * held;
        double endZ = startZ + warpZ * held;
        for (int crossed = 0; crossed < MOST_CROSSINGS; crossed++) {
            double exitX = exit(startX, endX, face.minBlockX(), face.widthBlocks());
            double exitZ = exit(startZ, endZ, face.minBlockZ(), face.depthBlocks());
            if (exitX == INSIDE && exitZ == INSIDE) {
                break;
            }

            double exit = Math.min(exitX, exitZ);
            double crossX = startX + exit * (endX - startX);
            double crossZ = startZ + exit * (endZ - startZ);
            Crossing crossing = this.crossings.at(atlasFace, exitX <= exitZ
                    ? CuboidCrossings.westOrEast(endX, face.minBlockX())
                    : CuboidCrossings.northOrSouth(endZ, face.minBlockZ()));
            SeamTransform transform = crossing.transform();
            startX = transform.applyX(crossX, crossZ);
            startZ = transform.applyZ(crossX, crossZ);
            double carriedX = transform.applyX(endX, endZ);
            endZ = transform.applyZ(endX, endZ);
            endX = carriedX;
            atlasFace = crossing.face();
            face = this.faces.get(atlasFace);
        }

        return field.sampleOnFace(atlasFace,
                Mth.clamp(endX - face.minBlockX(), 0.0, face.widthBlocks()), y,
                Mth.clamp(endZ - face.minBlockZ(), 0.0, face.depthBlocks()));
    }

    private double held(CuboidFace face, double localX, double localZ, double warpBlocks) {
        double toVertex = Math.hypot(
                Math.min(localX, face.widthBlocks() - localX),
                Math.min(localZ, face.depthBlocks() - localZ));
        return Math.min(1.0, toVertex / Math.max(this.largestBlocks, warpBlocks));
    }

    private static double exit(double start, double end, int min, int size) {
        if (end < min) {
            return Mth.clamp((min - start) / (end - start), 0.0, 1.0);
        }

        int max = min + size;
        return end > max ? Mth.clamp((max - start) / (end - start), 0.0, 1.0) : INSIDE;
    }
}
