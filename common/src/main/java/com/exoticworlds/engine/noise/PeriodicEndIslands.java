package com.exoticworlds.engine.noise;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class PeriodicEndIslands {
    public static final int SUB_SECTION_WIDTH_BLOCKS = 8;
    private static final int SECTIONS_PER_CHUNK = 2;

    private static final float ISLAND_PEAK_HEIGHT = 100.0F;
    public static final float MIN_HEIGHT_VALUE = -100.0F;
    private static final float MAX_HEIGHT_VALUE = 80.0F;

    public static final int ISLAND_SEARCH_REACH_CHUNKS = 12;
    private static final long OUTER_ISLAND_MIN_SQR_DISTANCE_CHUNKS = 4096L;
    private static final float ISLAND_NOISE_THRESHOLD = -0.9F;

    private static final float ISLAND_SIZE_X_FACTOR = 3439.0F;
    private static final float ISLAND_SIZE_Z_FACTOR = 147.0F;
    private static final float ISLAND_SIZE_SPREAD = 13.0F;
    private static final float ISLAND_SIZE_MIN = 9.0F;

    private static final float ZERO_DENSITY_HEIGHT_VALUE = 8.0F;
    private static final float HEIGHT_VALUE_PER_DENSITY = 128.0F;

    public static float outerHeightValue(SimplexNoise islandNoise, WorldFold fold, int blockX, int blockZ) {
        float height = MIN_HEIGHT_VALUE;

        int chunkX = Math.floorDiv(blockX, CoordinateConstants.CHUNK_WIDTH_BLOCKS);
        int chunkZ = Math.floorDiv(blockZ, CoordinateConstants.CHUNK_WIDTH_BLOCKS);
        int subSectionX = Math.floorMod(Math.floorDiv(blockX, SUB_SECTION_WIDTH_BLOCKS), SECTIONS_PER_CHUNK);
        int subSectionZ = Math.floorMod(Math.floorDiv(blockZ, SUB_SECTION_WIDTH_BLOCKS), SECTIONS_PER_CHUNK);
        TranslationLattice lattice = fold.getChunkLattice();

        for (int xo = -ISLAND_SEARCH_REACH_CHUNKS; xo <= ISLAND_SEARCH_REACH_CHUNKS; xo++) {
            for (int zo = -ISLAND_SEARCH_REACH_CHUNKS; zo <= ISLAND_SEARCH_REACH_CHUNKS; zo++) {
                long cellX = lattice.foldX(chunkX + xo, chunkZ + zo);
                long cellZ = lattice.foldZ(chunkZ + zo);
                if (isHostingIsland(islandNoise, cellX, cellZ)) {
                    float xd = subSectionX - xo * SECTIONS_PER_CHUNK;
                    float zd = subSectionZ - zo * SECTIONS_PER_CHUNK;
                    height = Math.max(height, islandHeightValue(cellX, cellZ, xd, zd));
                }
            }
        }

        return height;
    }

    public static boolean isHostingIsland(SimplexNoise islandNoise, long cellX, long cellZ) {
        return cellX * cellX + cellZ * cellZ > OUTER_ISLAND_MIN_SQR_DISTANCE_CHUNKS
                && islandNoise.get((double) cellX, (double) cellZ) < ISLAND_NOISE_THRESHOLD;
    }

    public static float islandHeightValue(long cellX, long cellZ, float sectionsX, float sectionsZ) {
        float islandSize = (Mth.abs((float) cellX) * ISLAND_SIZE_X_FACTOR
                + Mth.abs((float) cellZ) * ISLAND_SIZE_Z_FACTOR) % ISLAND_SIZE_SPREAD + ISLAND_SIZE_MIN;
        return Mth.clamp(ISLAND_PEAK_HEIGHT - Mth.sqrt(sectionsX * sectionsX + sectionsZ * sectionsZ) * islandSize,
                MIN_HEIGHT_VALUE, MAX_HEIGHT_VALUE);
    }

    public static float density(float heightValue) {
        return (heightValue - ZERO_DENSITY_HEIGHT_VALUE) / HEIGHT_VALUE_PER_DENSITY;
    }

    private PeriodicEndIslands() {
    }
}
