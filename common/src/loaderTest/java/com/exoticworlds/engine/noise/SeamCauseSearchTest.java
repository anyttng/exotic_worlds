package com.exoticworlds.engine.noise;

import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.biomeSource;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;

class SeamCauseSearchTest {
    private static final int WIDTH_BLOCKS = 512;
    private static final WorldFold FOLD = torusOfWidth(WIDTH_BLOCKS);
    private static final ResourceLocation MULTI_NOISE = ResourceLocation.withDefaultNamespace("multi_noise");
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
        assertNull(node.holder());
        assertNull(node.namespace());
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
    void aBrokenBiomeFollowsTheFirstBrokenClimateField() {
        Map<SeamField, List<SeamCauseSearch.Point>> broken = new LinkedHashMap<>();
        broken.put(SeamField.EROSION, POINTS);
        broken.put(SeamField.BIOME, POINTS);

        List<SeamVerdict.Break> breaks = SeamCauseSearch.causesOf(broken, LAP, overworld(), FOLD, overworldBiomes());

        assertEquals(new SeamVerdict.Break(SeamField.BIOME.id(), new SeamCause.Follows(SeamField.EROSION.id())),
                breaks.get(1));
    }

    @Test
    void aBrokenBiomeUnderAHoldingClimateNamesTheBiomeSource() {
        List<SeamVerdict.Break> breaks = SeamCauseSearch.causesOf(Map.of(SeamField.BIOME, POINTS), LAP,
                overworld(), FOLD, overworldBiomes());

        assertEquals(List.of(new SeamVerdict.Break(SeamField.BIOME.id(),
                new SeamCause.Biomes(MULTI_NOISE.toString(), ResourceLocation.DEFAULT_NAMESPACE))), breaks);
    }

    private static RandomState overworld() {
        return randomState(ClimateScanFixture.TYPES.getFirst(), FOLD, SEED_BASE);
    }

    private static BiomeSource overworldBiomes() {
        return biomeSource(ClimateScanFixture.TYPES.getFirst());
    }
}
