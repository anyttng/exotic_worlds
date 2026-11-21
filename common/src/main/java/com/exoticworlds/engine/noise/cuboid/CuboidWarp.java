package com.exoticworlds.engine.noise.cuboid;

import java.util.List;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.engine.noise.cuboid.CuboidCrossings.Crossing;

public final class CuboidWarp {
    private static final CuboidStraightWalk.Reading<CuboidOctaves> ON_FACE = CuboidOctaves::sampleOnFace;

    private final List<CuboidFace> faces;

    private final CuboidStraightWalk walk;

    private final CuboidAskingFace asking;

    private final CuboidOctaves shift;

    private final double shiftBlocks;

    private final double largestBlocks;

    private CuboidWarp(List<CuboidFace> faces, CuboidCrossings crossings, CuboidOctaves shift, double shiftBlocks,
            double largestBlocks) {
        this.faces = faces;
        this.walk = new CuboidStraightWalk(faces, crossings);
        this.asking = new CuboidAskingFace(faces, crossings);
        this.shift = shift;
        this.shiftBlocks = shiftBlocks;
        this.largestBlocks = largestBlocks;
    }

    public static CuboidWarp of(CuboidAtlas atlas, CuboidOctaves shift, double shiftBlocks, double largestBlocks) {
        return new CuboidWarp(atlas.faces(), CuboidCrossings.of(atlas), shift, shiftBlocks, largestBlocks);
    }

    public float sample(CuboidOctaves field, int blockX, double y, int blockZ) {
        int atlasFace = this.asking.faceAt(blockX, blockZ);
        if (atlasFace == CuboidAskingFace.NO_FACE) {
            Crossing crossing = this.asking.crossingOf(blockX, blockZ);
            return sampleFrom(field, crossing.face(), this.asking.localX(crossing, blockX, blockZ), y,
                    this.asking.localZ(crossing, blockX, blockZ));
        }

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
        return this.walk.read(field, ON_FACE, atlasFace, startX, startZ,
                startX + warpX * held, startZ + warpZ * held, y);
    }

    private double held(CuboidFace face, double localX, double localZ, double warpBlocks) {
        double toVertex = Math.hypot(
                Math.min(localX, face.widthBlocks() - localX),
                Math.min(localZ, face.depthBlocks() - localZ));
        return Math.min(1.0, toVertex / Math.max(this.largestBlocks, warpBlocks));
    }
}
