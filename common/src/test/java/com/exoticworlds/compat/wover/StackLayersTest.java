package com.exoticworlds.compat.wover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker;
import org.betterx.wover.generator.api.map.BiomeChunk;
import org.betterx.wover.generator.api.map.BiomeMap;
import org.betterx.wover.util.function.TriConsumer;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

class StackLayersTest {
    private static final float SCALE = 32.0F;

    private static final int PALETTE = 4;

    private static final int SEED = 0x1460;

    private static final LapPicker<Integer> PICKER = new LapPicker<>(
            random -> random.nextInt(PALETTE), (biome, random) -> biome);

    private static final WorldFold FOLD =
            WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-64, 64, -48, 48)));

    @Test
    void aStackOfOurMapsBindsEveryLayerAndReportsNothing() {
        List<Class<?>> reports = new ArrayList<>();
        StackLayers<Integer> stackLayers = new StackLayers<>(reports::add);
        OurMap lower = new OurMap();
        OurMap upper = new OurMap();

        List<LapMap<Integer>> layers = stackLayers.of(new BiomeMap[] {lower, upper}, FOLD);

        assertEquals(List.of(lower.lapMap, upper.lapMap), layers);
        assertTrue(reports.isEmpty());
    }

    @Test
    void aForeignMapLeavesTheStackUnboundAndIsReportedOncePerStack() {
        List<Class<?>> reports = new ArrayList<>();
        StackLayers<Integer> stackLayers = new StackLayers<>(reports::add);
        BiomeMap[] maps = {new OurMap(), new ForeignMap()};

        assertTrue(stackLayers.of(maps, FOLD).isEmpty());
        assertTrue(stackLayers.of(maps, FOLD).isEmpty());

        assertEquals(List.of(ForeignMap.class), reports);
    }

    private abstract static class StubMap implements BiomeMap {
        @Override
        public void setChunkProcessor(TriConsumer<Integer, Integer, Integer> processor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BiomeChunk getChunk(int cx, int cz, boolean update) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WoverBiomePicker.PickableBiome getBiome(double x, double y, double z) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearCache() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class ForeignMap extends StubMap {
    }

    private static final class OurMap extends StubMap implements LapMapHolder<Integer> {
        private final LapMap<Integer> lapMap =
                new HexLapMap<>(FOLD, SCALE, ClimateScaleCompression.NO_COMPRESSION, SEED, PICKER);

        @Override
        public LapMap<Integer> exotic_worlds$lapMap(WorldFold fold) {
            return this.lapMap;
        }
    }
}
