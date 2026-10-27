package com.exoticworlds.core;

import static com.exoticworlds.core.WorldFoldFixture.EVEN;
import static com.exoticworlds.core.WorldFoldFixture.ODD;
import static com.exoticworlds.core.WorldFoldFixture.UNEVEN;
import static com.exoticworlds.core.WorldFoldFixture.UNIT;
import static com.exoticworlds.core.WorldFoldFixture.X_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;

class BlockChunkAgreementTest {
    private static final long SEED = 0xB10CL;
    private static final int SAMPLES = 2000;

    private static final int[] HEIGHTS = {-64, -1, 0, 319};

    private static final List<WorldFold> FOLDS = List.of(EVEN, ODD, UNEVEN, UNIT, X_ONLY, WorldFolds.NOOP);

    private static int sampleBlock(Random random, WrapDomain blockDomain) {
        int reach = 3 * (blockDomain instanceof WrapDomain.Noop ? 16_000 : Math.min(blockDomain.domainLength, 16_000));
        return random.nextInt(2 * reach + 1) - reach;
    }

    private static int sampleHeight(Random random) {
        return random.nextInt(384) - 64;
    }

    private static String on(String axis, WorldFold fold) {
        return "on " + axis + " in " + fold;
    }

    private static String in(WorldFold fold) {
        return "in " + fold;
    }

    private static WrapDomain blockX(WorldFold fold) {
        return fold.blockDomain(Direction.Axis.X);
    }

    private static WrapDomain blockZ(WorldFold fold) {
        return fold.blockDomain(Direction.Axis.Z);
    }

    private static WrapDomain chunkX(WorldFold fold) {
        return fold.chunkDomain(Direction.Axis.X);
    }

    private static WrapDomain chunkZ(WorldFold fold) {
        return fold.chunkDomain(Direction.Axis.Z);
    }

    @Test
    void wrapThenConvertEqualsConvertThenWrap() {
        Random random = new Random(SEED);
        for (WorldFold fold : FOLDS) {
            for (int i = 0; i < SAMPLES; i++) {
                checkAgreement(blockX(fold), chunkX(fold),
                        sampleBlock(random, blockX(fold)), "X", fold);
                checkAgreement(blockZ(fold), chunkZ(fold),
                        sampleBlock(random, blockZ(fold)), "Z", fold);
            }
        }
    }

    @Test
    void theBoundsAndWholeWidthsOutAgreeLikeAnyOtherCoordinate() {
        for (WorldFold fold : FOLDS) {
            for (int edge : edges(blockX(fold))) {
                checkAgreement(blockX(fold), chunkX(fold), edge, "X", fold);
            }
            for (int edge : edges(blockZ(fold))) {
                checkAgreement(blockZ(fold), chunkZ(fold), edge, "Z", fold);
            }
        }
    }

    private static int[] edges(WrapDomain blockDomain) {
        int width = blockDomain.domainLength;
        return new int[] {
                blockDomain.lowerBound - 1, blockDomain.lowerBound, blockDomain.lowerBound + 1,
                blockDomain.upperBound - 1, blockDomain.upperBound, blockDomain.upperBound + 1,
                blockDomain.lowerBound - 3 * width, blockDomain.upperBound - 1 + 3 * width};
    }

    private static void checkAgreement(WrapDomain blockDomain, WrapDomain chunkDomain, int blockCoord,
            String axis, WorldFold fold) {
        int wrappedBlock = blockDomain.wrap(blockCoord);
        assertEquals(chunkDomain.wrap(SectionPos.blockToSectionCoord(blockCoord)),
                SectionPos.blockToSectionCoord(wrappedBlock),
                () -> "wrap(" + blockCoord + ") landed in the wrong chunk " + on(axis, fold));
        assertEquals(Math.floorMod(blockCoord, 16), Math.floorMod(wrappedBlock, 16),
                () -> "wrap(" + blockCoord + ") moved inside its chunk " + on(axis, fold));
    }

    @Test
    void aFoldedPositionKeysOntoASectionTheWorldHas() {
        Random random = new Random(SEED);
        for (WorldFold fold : FOLDS) {
            for (int i = 0; i < SAMPLES; i++) {
                checkKey(new BlockPos(
                        sampleBlock(random, blockX(fold)),
                        sampleHeight(random),
                        sampleBlock(random, blockZ(fold))), fold);
            }
        }
    }

    @Test
    void theBoundsAndWholeWidthsOutKeyLikeAnyOtherPosition() {
        for (WorldFold fold : FOLDS) {
            for (int x : edges(blockX(fold))) {
                for (int z : edges(blockZ(fold))) {
                    for (int y : HEIGHTS) {
                        checkKey(new BlockPos(x, y, z), fold);
                    }
                }
            }
        }
    }

    @Test
    void thePackedNearestCopyAgreesWithThePositionForm() {
        Random random = new Random(SEED);
        for (WorldFold fold : FOLDS) {
            for (int i = 0; i < SAMPLES; i++) {
                BlockPos ref = new BlockPos(sampleBlock(random, blockX(fold)), sampleHeight(random),
                        sampleBlock(random, blockZ(fold)));
                BlockPos target = new BlockPos(sampleBlock(random, blockX(fold)), sampleHeight(random),
                        sampleBlock(random, blockZ(fold)));
                assertEquals(fold.nearestCopy(ref, target).asLong(),
                        fold.nearestCopyBlockNode(ref.asLong(), target.asLong()),
                        () -> "the packed nearest copy of " + target + " around " + ref + " disagrees "
                                + in(fold));
            }
        }
    }

    private static void checkKey(BlockPos pos, WorldFold fold) {
        BlockPos folded = fold.fold(pos);
        long section = SectionPos.asLong(folded);

        assertFalse(chunkX(fold).isOver(SectionPos.x(section)),
                () -> "wrap(" + pos + ") named a section past the X bounds " + in(fold));
        assertFalse(chunkZ(fold).isOver(SectionPos.z(section)),
                () -> "wrap(" + pos + ") named a section past the Z bounds " + in(fold));
        assertEquals(SectionPos.sectionRelativePos(pos), SectionPos.sectionRelativePos(folded),
                () -> "wrap(" + pos + ") moved inside its section " + in(fold));
    }
}
