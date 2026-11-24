package com.exoticworlds.core.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.fold.loop.WorldLoopFold;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.WorldLoopPresets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

class WorldFoldsTest {
    private static final int SQUARE_CHUNKS = 32;
    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(SQUARE_CHUNKS);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE);
    private static final WorldLoopBounds Z_ONLY =
            new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16));

    private static final int LAPS_OUT = 3;
    private static final int LAPS_OUT_CHUNKS = LAPS_OUT * SQUARE_CHUNKS + SQUARE_CHUNKS / 2;
    private static final int LAPS_OUT_BLOCKS = SectionPos.sectionToBlockCoord(LAPS_OUT_CHUNKS);
    private static final int STEP_CHUNKS = 3;
    private static final int STEP_BLOCKS = 29;

    private static final ShapeOption<Boolean> OPTION = new ShapeOption<>("test_option", 0, Codec.BOOL, false);

    private static List<FlatShape> decomposable() {
        return List.of(
                FlatShape.rectangle(),
                ShapeFixtures.cylinder(X_ONLY),
                ShapeFixtures.cylinder(Z_ONLY),
                ShapeFixtures.torus(SQUARE));
    }

    private static final FlatShape SKEWED = FlatShape.latticeTorus(SQUARE, 5);

    private static List<FlatShape> mirrored() {
        return List.of(
                ShapeFixtures.mirrored(Z_ONLY, Direction.Axis.X, 3),
                ShapeFixtures.mirrored(SQUARE, Direction.Axis.Z, -7));
    }

    private static List<FlatShape> coupled() {
        return Stream.concat(Stream.of(SKEWED), mirrored().stream()).toList();
    }

    @Test
    void aWorldTooNarrowToGenerateIsRefused() {
        assertTrue(WorldFolds.verifyGeneratable(ShapeFixtures.torus(WorldLoopBounds.ofWidth(8))).isError());
        assertTrue(WorldFolds.verifyGeneratable(ShapeFixtures.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 8)))
                .isError());
    }

    @Test
    void aWorldAtTheGenerationFloorAndAnEndlessAxisAreAccepted() {
        assertTrue(WorldFolds.verifyGeneratable(
                ShapeFixtures.torus(WorldLoopBounds.ofWidth(WorldLoopSizes.MIN_WIDTH_CHUNKS)))
                .result().isPresent());
        assertTrue(WorldFolds.verifyGeneratable(FlatShape.rectangle()).result().isPresent());
    }

    @Test
    void everyShippedPresetClearsTheGenerationFloor() {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            for (int widthChunks : new int[] {preset.getWidthChunks(), preset.getEndWidthChunks(),
                    preset.getWidthChunks() / preset.getNetherScale()}) {
                assertTrue(WorldFolds.verifyGeneratable(
                        ShapeFixtures.torus(WorldLoopBounds.ofWidth(widthChunks))).result().isPresent(),
                        preset.getId());
            }
        }
    }

    @Test
    void theFoldContractIsNotHeldToTheGenerationFloor() {
        assertTrue(WorldFolds.verifyFoldable(ShapeFixtures.torus(WorldLoopBounds.ofWidth(8)))
                .result().isPresent(),
                "narrow worlds are the arithmetic fixtures of half this suite; a width floor here breaks them");
    }

    @Test
    void aDecomposableShapeGetsAFoldOverItsOwnBounds() {
        for (FlatShape shape : decomposable()) {
            assertEquals(shape.bounds(), WorldFolds.of(shape).getBounds(), shape.toString());
        }
    }

    @Test
    void anUnboundedShapeStillGetsAFoldThatKnowsItDoesNotWrap() {
        assertFalse(WorldFolds.of(FlatShape.rectangle()).isWrapped());
        assertTrue(WorldFolds.of(ShapeFixtures.cylinder(X_ONLY)).isWrapped());
    }

    @Test
    void aShapeLoopingOneAxisCarriesTheOptionsItWasBuiltWith() {
        FlatShape cylinder = ShapeFixtures.cylinder(X_ONLY);
        ShapeOptions chosen = ShapeOptions.DEFAULT.with(OPTION, true);

        assertTrue(WorldFolds.of(cylinder, chosen).getOptions().get(OPTION));
        assertTrue(new DeckGroupFold(cylinder, chosen).getOptions().get(OPTION));
    }

    @Test
    void noopIsTheRectanglesFold() {
        assertFalse(WorldFolds.NOOP.isWrapped());
        assertEquals(WorldLoopBounds.UNBOUNDED, WorldFolds.NOOP.getBounds());
        assertTrue(WorldFolds.NOOP.isDecomposedPerAxis());
    }

    @Test
    void aMirroredShapeIsRefusedAndNamed() {
        for (FlatShape shape : mirrored()) {
            IllegalArgumentException refused =
                    assertThrows(IllegalArgumentException.class, () -> WorldFolds.of(shape), shape.toString());
            assertTrue(refused.getMessage().contains(shape.getIdentification().toString()), refused.getMessage());
        }
    }

    @Test
    void aSkewedShapeGetsTheDeckGroupFoldOverItsLattice() {
        WorldFold fold = WorldFolds.of(SKEWED, ShapeOptions.DEFAULT.with(OPTION, true));

        assertTrue(fold instanceof DeckGroupFold, fold.getClass().getName());
        assertEquals(SKEWED.bounds(), fold.getBounds());
        assertFalse(fold.isDecomposedPerAxis());
        assertEquals(SKEWED.skewChunks(), fold.getChunkLattice().getSkew());
        assertTrue(fold.getOptions().get(OPTION));
    }

    @Test
    void aDecomposableShapeKeepsThePerAxisFold() {
        for (FlatShape shape : decomposable()) {
            assertTrue(WorldFolds.of(shape) instanceof WorldLoopFold, shape.toString());
        }
    }

    @Test
    void theFixturesSplitExactlyOnTheCapabilityFlag() {
        decomposable().forEach(shape -> assertTrue(shape.isDecomposedPerAxis(), shape.toString()));
        coupled().forEach(shape -> assertFalse(shape.isDecomposedPerAxis(), shape.toString()));
    }

    @Test
    void theLocalIndexFloorSeparatesASkewFromAMirror() {
        assertSame(SKEWED, WorldFolds.verifyFoldable(SKEWED).getOrThrow());

        for (FlatShape shape : mirrored()) {
            DataResult<FlatShape> result = WorldFolds.verifyFoldable(shape);
            assertTrue(result.isError(), shape.toString());
            assertTrue(result.error().orElseThrow().message().contains(shape.getIdentification().toString()),
                    result.toString());
        }
    }

    @Test
    void everyShapeThatLosesItsLocalIndicesAlsoFailsToDecompose() {
        for (FlatShape shape : Stream.concat(decomposable().stream(), coupled().stream()).toList()) {
            if (!shape.isPreservingLocalIndices()) {
                assertFalse(shape.isDecomposedPerAxis(), shape.toString());
            }
        }
    }

    @Test
    void theFoldableGatePassesEveryShapeTheEngineCanCarry() {
        for (FlatShape shape : Stream.concat(decomposable().stream(), Stream.of(SKEWED)).toList()) {
            assertSame(shape, WorldFolds.verifyFoldable(shape).getOrThrow(), shape.toString());
        }
    }

    @Test
    void foldingXFirstLeavesTheFoldOfZUnchanged() {
        for (FlatShape shape : Stream.concat(decomposable().stream(), Stream.of(SKEWED)).toList()) {
            WorldFold fold = WorldFolds.of(shape);
            for (int x = -LAPS_OUT_BLOCKS; x <= LAPS_OUT_BLOCKS; x += STEP_BLOCKS) {
                for (int z = -LAPS_OUT_BLOCKS; z <= LAPS_OUT_BLOCKS; z += STEP_BLOCKS) {
                    long folded = fold.foldBlockNode(BlockPos.asLong(x, 0, z));
                    long xFirst = fold.foldBlockNode(BlockPos.asLong(BlockPos.getX(folded), 0, z));
                    assertEquals(BlockPos.getZ(folded), BlockPos.getZ(xFirst), shape + " block " + x + ", " + z);
                }
            }

            for (int x = -LAPS_OUT_CHUNKS; x <= LAPS_OUT_CHUNKS; x += STEP_CHUNKS) {
                for (int z = -LAPS_OUT_CHUNKS; z <= LAPS_OUT_CHUNKS; z += STEP_CHUNKS) {
                    long folded = fold.foldChunkKey(ChunkPos.pack(x, z));
                    long xFirst = fold.foldChunkKey(ChunkPos.pack(ChunkPos.getX(folded), z));
                    assertEquals(ChunkPos.getZ(folded), ChunkPos.getZ(xFirst), shape + " chunk " + x + ", " + z);
                }
            }
        }
    }
}
