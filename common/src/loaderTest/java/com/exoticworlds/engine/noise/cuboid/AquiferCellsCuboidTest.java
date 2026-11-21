package com.exoticworlds.engine.noise.cuboid;

import static com.exoticworlds.core.CuboidFixtures.MIN_CHUNK_X;
import static com.exoticworlds.core.CuboidFixtures.MIN_CHUNK_Z;
import static com.exoticworlds.core.CuboidFixtures.cuboidFold;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.NOISE_DATA;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.foldedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.engine.noise.AquiferCells;
import com.exoticworlds.engine.noise.NoiseConstants;

import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;

class AquiferCellsCuboidTest {
    private static final int CELL_Y = 3;
    private static final double SPREAD_XZ_SCALE = 1.0;
    private static final double SPREAD_Y_SCALE = 0.7;
    private static final int MIN_DISTINCT_CELLS = 2;

    private static final DensityFunction SPREAD = DensityFunctions.noise(NOISE_DATA, SPREAD_XZ_SCALE, SPREAD_Y_SCALE);

    private static final DensityFunction SHIFTED_SPREAD = DensityFunctions.shiftedNoise2d(
            DensityFunctions.shiftA(NOISE_DATA), DensityFunctions.shiftB(NOISE_DATA), SPREAD_XZ_SCALE, NOISE_DATA);

    private static final DensityFunction UNSHIFTED_SPREAD = DensityFunctions.shiftedNoise2d(
            DensityFunctions.zero(), DensityFunctions.zero(), SPREAD_XZ_SCALE, NOISE_DATA);

    private static final CuboidAtlas[] ATLASES = {
            CuboidAtlas.of(3, 5, 2, MIN_CHUNK_X, MIN_CHUNK_Z),
            CuboidAtlas.of(16, 24, 40, MIN_CHUNK_X, MIN_CHUNK_Z),
            CuboidAtlas.of(18, 16, 20, MIN_CHUNK_X, MIN_CHUNK_Z)};

    private static AquiferCells compile(CuboidAtlas atlas, DensityFunction spread) {
        DensityFunction zero = DensityFunctions.zero();
        return AquiferCells.compile(foldedContext(cuboidFold(atlas)),
                new Aquifer.Config(zero, zero, spread, spread, zero, zero));
    }

    @Test
    void bothGridsReadOneValueOverACellAndDifferentOnesOverAFace() {
        for (CuboidAtlas atlas : ATLASES) {
            AquiferCells cells = compile(atlas, SPREAD);
            for (CuboidFace face : atlas.faces()) {
                assertCellsHold(cells, face, NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH, true);
                assertCellsHold(cells, face, NoiseConstants.AQUIFER_FLUID_TYPE_CELL_WIDTH, false);
            }
        }
    }

    @Test
    void aShiftedSpreadReadsUnshiftedOnTheCellGrid() {
        for (CuboidAtlas atlas : ATLASES) {
            AquiferCells shifted = compile(atlas, SHIFTED_SPREAD);
            AquiferCells unshifted = compile(atlas, UNSHIFTED_SPREAD);
            for (CuboidFace face : atlas.faces()) {
                for (int localX = 0; localX < face.widthBlocks(); localX++) {
                    int blockX = face.blockX(localX);
                    int blockZ = face.blockZ(localX % face.depthBlocks());
                    assertEquals(unshifted.level(blockX, CELL_Y, blockZ), shifted.level(blockX, CELL_Y, blockZ));
                    assertEquals(unshifted.type(blockX, CELL_Y, blockZ), shifted.type(blockX, CELL_Y, blockZ));
                }
            }
        }
    }

    private static void assertCellsHold(AquiferCells cells, CuboidFace face, int vanillaCellWidth, boolean level) {
        CuboidCellGrid grid = (CuboidCellGrid) (level ? cells.levelGrid() : cells.typeGrid());
        int xCellWidth = grid.xCellWidth(face);
        int zCellWidth = grid.zCellWidth(face);
        Set<Float> values = new HashSet<>();
        for (int localX = 0; localX < face.widthBlocks(); localX += xCellWidth) {
            for (int localZ = 0; localZ < face.depthBlocks(); localZ += zCellWidth) {
                float atOrigin = read(cells, level, face.blockX(localX), face.blockZ(localZ));
                float atFarCorner = read(cells, level, face.blockX(localX + xCellWidth - 1),
                        face.blockZ(localZ + zCellWidth - 1));
                assertEquals(atOrigin, atFarCorner, "cell at " + localX + ", " + localZ + " on " + face.side());
                values.add(atOrigin);
            }
        }

        int cellCount = face.widthBlocks() / xCellWidth * (face.depthBlocks() / zCellWidth);
        assertTrue(values.size() >= Math.min(MIN_DISTINCT_CELLS, cellCount),
                values.size() + " values over " + cellCount + " cells of the " + vanillaCellWidth
                        + "-block grid on " + face.side());
    }

    private static float read(AquiferCells cells, boolean level, int blockX, int blockZ) {
        return level ? cells.level(blockX, CELL_Y, blockZ) : cells.type(blockX, CELL_Y, blockZ);
    }
}
