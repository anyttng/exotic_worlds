package com.exoticworlds.engine.noise.check;

import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.biomeSource;
import static com.exoticworlds.engine.noise.ClimateScanFixture.cylinderOfWidth;
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

import com.exoticworlds.accessors.InterpretedDensity;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.ClimateScanFixture;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;

class SeamCauseSearchTest {
    private static final int WIDTH_BLOCKS = 512;
    private static final WorldFold FOLD = torusOfWidth(WIDTH_BLOCKS);
    private static final Identifier MULTI_NOISE = Identifier.withDefaultNamespace("multi_noise");
    private static final double RAW_X_SCALE = 1024.0;
    private static final int SKEW_CHUNKS = 5;
    private static final int SKEW_BLOCKS = SectionPos.sectionToBlockCoord(SKEW_CHUNKS);
    private static final SeamCauseSearch.Lap LAP = new SeamCauseSearch.Lap(Direction.Axis.X, WIDTH_BLOCKS, 0);
    private static final SeamCauseSearch.Lap Z_LAP = new SeamCauseSearch.Lap(Direction.Axis.Z, 0, WIDTH_BLOCKS);
    private static final SeamCauseSearch.Lap SKEWED_Z_LAP =
            new SeamCauseSearch.Lap(Direction.Axis.Z, SKEW_BLOCKS, WIDTH_BLOCKS);
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

    private record Compiled(DensityFunction compiled, DensityFunction interpreted)
            implements DensityFunction.SimpleFunction, InterpretedDensity {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return this.compiled.compute(context);
        }

        @Override
        public DensityFunction exotic_worlds$interpreted() {
            return this.interpreted;
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
    void aCompiledNodeIsSearchedThroughTheTreeItCompiled() {
        RandomState state = overworld();
        DensityFunction interpreted = DensityFunctions.add(state.router().erosion(), new RawX());

        SeamCause.Node node = assertInstanceOf(SeamCause.Node.class,
                SeamCauseSearch.densityCause(new Compiled(interpreted, interpreted), POINTS, LAP, FOLD));

        assertEquals(RawX.class.getName(), node.type());
    }

    @Test
    void aBreakOnlyTheCompiledNodeShowsNamesTheCompiledNode() {
        RandomState state = overworld();

        SeamCause.Node node = assertInstanceOf(SeamCause.Node.class,
                SeamCauseSearch.densityCause(new Compiled(new RawX(), state.router().erosion()), POINTS, LAP,
                        FOLD));

        assertEquals(Compiled.class.getName(), node.type());
    }

    @Test
    void aTorusHasALapOnEachAxis() {
        assertEquals(List.of(LAP, Z_LAP), SeamCauseSearch.Lap.of(torusOfWidth(WIDTH_BLOCKS).blockLattice()));
    }

    @Test
    void aCylinderHasItsOneLap() {
        assertEquals(List.of(LAP), SeamCauseSearch.Lap.of(cylinderOfWidth(WIDTH_BLOCKS).blockLattice()));
    }

    @Test
    void theZLapOfASkewedTorusMovesXByTheSkew() {
        assertEquals(List.of(LAP, SKEWED_Z_LAP), SeamCauseSearch.Lap.of(skewedTorus().blockLattice()));
    }

    @Test
    void aNodeThatReadsTheRawXHasNoCauseOnTheZLap() {
        RandomState state = overworld();
        DensityFunction root = DensityFunctions.add(state.router().erosion(), new RawX());

        assertInstanceOf(SeamCause.NotFound.class, SeamCauseSearch.densityCause(root, POINTS, Z_LAP, FOLD));
    }

    @Test
    void aSkewedFieldRepeatsOnItsOwnZLapAndNotOnThePlainOne() {
        WorldFold skewed = skewedTorus();
        RandomState state = randomState(ClimateScanFixture.TYPES.getFirst(), skewed, SEED_BASE);
        DensityFunction erosion = state.router().erosion();

        assertInstanceOf(SeamCause.NotFound.class,
                SeamCauseSearch.densityCause(erosion, POINTS, SKEWED_Z_LAP, skewed));
        assertInstanceOf(SeamCause.Node.class, SeamCauseSearch.densityCause(erosion, POINTS, Z_LAP, skewed));
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
                new SeamCause.Biomes(MULTI_NOISE.toString(), Identifier.DEFAULT_NAMESPACE))), breaks);
    }

    private static RandomState overworld() {
        return randomState(ClimateScanFixture.TYPES.getFirst(), FOLD, SEED_BASE);
    }

    private static WorldFold skewedTorus() {
        return new DeckGroupFold(
                FlatShape.latticeTorus(WorldLoopBounds.ofWidth(SectionPos.blockToSectionCoord(WIDTH_BLOCKS)),
                        SKEW_CHUNKS),
                GenerationOptions.DEFAULT);
    }

    private static BiomeSource overworldBiomes() {
        return biomeSource(ClimateScanFixture.TYPES.getFirst());
    }
}
