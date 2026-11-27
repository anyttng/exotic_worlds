package com.exoticworlds.engine.noise.cuboid;

import java.util.List;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.engine.noise.cuboid.CuboidCrossings.Crossing;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public final class CuboidStraightWalk {
    private static final double INSIDE = 2.0;

    private static final int MOST_CROSSINGS = 64;

    private static final double NO_HEIGHT = 0.0;

    private static final int NO_FACE = -1;

    private static final double CELL_CENTER = 0.5;

    private static final Reading<Landing> LANDED = (landing, atlasFace, localX, y, localZ) -> {
        landing.at(atlasFace, localX, localZ);
        return 0.0F;
    };

    @FunctionalInterface
    interface Landing {
        void at(int atlasFace, double localX, double localZ);
    }

    @FunctionalInterface
    interface Reading<T> {
        float at(T field, int atlasFace, double localX, double y, double localZ);
    }

    private final List<CuboidFace> faces;

    private final CuboidCrossings crossings;

    CuboidStraightWalk(List<CuboidFace> faces, CuboidCrossings crossings) {
        this.faces = faces;
        this.crossings = crossings;
    }

    public static CuboidStraightWalk of(CuboidAtlas atlas) {
        return new CuboidStraightWalk(atlas.faces(), CuboidCrossings.of(atlas));
    }

    void land(int atlasFace, double startX, double startZ, double endX, double endZ, Landing landing) {
        read(landing, LANDED, atlasFace, startX, startZ, endX, endZ, NO_HEIGHT);
    }

    public BlockPos landedBlock(BlockPos from, int blockX, int blockZ) {
        BlockPos.MutableBlockPos landed = new BlockPos.MutableBlockPos(blockX, from.getY(), blockZ);
        int startFace = faceAt(from.getX(), from.getZ());
        if (startFace == NO_FACE) {
            return landed;
        }

        land(startFace, from.getX() + CELL_CENTER, from.getZ() + CELL_CENTER,
                blockX + CELL_CENTER, blockZ + CELL_CENTER,
                (atlasFace, localX, localZ) -> {
                    CuboidFace face = this.faces.get(atlasFace);
                    landed.setX(face.blockX(Math.min(Mth.floor(localX), face.widthBlocks() - 1)));
                    landed.setZ(face.blockZ(Math.min(Mth.floor(localZ), face.depthBlocks() - 1)));
                });
        return landed;
    }

    private int faceAt(int blockX, int blockZ) {
        for (int face = 0; face < this.faces.size(); face++) {
            if (this.faces.get(face).contains(blockX, blockZ)) {
                return face;
            }
        }

        return NO_FACE;
    }

    <T> float read(T field, Reading<T> reading, int atlasFace, double startX, double startZ, double endX, double endZ,
            double y) {
        CuboidFace face = this.faces.get(atlasFace);
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

        return reading.at(field, atlasFace,
                Mth.clamp(endX - face.minBlockX(), 0.0, face.widthBlocks()), y,
                Mth.clamp(endZ - face.minBlockZ(), 0.0, face.depthBlocks()));
    }

    private static double exit(double start, double end, int min, int size) {
        if (end < min) {
            return Mth.clamp((min - start) / (end - start), 0.0, 1.0);
        }

        int max = min + size;
        return end > max ? Mth.clamp((max - start) / (end - start), 0.0, 1.0) : INSIDE;
    }
}
