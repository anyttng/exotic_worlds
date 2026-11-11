package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.GenerationHookSetup;
import com.exoticworlds.shape.WorldOptionSetup;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

class SpawnSearchTest {
    private static final long SEED = 1383L;
    private static final int WIDE_CHUNKS = 1024;
    private static final SpawnPoint FAR_POINT = new SpawnPoint(300, -300);
    private static final int SEARCH_REACH_BLOCKS = 2560;

    private static HolderLookup.Provider holders;
    private static Holder<NoiseGeneratorSettings> overworldSettings;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        WorldOptionSetup.registerAll(false);
        GenerationHookSetup.registerAll();
        holders = VanillaRegistries.createWorldLookup();
        overworldSettings = holders.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
    }

    @Test
    void theSpawnSearchStartsAtTheSpawnPoint() {
        LoopedChunkGenerator generator = noiseGenerator(FAR_POINT);
        RandomState state = randomState(generator);

        ChunkPos spawn = SpawnSearch.from(generator, () -> generator.getOrigin(state));

        assertTrue(Math.abs(spawn.getMinBlockX() - FAR_POINT.blockX()) <= SEARCH_REACH_BLOCKS
                        && Math.abs(spawn.getMinBlockZ() - FAR_POINT.blockZ()) <= SEARCH_REACH_BLOCKS,
                "spawn chunk " + spawn + " is not near the spawn point " + FAR_POINT);
    }

    @Test
    void theStructureOriginKeepsSearchingFromZero() {
        LoopedChunkGenerator moved = noiseGenerator(FAR_POINT);
        LoopedChunkGenerator unmoved = noiseGenerator(SpawnPoint.DEFAULT);
        RandomState state = randomState(unmoved);

        assertEquals(unmoved.getOrigin(state), moved.getOrigin(state));
    }

    @Test
    void aWorldWithoutASpawnPointSearchesAsVanillaDoes() {
        LoopedChunkGenerator generator = noiseGenerator(SpawnPoint.DEFAULT);
        RandomState state = randomState(generator);

        assertEquals(generator.getOrigin(state), SpawnSearch.from(generator, () -> generator.getOrigin(state)));
    }

    @Test
    void aPointAtChunkZeroIsBoundAndDefaultIsNot() {
        LoopedChunkGenerator atZero = noiseGenerator(new SpawnPoint(0, 0));
        LoopedChunkGenerator unset = noiseGenerator(SpawnPoint.DEFAULT);
        List<SpawnPoint> seen = new ArrayList<>();

        SpawnSearch.from(atZero, () -> {
            seen.add(SpawnSearch.bound());
            return ChunkPos.ZERO;
        });
        SpawnSearch.from(unset, () -> {
            seen.add(SpawnSearch.bound());
            return ChunkPos.ZERO;
        });

        assertEquals(List.of(new SpawnPoint(0, 0), SpawnPoint.DEFAULT), seen);
        assertTrue(seen.get(0).isCustom(), "a chosen chunk 0, 0 is a point like any other");
    }

    @Test
    void aFlatWorldSpawnsAtItsSpawnPointChunk() {
        LoopedFlatChunkGenerator generator = new LoopedFlatChunkGenerator(
                new FlatLevelGeneratorSettings(Optional.empty(),
                        holders.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS), List.of()),
                new CarriedShape(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDE_CHUNKS)),
                        GenerationOptions.DEFAULT.with(SpawnPoint.OPTION, FAR_POINT)));
        RandomState state = randomState(noiseGenerator(SpawnPoint.DEFAULT));

        assertEquals(new ChunkPos(FAR_POINT.xChunk(), FAR_POINT.zChunk()),
                SpawnSearch.from(generator, () -> generator.getOrigin(state)));
    }

    private static LoopedChunkGenerator noiseGenerator(SpawnPoint point) {
        return new LoopedChunkGenerator(
                new FixedBiomeSource(holders.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                overworldSettings,
                new CarriedShape(ShapeFixtures.torus(WorldLoopBounds.ofWidth(WIDE_CHUNKS)),
                        GenerationOptions.DEFAULT.with(SpawnPoint.OPTION, point)));
    }

    private static RandomState randomState(LoopedChunkGenerator generator) {
        return GenerationFoldContext.withRouterBuild(generator.carriedShape().fold(),
                () -> RandomState.create(holders.lookupOrThrow(Registries.NOISE), SEED, overworldSettings.value()));
    }
}
