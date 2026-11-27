package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidEdge;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;
import com.exoticworlds.engine.noise.cuboid.CuboidCellGrid;
import com.exoticworlds.shape.WorldLoopPresets;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

class TilingCellGridTest {
    private static final int TYPE_CELL_WIDTH_BLOCKS = NoiseConstants.AQUIFER_FLUID_TYPE_CELL_WIDTH_BLOCKS;
    private static final int LEVEL_CELL_WIDTH_BLOCKS = NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH_BLOCKS;

    private static final int SWEEP_MIN_CHUNKS = 16;
    private static final int SWEEP_MAX_CHUNKS = 2000;

    private static final int[] PERIODICITY_CHUNK_WIDTHS = {16, 17, 18, 19, 20, 32, 36, 101, 582};

    // Coprime with both vanilla cell widths, so a sweep of it lands on every residue of either grid.
    private static final int PERIODICITY_STEP = 7;

    private static TilingCellGrid grid(int widthChunks, int vanillaCellWidthBlocks) {
        return TilingCellGrid.of(torus(widthChunks), vanillaCellWidthBlocks);
    }

    private static WorldFold torus(int widthChunks) {
        return WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(widthChunks)));
    }

    @Nested
    class VanillaWidthKept {
        @Test
        void everyPresetKeepsBothVanillaCellWidths() {
            for (WorldLoopPresets preset : WorldLoopPresets.values()) {
                for (int widthChunks : new int[] {
                        preset.getWidthChunks(),
                        preset.getWidthChunks() / preset.getNetherScale(),
                        preset.getEndWidthChunks()}) {
                    String world = preset.getId() + " at " + widthChunks + " chunks";
                    assertEquals(TYPE_CELL_WIDTH_BLOCKS, grid(widthChunks, TYPE_CELL_WIDTH_BLOCKS).xCellWidthBlocks(), world);
                    assertEquals(LEVEL_CELL_WIDTH_BLOCKS, grid(widthChunks, LEVEL_CELL_WIDTH_BLOCKS).xCellWidthBlocks(), world);
                }
            }
        }

        @Test
        void everyWidthOfWholeFourChunksKeepsTheSixtyFourBlockGrid() {
            for (int widthChunks = SWEEP_MIN_CHUNKS; widthChunks <= SWEEP_MAX_CHUNKS; widthChunks += 4) {
                assertEquals(TYPE_CELL_WIDTH_BLOCKS, grid(widthChunks, TYPE_CELL_WIDTH_BLOCKS).xCellWidthBlocks(),
                        "in a " + widthChunks + "-chunk world");
            }
        }

        @Test
        void everyWidthKeepsTheSixteenBlockGrid() {
            for (int widthChunks = SWEEP_MIN_CHUNKS; widthChunks <= SWEEP_MAX_CHUNKS; widthChunks++) {
                assertEquals(LEVEL_CELL_WIDTH_BLOCKS, grid(widthChunks, LEVEL_CELL_WIDTH_BLOCKS).xCellWidthBlocks(),
                        "in a " + widthChunks + "-chunk world");
            }
        }

        @Test
        void anUnboundedAxisKeepsTheVanillaCellWidth() {
            WorldFold fold = WorldFolds.of(ShapeFixtures.cylinder(
                    new WorldLoopBounds(new AxisBounds.Looped(-9, 9), AxisBounds.Unbounded.INSTANCE)));
            TilingCellGrid grid = TilingCellGrid.of(fold, TYPE_CELL_WIDTH_BLOCKS);

            assertEquals(72, grid.xCellWidthBlocks());
            assertEquals(TYPE_CELL_WIDTH_BLOCKS, grid.zCellWidthBlocks());
        }
    }

    @Nested
    class NearestDivisor {
        @Test
        void eighteenChunksLandsOnSeventyTwoBlocks() {
            assertEquals(72, grid(18, TYPE_CELL_WIDTH_BLOCKS).xCellWidthBlocks());
        }

        @Test
        void aPrimeMultipleOfAChunkTakesTheNearestDivisorItHas() {
            assertEquals(101, grid(101, TYPE_CELL_WIDTH_BLOCKS).xCellWidthBlocks());
        }

        @Test
        void anExactTieTakesTheFinerGrid() {
            assertEquals(48, grid(582, TYPE_CELL_WIDTH_BLOCKS).xCellWidthBlocks());
        }

        @Test
        void theCellWidthDividesTheAxisWidthOnEveryWidth() {
            for (int widthChunks = SWEEP_MIN_CHUNKS; widthChunks <= SWEEP_MAX_CHUNKS; widthChunks++) {
                int widthBlocks = widthChunks * 16;
                assertEquals(0, widthBlocks % grid(widthChunks, TYPE_CELL_WIDTH_BLOCKS).xCellWidthBlocks(),
                        "in a " + widthChunks + "-chunk world");
            }
        }

        @Test
        void eachAxisTakesItsOwnWidth() {
            WorldFold fold = WorldFolds.of(ShapeFixtures.torus(
                    new WorldLoopBounds(new AxisBounds.Looped(-9, 9), new AxisBounds.Looped(-16, 16))));
            TilingCellGrid grid = TilingCellGrid.of(fold, TYPE_CELL_WIDTH_BLOCKS);

            assertEquals(72, grid.xCellWidthBlocks());
            assertEquals(TYPE_CELL_WIDTH_BLOCKS, grid.zCellWidthBlocks());
        }
    }

    @Nested
    class Periodicity {
        @Test
        void theCellOriginIsTheSameAWorldWidthAway() {
            for (int widthChunks : PERIODICITY_CHUNK_WIDTHS) {
                assertPeriodic(widthChunks, TYPE_CELL_WIDTH_BLOCKS);
                assertPeriodic(widthChunks, LEVEL_CELL_WIDTH_BLOCKS);
            }
        }

        @Test
        void theVanillaCellWidthIsNotPeriodicOnEighteenChunks() {
            WorldFold fold = torus(18);
            WrapDomain domain = fold.getBlockDomain(Direction.Axis.X);
            int widthBlocks = domain.domainLength;
            boolean differs = false;

            for (int x = domain.lowerBound; x < domain.upperBound && !differs; x++) {
                differs = vanillaCellOrigin(domain, x) != vanillaCellOrigin(domain, x + widthBlocks);
            }

            assertTrue(differs, "the 64-block grid must break a lap away, or the periodicity test cannot go red");
        }

        private void assertPeriodic(int widthChunks, int vanillaCellWidthBlocks) {
            WorldFold fold = torus(widthChunks);
            TilingCellGrid grid = TilingCellGrid.of(fold, vanillaCellWidthBlocks);
            WrapDomain domain = fold.getBlockDomain(Direction.Axis.X);
            int widthBlocks = domain.domainLength;

            for (int x = domain.lowerBound - widthBlocks; x < domain.upperBound + widthBlocks; x += PERIODICITY_STEP) {
                String at = "at x=" + x + " in a " + widthChunks + "-chunk world on a "
                        + vanillaCellWidthBlocks + "-block vanilla grid";
                assertEquals(domain.wrap(grid.cellOriginX(x, x)), domain.wrap(grid.cellOriginX(x + widthBlocks, x)), at);
                assertEquals(domain.wrap(grid.cellOriginZ(x)), domain.wrap(grid.cellOriginZ(x + widthBlocks)), at);
            }
        }

        private int vanillaCellOrigin(WrapDomain domain, int blockCoord) {
            return domain.wrap(Math.floorDiv(blockCoord, TYPE_CELL_WIDTH_BLOCKS) * TYPE_CELL_WIDTH_BLOCKS);
        }
    }

    @Nested
    class Cuboid {
        private static final int Z_STEP = 5;
        private static final double BLOCK_CENTER = 0.5;

        @Test
        void everyBlockOfAFaceLandsInACellLyingWholeInThatFace() {
            for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
                for (int vanillaCellWidth : new int[] {TYPE_CELL_WIDTH_BLOCKS, LEVEL_CELL_WIDTH_BLOCKS}) {
                    CuboidCellGrid grid = cuboidGrid(atlas, vanillaCellWidth);
                    for (CuboidFace face : atlas.faces()) {
                        assertCellsLieWholeIn(atlas, grid, face);
                    }
                }
            }
        }

        @Test
        void theCellsOnEitherSideOfAnEdgeShareIt() {
            for (CuboidAtlas atlas : CuboidFixtures.atlases()) {
                for (int vanillaCellWidth : new int[] {TYPE_CELL_WIDTH_BLOCKS, LEVEL_CELL_WIDTH_BLOCKS}) {
                    CuboidCellGrid grid = cuboidGrid(atlas, vanillaCellWidth);
                    for (CuboidEdge edge : atlas.edges()) {
                        assertCellsShare(atlas, grid, edge);
                    }
                }
            }
        }

        @Test
        void edgesOfWholeFourChunksKeepTheSixtyFourBlockGrid() {
            CuboidCellGrid grid = cuboidGrid(CuboidAtlas.of(16, 24, 40, CuboidFixtures.MIN_CHUNK_X,
                    CuboidFixtures.MIN_CHUNK_Z), TYPE_CELL_WIDTH_BLOCKS);

            assertEquals(TYPE_CELL_WIDTH_BLOCKS, grid.aCellWidth());
            assertEquals(TYPE_CELL_WIDTH_BLOCKS, grid.bCellWidth());
            assertEquals(TYPE_CELL_WIDTH_BLOCKS, grid.cCellWidth());
        }

        @Test
        void eachBoxAxisTakesItsOwnWidth() {
            CuboidCellGrid grid = cuboidGrid(CuboidAtlas.of(18, 16, 101, CuboidFixtures.MIN_CHUNK_X,
                    CuboidFixtures.MIN_CHUNK_Z), TYPE_CELL_WIDTH_BLOCKS);

            assertEquals(72, grid.aCellWidth());
            assertEquals(TYPE_CELL_WIDTH_BLOCKS, grid.bCellWidth());
            assertEquals(101, grid.cCellWidth());
        }

        @Test
        void aBlockOutsideEveryFaceIsRefused() {
            CuboidAtlas atlas = CuboidAtlas.of(3, 5, 2, CuboidFixtures.MIN_CHUNK_X, CuboidFixtures.MIN_CHUNK_Z);
            CuboidCellGrid grid = cuboidGrid(atlas, TYPE_CELL_WIDTH_BLOCKS);
            CuboidFace top = CuboidFixtures.face(atlas, CuboidFace.Side.TOP);

            assertThrows(IllegalArgumentException.class,
                    () -> grid.cellOriginX(top.blockX(top.widthBlocks()), top.blockZ(0)));
        }

        private static CuboidCellGrid cuboidGrid(CuboidAtlas atlas, int vanillaCellWidth) {
            return assertInstanceOf(CuboidCellGrid.class,
                    AquiferCellGrid.of(CuboidFixtures.cuboidFold(atlas), vanillaCellWidth));
        }

        private static void assertCellsLieWholeIn(CuboidAtlas atlas, CuboidCellGrid grid, CuboidFace face) {
            int xCellWidth = grid.xCellWidth(face);
            int zCellWidth = grid.zCellWidth(face);
            assertEquals(0, face.widthBlocks() % xCellWidth, "x cells of " + face.side());
            assertEquals(0, face.depthBlocks() % zCellWidth, "z cells of " + face.side());

            for (int localX = 0; localX < face.widthBlocks(); localX += PERIODICITY_STEP) {
                for (int localZ = 0; localZ < face.depthBlocks(); localZ += Z_STEP) {
                    int blockX = face.blockX(localX);
                    int blockZ = face.blockZ(localZ);
                    int originX = grid.cellOriginX(blockX, blockZ);
                    int originZ = grid.cellOriginZ(blockX, blockZ);
                    String at = "at " + localX + ", " + localZ + " on " + face.side();
                    assertTrue(originX <= blockX && blockX < originX + xCellWidth, at);
                    assertTrue(originZ <= blockZ && blockZ < originZ + zCellWidth, at);
                    assertSame(face, atlas.faceAt(originX, originZ), at);
                    assertSame(face, atlas.faceAt(originX + xCellWidth - 1, originZ + zCellWidth - 1), at);
                }
            }
        }

        private static void assertCellsShare(CuboidAtlas atlas, CuboidCellGrid grid, CuboidEdge edge) {
            CuboidFace first = CuboidFixtures.face(atlas, edge.first());
            CuboidFace second = CuboidFixtures.face(atlas, edge.second());
            for (int along = 0; along < CuboidFixtures.edgeBlocks(first, edge.firstSide()); along++) {
                double[] inside = CuboidFixtures.pointBy(first, edge.firstSide(), along + BLOCK_CENTER,
                        -BLOCK_CENTER);
                double[] across = CuboidFixtures.carried(edge.transform(),
                        CuboidFixtures.pointBy(first, edge.firstSide(), along + BLOCK_CENTER, BLOCK_CENTER));
                double[][] carried = edgeSpan(grid, first, edge.firstSide(), inside);
                carried[0] = CuboidFixtures.carried(edge.transform(), carried[0]);
                carried[1] = CuboidFixtures.carried(edge.transform(), carried[1]);
                double[][] met = edgeSpan(grid, second, edge.secondSide(), across);
                assertTrue((samePoint(carried[0], met[0]) && samePoint(carried[1], met[1]))
                        || (samePoint(carried[0], met[1]) && samePoint(carried[1], met[0])),
                        "block " + along + " of " + edge.first() + " to " + edge.second());
            }
        }

        private static double[][] edgeSpan(CuboidCellGrid grid, CuboidFace face, Direction side, double[] point) {
            int blockX = Mth.floor(point[0]);
            int blockZ = Mth.floor(point[1]);
            boolean alongZ = CuboidFixtures.runsAlongZ(side);
            int start = alongZ
                    ? face.localZ(grid.cellOriginZ(blockX, blockZ))
                    : face.localX(grid.cellOriginX(blockX, blockZ));
            int width = alongZ ? grid.zCellWidth(face) : grid.xCellWidth(face);
            return new double[][] {
                    CuboidFixtures.pointBy(face, side, start, 0.0),
                    CuboidFixtures.pointBy(face, side, start + width, 0.0)};
        }

        private static boolean samePoint(double[] first, double[] second) {
            return first[0] == second[0] && first[1] == second[1];
        }
    }

    @Nested
    class SkewedLattice {
        private static final int[] SKEW_CHUNKS = {1, 3, -5, 8};

        @Test
        void anOddChunkSkewKeepsBothVanillaCellWidths() {
            for (int skewChunks : SKEW_CHUNKS) {
                WorldFold fold = skewed(skewChunks);
                assertEquals(TYPE_CELL_WIDTH_BLOCKS, TilingCellGrid.of(fold, TYPE_CELL_WIDTH_BLOCKS).xCellWidthBlocks());
                assertEquals(LEVEL_CELL_WIDTH_BLOCKS, TilingCellGrid.of(fold, LEVEL_CELL_WIDTH_BLOCKS).xCellWidthBlocks());
            }
        }

        @Test
        void aCellAndItsDeckCopyFoldOntoOneOrigin() {
            for (int skewChunks : SKEW_CHUNKS) {
                WorldFold fold = skewed(skewChunks);
                TranslationLattice lattice = fold.getBlockLattice();
                int widthBlocks = lattice.getX().domainLength;
                int heightBlocks = lattice.getZ().domainLength;
                int skewBlocks = lattice.getSkew();
                for (int vanillaCellWidthBlocks : new int[] {TYPE_CELL_WIDTH_BLOCKS, LEVEL_CELL_WIDTH_BLOCKS}) {
                    TilingCellGrid grid = TilingCellGrid.of(fold, vanillaCellWidthBlocks);
                    for (int x = lattice.getX().lowerBound - widthBlocks; x < lattice.getX().upperBound; x += PERIODICITY_STEP) {
                        for (int z = lattice.getZ().lowerBound - heightBlocks; z < lattice.getZ().upperBound;
                                z += PERIODICITY_STEP * 5) {
                            String at = "at " + x + ", " + z + " under a " + skewChunks + "-chunk skew on a "
                                    + vanillaCellWidthBlocks + "-block vanilla grid";
                            assertSameFoldedOrigin(lattice, grid, x, z, x + widthBlocks, z, at);
                            assertSameFoldedOrigin(lattice, grid, x, z, x + skewBlocks, z + heightBlocks, at);
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
            return new DeckGroupFold(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(32), skewChunks),
                    ShapeOptions.DEFAULT);
        }
    }
}
