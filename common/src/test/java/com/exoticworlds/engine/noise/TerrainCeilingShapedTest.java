package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

class TerrainCeilingShapedTest {
    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(64)));

    private static NoiseGeneratorSettings overworld;

    @BeforeAll
    static void loadOverworldSettings() {
        overworld = VanillaRegistries.createLookup().lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
    }

    @Test
    void anUnshapedGeneratorKeepsItsSettings() {
        assertSame(overworld, TerrainCeiling.shaped(null, overworld));
    }

    @Test
    void aShapedGeneratorTakesTheCeilingedRouter() {
        NoiseGeneratorSettings shaped = TerrainCeiling.shaped(TORUS, overworld);

        assertNotSame(overworld, shaped);
        assertNotSame(overworld.noiseRouter(), shaped.noiseRouter());
    }
}
