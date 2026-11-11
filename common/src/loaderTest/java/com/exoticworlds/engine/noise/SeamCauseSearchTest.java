package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;

class SeamCauseSearchTest {
    private static final int WIDTH_BLOCKS = 512;
    private static final WorldFold FOLD = torusOfWidth(WIDTH_BLOCKS);
    private static final double RAW_X_SCALE = 1024.0;
    private static final SeamCauseSearch.Lap LAP = new SeamCauseSearch.Lap(Direction.Axis.X, WIDTH_BLOCKS);
    private static final List<SeamCauseSearch.Point> POINTS = List.of(
            new SeamCauseSearch.Point(0, 64, 0),
            new SeamCauseSearch.Point(100, 64, 137),
            new SeamCauseSearch.Point(255, 64, -211));

    private record RawX() implements DensityFunction.SimpleFunction {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return context.blockX() / RAW_X_SCALE;
        }

        @Override
        public double minValue() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
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
        DensityFunction root = DensityFunctions.add(state.router().erosion(), new RawX());

        SeamCause.Node node = assertInstanceOf(SeamCause.Node.class,
                SeamCauseSearch.densityCause(root, POINTS, LAP, FOLD));

        assertEquals(RawX.class.getName(), node.type());
        assertFalse(node.vanilla());
        assertEquals(List.of(), node.candidates());
    }

    @Test
    void aFieldThatRepeatsOneLapAwayHasNoCause() {
        RandomState state = overworld();

        assertInstanceOf(SeamCause.NotFound.class,
                SeamCauseSearch.densityCause(state.router().erosion(), POINTS, LAP, FOLD));
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
        return randomState(ClimateScanFixture.TYPES.getFirst(), FOLD, SEED_BASE);
    }
}
