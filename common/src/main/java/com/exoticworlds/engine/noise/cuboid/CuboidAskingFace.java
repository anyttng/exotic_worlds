package com.exoticworlds.engine.noise.cuboid;

import java.util.List;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.cuboid.CuboidCrossings.Crossing;

import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

final class CuboidAskingFace {
    static final int NO_FACE = -1;

    private static final int ORDERS_PER_FACE = 8;
    private static final int ONE_BLOCK = 1;

    private final List<CuboidFace> faces;

    private final CuboidCrossings crossings;

    private final Crossing[][] pastTwoEdges;

    CuboidAskingFace(List<CuboidFace> faces, CuboidCrossings crossings) {
        this.faces = faces;
        this.crossings = crossings;
        this.pastTwoEdges = new Crossing[faces.size()][ORDERS_PER_FACE];
        for (int face = 0; face < faces.size(); face++) {
            for (int order = 0; order < ORDERS_PER_FACE; order++) {
                this.pastTwoEdges[face][order] = pastTwoEdges(face, order);
            }
        }
    }

    static CuboidAskingFace of(CuboidAtlas atlas) {
        return new CuboidAskingFace(atlas.faces(), CuboidCrossings.of(atlas));
    }

    int faceAt(int blockX, int blockZ) {
        for (int i = 0; i < this.faces.size(); i++) {
            if (this.faces.get(i).contains(blockX, blockZ)) {
                return i;
            }
        }

        return NO_FACE;
    }

    Crossing crossingOf(int blockX, int blockZ) {
        ChunkPos asking = GenerationFoldContext.getContext().getAskingChunk();
        int askingFace = asking == null ? NO_FACE : faceAt(asking.getMinBlockX(), asking.getMinBlockZ());
        if (askingFace == NO_FACE) {
            throw onNoFace(blockX, blockZ);
        }

        CuboidFace face = this.faces.get(askingFace);
        boolean pastX = isPast(face.localX(blockX), face.widthBlocks());
        boolean pastZ = isPast(face.localZ(blockZ), face.depthBlocks());
        if (!pastX && !pastZ) {
            throw onNoFace(blockX, blockZ);
        }

        int sideX = CuboidCrossings.westOrEast(blockX, face.minBlockX());
        int sideZ = CuboidCrossings.northOrSouth(blockZ, face.minBlockZ());
        if (pastX && pastZ) {
            boolean alongXFirst = blocksPast(face.localX(blockX), face.widthBlocks())
                    >= blocksPast(face.localZ(blockZ), face.depthBlocks());
            return this.pastTwoEdges[askingFace][alongXFirst ? orderOf(sideX, sideZ) : orderOf(sideZ, sideX)];
        }

        return this.crossings.at(askingFace, pastX ? sideX : sideZ);
    }

    private Crossing pastTwoEdges(int askingFace, int order) {
        int firstSide = order >> 1;
        boolean alongXFirst = firstSide == CuboidCrossings.WEST || firstSide == CuboidCrossings.EAST;
        int secondSide = (alongXFirst ? CuboidCrossings.NORTH : CuboidCrossings.WEST) + (order & 1);
        int sideX = alongXFirst ? firstSide : secondSide;
        int sideZ = alongXFirst ? secondSide : firstSide;
        CuboidFace face = this.faces.get(askingFace);
        int blockX = sideX == CuboidCrossings.WEST
                ? face.minBlockX() - ONE_BLOCK
                : face.minBlockX() + face.widthBlocks();
        int blockZ = sideZ == CuboidCrossings.NORTH
                ? face.minBlockZ() - ONE_BLOCK
                : face.minBlockZ() + face.depthBlocks();
        Crossing first = this.crossings.at(askingFace, firstSide);
        int carriedX = first.transform().applyCellX(blockX, blockZ);
        int carriedZ = first.transform().applyCellZ(blockX, blockZ);
        CuboidFace beside = this.faces.get(first.face());
        Crossing second = this.crossings.at(first.face(), isPast(beside.localX(carriedX), beside.widthBlocks())
                ? CuboidCrossings.westOrEast(carriedX, beside.minBlockX())
                : CuboidCrossings.northOrSouth(carriedZ, beside.minBlockZ()));
        return new Crossing(second.face(), first.transform().then(second.transform()));
    }

    private static int orderOf(int firstSide, int secondSide) {
        return (firstSide << 1) + (secondSide & 1);
    }

    private static int blocksPast(int local, int size) {
        return local < 0 ? -local : local - size + 1;
    }

    double localX(Crossing crossing, double x, double z) {
        CuboidFace face = this.faces.get(crossing.face());
        return Mth.clamp(crossing.transform().applyX(x, z) - face.minBlockX(), 0.0, face.widthBlocks());
    }

    double localZ(Crossing crossing, double x, double z) {
        CuboidFace face = this.faces.get(crossing.face());
        return Mth.clamp(crossing.transform().applyZ(x, z) - face.minBlockZ(), 0.0, face.depthBlocks());
    }

    private static boolean isPast(int local, int size) {
        return local < 0 || local >= size;
    }

    private static IllegalArgumentException onNoFace(int blockX, int blockZ) {
        return new IllegalArgumentException(
                "A cuboid noise samples inside a face, got x=" + blockX + " z=" + blockZ);
    }
}
