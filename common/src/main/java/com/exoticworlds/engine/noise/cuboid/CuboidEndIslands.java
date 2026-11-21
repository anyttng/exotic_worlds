package com.exoticworlds.engine.noise.cuboid;

import java.util.List;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.engine.noise.PeriodicEndIslands;
import com.exoticworlds.engine.noise.cuboid.CuboidCrossings.Crossing;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class CuboidEndIslands {
    private static final int REACH = PeriodicEndIslands.ISLAND_SEARCH_CHUNK_REACH;
    private static final int SECTION_WIDTH = PeriodicEndIslands.SECTION_WIDTH;
    private static final double SECTION_CENTER = SECTION_WIDTH / 2.0;
    private static final double CHUNK_CENTER = CoordinateConstants.CHUNK_WIDTH / 2.0;

    private static final int NO_SIDE = -1;
    private static final int BOTH_AXES = 2;

    private final List<CuboidFace> faces;

    private final CuboidCrossings crossings;

    private CuboidEndIslands(List<CuboidFace> faces, CuboidCrossings crossings) {
        this.faces = faces;
        this.crossings = crossings;
    }

    public static CuboidEndIslands of(CuboidAtlas atlas) {
        return new CuboidEndIslands(atlas.faces(), CuboidCrossings.of(atlas));
    }

    public float outerHeightValue(SimplexNoise islandNoise, int blockX, int blockZ) {
        int atlasFace = CuboidOctaves.faceAt(this.faces, blockX, blockZ);
        CuboidFace face = this.faces.get(atlasFace);
        double sectionX = Math.floorDiv(blockX, SECTION_WIDTH) * SECTION_WIDTH + SECTION_CENTER;
        double sectionZ = Math.floorDiv(blockZ, SECTION_WIDTH) * SECTION_WIDTH + SECTION_CENTER;
        int chunkX = Math.floorDiv(blockX, CoordinateConstants.CHUNK_WIDTH);
        int chunkZ = Math.floorDiv(blockZ, CoordinateConstants.CHUNK_WIDTH);
        float height = PeriodicEndIslands.MIN_HEIGHT_VALUE;

        for (int xo = -REACH; xo <= REACH; xo++) {
            for (int zo = -REACH; zo <= REACH; zo++) {
                double cellX = (chunkX + xo) * CoordinateConstants.CHUNK_WIDTH + CHUNK_CENTER;
                double cellZ = (chunkZ + zo) * CoordinateConstants.CHUNK_WIDTH + CHUNK_CENTER;
                int edges = (sideX(face, cellX) == NO_SIDE ? 0 : 1) + (sideZ(face, cellZ) == NO_SIDE ? 0 : 1);
                height = Math.max(height,
                        islandHeightValue(islandNoise, atlasFace, cellX, cellZ, sectionX, sectionZ, true, edges));
                if (edges == BOTH_AXES) {
                    height = Math.max(height,
                            islandHeightValue(islandNoise, atlasFace, cellX, cellZ, sectionX, sectionZ, false, edges));
                }
            }
        }

        return height;
    }

    private float islandHeightValue(SimplexNoise islandNoise, int atlasFace, double cellX, double cellZ,
            double sectionX, double sectionZ, boolean alongXFirst, int edges) {
        CuboidFace face = this.faces.get(atlasFace);
        for (int crossed = 0; crossed < edges; crossed++) {
            int side = exitSide(face, cellX, cellZ, alongXFirst);
            if (side == NO_SIDE) {
                break;
            }

            Crossing crossing = this.crossings.at(atlasFace, side);
            SeamTransform transform = crossing.transform();
            double carriedCellX = transform.applyX(cellX, cellZ);
            cellZ = transform.applyZ(cellX, cellZ);
            cellX = carriedCellX;
            double carriedSectionX = transform.applyX(sectionX, sectionZ);
            sectionZ = transform.applyZ(sectionX, sectionZ);
            sectionX = carriedSectionX;
            atlasFace = crossing.face();
            face = this.faces.get(atlasFace);
        }

        if (exitSide(face, cellX, cellZ, alongXFirst) != NO_SIDE) {
            return PeriodicEndIslands.MIN_HEIGHT_VALUE;
        }

        int chunkX = Math.floorDiv(Mth.floor(cellX), CoordinateConstants.CHUNK_WIDTH);
        int chunkZ = Math.floorDiv(Mth.floor(cellZ), CoordinateConstants.CHUNK_WIDTH);
        if (!PeriodicEndIslands.hostsIsland(islandNoise, chunkX, chunkZ)) {
            return PeriodicEndIslands.MIN_HEIGHT_VALUE;
        }

        double anchorX = chunkX * CoordinateConstants.CHUNK_WIDTH + SECTION_CENTER;
        double anchorZ = chunkZ * CoordinateConstants.CHUNK_WIDTH + SECTION_CENTER;
        return PeriodicEndIslands.islandHeightValue(chunkX, chunkZ, (float) ((sectionX - anchorX) / SECTION_WIDTH),
                (float) ((sectionZ - anchorZ) / SECTION_WIDTH));
    }

    private static int exitSide(CuboidFace face, double x, double z, boolean alongXFirst) {
        int sideX = sideX(face, x);
        int sideZ = sideZ(face, z);
        return sideX != NO_SIDE && (alongXFirst || sideZ == NO_SIDE) ? sideX : sideZ;
    }

    private static int sideX(CuboidFace face, double x) {
        return x >= face.minBlockX() && x <= face.minBlockX() + face.widthBlocks()
                ? NO_SIDE
                : CuboidCrossings.westOrEast(x, face.minBlockX());
    }

    private static int sideZ(CuboidFace face, double z) {
        return z >= face.minBlockZ() && z <= face.minBlockZ() + face.depthBlocks()
                ? NO_SIDE
                : CuboidCrossings.northOrSouth(z, face.minBlockZ());
    }
}
