package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.settingsOf;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.ExoticWorlds;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Direction;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

class SeamCauseSearchTest {
    private static final int WIDTH_BLOCKS = 512;
    private static final float RAW_X_SCALE = 1024.0F;
    private static final SeamCauseSearch.Lap LAP = new SeamCauseSearch.Lap(Direction.Axis.X, WIDTH_BLOCKS);
    private static final List<SeamCauseSearch.Point> POINTS = List.of(
            new SeamCauseSearch.Point(0, 64, 0),
            new SeamCauseSearch.Point(100, 64, 137),
            new SeamCauseSearch.Point(255, 64, -211));

    private record RawX() implements DensityFunction {
        @Override
        public DensitySampler compileSampler(DensityFunction.CompileContext context) {
            return new DensitySampler() {
                @Override
                public void sampleVolume(SamplerContext samplerContext, DensityBuffer outputBuffer,
                        DensityVolume volume) {
                    DensitySampler.sampleVolumeNaive(samplerContext, outputBuffer, volume, this);
                }

                @Override
                public float sampleValue(SamplerContext samplerContext, int blockX, int blockY, int blockZ) {
                    return blockX / RAW_X_SCALE;
                }
            };
        }

        @Override
        public DensityFunction rewriteChildren(DfRewriteRule rule) {
            return this;
        }

        @Override
        public Interval range() {
            return Interval.INFINITE;
        }

        @Override
        public @DensityFunction.Axes int domainAxes() {
            return DensityFunction.AXIS_X;
        }

        @Override
        public MapCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException("a test node has no registered type");
        }
    }

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void theNodeThatReadsTheRawCoordinateIsTheCauseUnderAVanillaParent() {
        RandomState state = overworld();
        DensityFunction root = DensityFunctions.add(state.router.erosion(), new RawX());

        SeamCause.Node node = assertInstanceOf(SeamCause.Node.class,
                SeamCauseSearch.densityCause(root, POINTS, LAP, state));

        assertEquals(RawX.class.getName(), node.type());
        assertFalse(node.vanilla());
        assertEquals(List.of(), node.candidates());
    }

    @Test
    void aFieldThatRepeatsOneLapAwayHasNoCause() {
        RandomState state = overworld();

        assertInstanceOf(SeamCause.NotFound.class,
                SeamCauseSearch.densityCause(state.router.erosion(), POINTS, LAP, state));
    }

    @Test
    void candidatesLeaveOurOwnMixinsOutAndNameAnUnmappedMixinByItself() {
        List<AppliedMixins.Applied> applied = List.of(
                new AppliedMixins.Applied("com.exoticworlds.mixin.ExampleMixin", ExoticWorlds.MODID),
                new AppliedMixins.Applied("com.example.mixin.FirstMixin", "example_mod"),
                new AppliedMixins.Applied("com.example.mixin.SecondMixin", "example_mod"),
                new AppliedMixins.Applied("com.loose.mixin.LooseMixin", null));

        assertEquals(List.of("com.loose.mixin.LooseMixin", "example_mod"), AppliedMixins.foreign(applied));
    }

    private static RandomState overworld() {
        NoiseGeneratorSettings settings = settingsOf(NoiseGeneratorSettings.OVERWORLD);
        return randomState(settings, torusOfWidth(WIDTH_BLOCKS), SEED_BASE);
    }
}
