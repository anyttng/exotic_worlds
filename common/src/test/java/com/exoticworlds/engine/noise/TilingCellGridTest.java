package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.core.Direction;

class TilingCellGridTest {
    private static final int TYPE_CELL_WIDTH = NoiseConstants.AQUIFER_FLUID_TYPE_CELL_WIDTH;
    private static final int LEVEL_CELL_WIDTH = NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH;

    private static final int SWEEP_MIN_CHUNKS = 16;
    private static final int SWEEP_MAX_CHUNKS = 2000;

    private static final int[] PERIODICITY_CHUNK_WIDTHS = {16, 17, 18, 19, 20, 32, 36, 101, 582};

    // Coprime with both vanilla cell widths, so a sweep of it lands on every residue of either grid.
    private static final int PERIODICITY_STEP = 7;

    private static TilingCellGrid grid(int chunkWidth, int vanillaCellWidth) {
        return TilingCellGrid.of(torus(chunkWidth), vanillaCellWidth);
    }

    private static WorldFold torus(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)));
    }

    @Nested
    class VanillaWidthKept {
        @Test
        void everyPresetKeepsBothVanillaCellWidths() {
            for (WorldLoopPresets preset : WorldLoopPresets.values()) {
                for (int chunkWidth : new int[] {
                        preset.chunkWidth(),
                        preset.chunkWidth() / preset.netherScale(),
                        preset.endChunkWidth()}) {
                    String world = preset.id() + " at " + chunkWidth + " chunks";
                    assertEquals(TYPE_CELL_WIDTH, grid(chunkWidth, TYPE_CELL_WIDTH).xCellWidth(), world);
                    assertEquals(LEVEL_CELL_WIDTH, grid(chunkWidth, LEVEL_CELL_WIDTH).xCellWidth(), world);
                }
            }
        }

        @Test
        void everyWidthOfWholeFourChunksKeepsTheSixtyFourBlockGrid() {
            for (int chunkWidth = SWEEP_MIN_CHUNKS; chunkWidth <= SWEEP_MAX_CHUNKS; chunkWidth += 4) {
                assertEquals(TYPE_CELL_WIDTH, grid(chunkWidth, TYPE_CELL_WIDTH).xCellWidth(),
                        "in a " + chunkWidth + "-chunk world");
            }
        }

        @Test
        void everyWidthKeepsTheSixteenBlockGrid() {
            for (int chunkWidth = SWEEP_MIN_CHUNKS; chunkWidth <= SWEEP_MAX_CHUNKS; chunkWidth++) {
                assertEquals(LEVEL_CELL_WIDTH, grid(chunkWidth, LEVEL_CELL_WIDTH).xCellWidth(),
                        "in a " + chunkWidth + "-chunk world");
            }
        }

        @Test
        void anUnboundedAxisKeepsTheVanillaCellWidth() {
            WorldFold transformer = WorldFolds.of(FlatShape.cylinder(
                    new WorldLoopBounds(new AxisBounds.Looped(-9, 9), AxisBounds.Unbounded.INSTANCE)));
            TilingCellGrid grid = TilingCellGrid.of(transformer, TYPE_CELL_WIDTH);

            assertEquals(72, grid.xCellWidth());
            assertEquals(TYPE_CELL_WIDTH, grid.zCellWidth());
        }
    }

    @Nested
    class NearestDivisor {
        @Test
        void eighteenChunksLandsOnSeventyTwoBlocks() {
            assertEquals(72, grid(18, TYPE_CELL_WIDTH).xCellWidth());
        }

        @Test
        void aPrimeMultipleOfAChunkTakesTheNearestDivisorItHas() {
            assertEquals(101, grid(101, TYPE_CELL_WIDTH).xCellWidth());
        }

        @Test
        void anExactTieTakesTheFinerGrid() {
            assertEquals(48, grid(582, TYPE_CELL_WIDTH).xCellWidth());
        }

        @Test
        void theCellWidthDividesTheAxisWidthOnEveryWidth() {
            for (int chunkWidth = SWEEP_MIN_CHUNKS; chunkWidth <= SWEEP_MAX_CHUNKS; chunkWidth++) {
                int width = chunkWidth * 16;
                assertEquals(0, width % grid(chunkWidth, TYPE_CELL_WIDTH).xCellWidth(),
                        "in a " + chunkWidth + "-chunk world");
            }
        }

        @Test
        void eachAxisTakesItsOwnWidth() {
            WorldFold transformer = WorldFolds.of(FlatShape.torus(
                    new WorldLoopBounds(new AxisBounds.Looped(-9, 9), new AxisBounds.Looped(-16, 16))));
            TilingCellGrid grid = TilingCellGrid.of(transformer, TYPE_CELL_WIDTH);

            assertEquals(72, grid.xCellWidth());
            assertEquals(TYPE_CELL_WIDTH, grid.zCellWidth());
        }
    }

    @Nested
    class Periodicity {
        @Test
        void theCellOriginIsTheSameAWorldWidthAway() {
            for (int chunkWidth : PERIODICITY_CHUNK_WIDTHS) {
                assertPeriodic(chunkWidth, TYPE_CELL_WIDTH);
                assertPeriodic(chunkWidth, LEVEL_CELL_WIDTH);
            }
        }

        @Test
        void theVanillaCellWidthIsNotPeriodicOnEighteenChunks() {
            WorldFold transformer = torus(18);
            WrapDomain domain = transformer.blockDomain(Direction.Axis.X);
            int width = domain.domainLength;
            boolean differs = false;

            for (int x = domain.lowerBound; x < domain.upperBound && !differs; x++) {
                differs = vanillaCellOrigin(domain, x) != vanillaCellOrigin(domain, x + width);
            }

            assertTrue(differs, "the 64-block grid must break a lap away, or the periodicity test cannot go red");
        }

        private void assertPeriodic(int chunkWidth, int vanillaCellWidth) {
            WorldFold transformer = torus(chunkWidth);
            TilingCellGrid grid = TilingCellGrid.of(transformer, vanillaCellWidth);
            WrapDomain domain = transformer.blockDomain(Direction.Axis.X);
            int width = domain.domainLength;

            for (int x = domain.lowerBound - width; x < domain.upperBound + width; x += PERIODICITY_STEP) {
                String at = "at x=" + x + " in a " + chunkWidth + "-chunk world on a "
                        + vanillaCellWidth + "-block vanilla grid";
                assertEquals(domain.wrap(grid.cellOriginX(x, x)), domain.wrap(grid.cellOriginX(x + width, x)), at);
                assertEquals(domain.wrap(grid.cellOriginZ(x)), domain.wrap(grid.cellOriginZ(x + width)), at);
            }
        }

        private int vanillaCellOrigin(WrapDomain domain, int blockCoord) {
            return domain.wrap(Math.floorDiv(blockCoord, TYPE_CELL_WIDTH) * TYPE_CELL_WIDTH);
        }
    }

    @Nested
    class SkewedLattice {
        private static final int[] SKEW_CHUNKS = {1, 3, -5, 8};

        @Test
        void anOddChunkSkewKeepsBothVanillaCellWidths() {
            for (int skewChunks : SKEW_CHUNKS) {
                WorldFold transformer = skewed(skewChunks);
                assertEquals(TYPE_CELL_WIDTH, TilingCellGrid.of(transformer, TYPE_CELL_WIDTH).xCellWidth());
                assertEquals(LEVEL_CELL_WIDTH, TilingCellGrid.of(transformer, LEVEL_CELL_WIDTH).xCellWidth());
            }
        }

        @Test
        void aCellAndItsDeckCopyFoldOntoOneOrigin() {
            for (int skewChunks : SKEW_CHUNKS) {
                WorldFold transformer = skewed(skewChunks);
                TranslationLattice lattice = transformer.blockLattice();
                int width = lattice.x().domainLength;
                int height = lattice.z().domainLength;
                int skew = lattice.skew();
                for (int vanillaCellWidth : new int[] {TYPE_CELL_WIDTH, LEVEL_CELL_WIDTH}) {
                    TilingCellGrid grid = TilingCellGrid.of(transformer, vanillaCellWidth);
                    for (int x = lattice.x().lowerBound - width; x < lattice.x().upperBound; x += PERIODICITY_STEP) {
                        for (int z = lattice.z().lowerBound - height; z < lattice.z().upperBound;
                                z += PERIODICITY_STEP * 5) {
                            String at = "at " + x + ", " + z + " under a " + skewChunks + "-chunk skew on a "
                                    + vanillaCellWidth + "-block vanilla grid";
                            assertSameFoldedOrigin(lattice, grid, x, z, x + width, z, at);
                            assertSameFoldedOrigin(lattice, grid, x, z, x + skew, z + height, at);
                        }
                    }
                }
            }
        }

        private void assertSameFoldedOrigin(TranslationLattice lattice, TilingCellGrid grid, int x, int z,
                int copyX, int copyZ, String at) {
            int originX = grid.cellOriginX(x, z);
            int originZ = grid.cellOriginZ(z);
            int copyOriginX = grid.cellOriginX(copyX, copyZ);
            int copyOriginZ = grid.cellOriginZ(copyZ);
            assertEquals(lattice.foldX(originX, originZ), lattice.foldX(copyOriginX, copyOriginZ), at);
            assertEquals(lattice.foldZ(originZ), lattice.foldZ(copyOriginZ), at);
        }

        private WorldFold skewed(int skewChunks) {
            return new DeckGroupFold(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(32), skewChunks));
        }
    }
}
