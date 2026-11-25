package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.core.feature.RandomStateFeature;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

@Timeout(60)
class RandomStateFeaturesTest {
    private static final long SEED = 7L;

    private static final int WIDTH_BLOCKS = 1024;

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void aFeatureOfTheRouterBuildRunsOnceOnTheStateItBuilds() {
        WorldFold fold = ClimateScanFixture.torusOfWidth(WIDTH_BLOCKS);
        Counting feature = new Counting();

        RandomState state = ClimateScanFixture.randomState(overworld(), fold, SEED, List.of(feature));

        assertEquals(1, feature.runs.get());
        assertSame(state, feature.lastState);
        assertSame(fold, feature.lastFold);
    }

    @Test
    void aFeatureOfOneRouterBuildDoesNotRunInTheNext() {
        WorldFold fold = ClimateScanFixture.torusOfWidth(WIDTH_BLOCKS);
        Counting feature = new Counting();
        ClimateScanFixture.randomState(overworld(), fold, SEED, List.of(feature));

        ClimateScanFixture.randomState(overworld(), fold, SEED);

        assertEquals(1, feature.runs.get());
    }

    private static NoiseGeneratorSettings overworld() {
        return ClimateScanFixture.settingsOf(ClimateScanFixture.TYPES.getFirst());
    }

    private static final class Counting implements RandomStateFeature {
        private final AtomicInteger runs = new AtomicInteger();
        private volatile RandomState lastState;
        private volatile WorldFold lastFold;

        @Override
        public String getKey() {
            return "random_state_features_test";
        }

        @Override
        public void atRandomState(RandomState randomState, WorldFold fold, int seaLevel) {
            this.runs.incrementAndGet();
            this.lastState = randomState;
            this.lastFold = fold;
        }
    }
}
