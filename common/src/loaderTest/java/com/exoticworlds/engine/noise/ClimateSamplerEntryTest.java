package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.TYPES;
import static com.exoticworlds.engine.noise.ClimateScanFixture.cylinderOfWidth;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.accessors.FoldedClimateSampler;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

class ClimateSamplerEntryTest {
    private static final int WIDTH_BLOCKS = 512;
    private static final int SKEW_CHUNKS = 8;
    private static final int SKEW_BLOCKS = SectionPos.sectionToBlockCoord(SKEW_CHUNKS);

    private static final BlockPos CANONICAL = new BlockPos(40, 64, 72);

    private static final class Recorder implements DensitySampler {
        private BlockPos received = BlockPos.ZERO;

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            this.received = new BlockPos(blockX, blockY, blockZ);
            return 0.0F;
        }

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
        }
    }

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void aLoopedRandomStateStampsItsFoldOnTheSampler() {
        WorldFold fold = torusOfWidth(WIDTH_BLOCKS);
        Climate.Sampler sampler = randomState(TYPES.getFirst(), fold, SEED_BASE)
                .createClimateSampler(SamplerContext.EMPTY_UNCACHED);

        assertSame(fold, foldOf(sampler));
    }

    @Test
    void anUnboundedRandomStateLeavesTheSamplerUnfolded() {
        Climate.Sampler sampler = randomState(TYPES.getFirst(), WorldFolds.NOOP, SEED_BASE)
                .createClimateSampler(SamplerContext.EMPTY_UNCACHED);

        assertSame(WorldFolds.NOOP, foldOf(sampler));
    }

    @Test
    void aCopyOnATorusReachesTheMethodAsTheCanonicalQuart() {
        WorldFold fold = torusOfWidth(WIDTH_BLOCKS);

        assertEquals(CANONICAL, receivedAt(fold, CANONICAL.offset(WIDTH_BLOCKS, 0, 0)), "a copy one lap away on X");
        assertEquals(CANONICAL, receivedAt(fold, CANONICAL.offset(0, 0, -WIDTH_BLOCKS)), "a copy one lap away on Z");
    }

    @Test
    void aCopyAcrossASkewedZEdgeReachesTheMethodAsTheCanonicalQuart() {
        WorldFold fold = WorldFolds.of(
                FlatShape.latticeTorus(WorldLoopBounds.ofWidth(WIDTH_BLOCKS / 16), SKEW_CHUNKS));

        assertEquals(CANONICAL, receivedAt(fold, CANONICAL.offset(SKEW_BLOCKS, 0, WIDTH_BLOCKS)),
                "a copy one lattice lap away across the Z edge");
    }

    @Test
    void aCylinderFoldsItsLoopedAxisAlone() {
        WorldFold fold = cylinderOfWidth(WIDTH_BLOCKS);
        BlockPos far = CANONICAL.offset(WIDTH_BLOCKS, 0, WIDTH_BLOCKS);

        assertEquals(CANONICAL.offset(0, 0, WIDTH_BLOCKS), receivedAt(fold, far));
    }

    @Test
    void anUnstampedSamplerPassesTheCallersQuartUnchanged() {
        BlockPos far = CANONICAL.offset(WIDTH_BLOCKS, 0, -WIDTH_BLOCKS);
        Recorder recorder = new Recorder();
        Climate.Sampler sampler = recordingSampler(recorder);

        sampler.sample(QuartPos.fromBlock(far.getX()), QuartPos.fromBlock(far.getY()), QuartPos.fromBlock(far.getZ()));

        assertEquals(far, recorder.received);
    }

    private static BlockPos receivedAt(WorldFold fold, BlockPos asked) {
        Recorder recorder = new Recorder();
        Climate.Sampler sampler = recordingSampler(recorder);
        ((FoldedClimateSampler) (Object) sampler).toroidal$fold(fold);

        sampler.sample(QuartPos.fromBlock(asked.getX()), QuartPos.fromBlock(asked.getY()),
                QuartPos.fromBlock(asked.getZ()));
        return recorder.received;
    }

    private static Climate.Sampler recordingSampler(Recorder recorder) {
        DensitySampler.Bound bound = new DensitySampler.Bound(recorder, SamplerContext.EMPTY_UNCACHED);
        return new Climate.Sampler(bound, bound, bound, bound, bound, bound);
    }

    private static WorldFold foldOf(Climate.Sampler sampler) {
        return ((FoldedClimateSampler) (Object) sampler).toroidal$fold();
    }
}
