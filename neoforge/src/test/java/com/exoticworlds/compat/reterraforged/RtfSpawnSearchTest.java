package com.exoticworlds.compat.reterraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.gen.LoopedChunkGenerator;
import com.exoticworlds.engine.gen.SpawnSearch;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.GenerationHookSetup;
import com.exoticworlds.shape.WorldOptionSetup;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.data.worldgen.preset.settings.SpawnType;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.world.worldgen.biome.RTFClimateSampler;

class RtfSpawnSearchTest {
    private static final long SEED = 1383L;
    private static final int WIDE_CHUNKS = 1024;
    private static final SpawnPoint FAR_POINT = new SpawnPoint(300, -300);
    private static final BlockPos SEARCH_CENTER = new BlockPos(-3000, 0, 2000);
    private static final BlockPos USER_SPAWN = new BlockPos(777, 0, -333);
    private static final int SEARCH_REACH_BLOCKS = 2560;

    private static HolderLookup.Provider holders;
    private static Holder<NoiseGeneratorSettings> overworldSettings;

    private SpawnType spawnType;
    private int spawnX;
    private int spawnZ;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        WorldOptionSetup.registerAll(false);
        GenerationHookSetup.registerAll();
        holders = VanillaRegistries.createLookup();
        overworldSettings = holders.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
    }

    @BeforeEach
    void keepReTerraForgedSettings() {
        this.spawnType = WorldSettings.Properties.spawnType;
        this.spawnX = WorldSettings.Properties.spawnX;
        this.spawnZ = WorldSettings.Properties.spawnZ;
    }

    @AfterEach
    void restoreReTerraForgedSettings() {
        WorldSettings.Properties.spawnType = this.spawnType;
        WorldSettings.Properties.spawnX = this.spawnX;
        WorldSettings.Properties.spawnZ = this.spawnZ;
    }

    @Test
    void theSearchStartsAtTheSpawnPoint() {
        WorldSettings.Properties.spawnType = SpawnType.CONTINENT_CENTER;

        BlockPos spawn = search(FAR_POINT);

        assertNear(new BlockPos(FAR_POINT.blockX(), 0, FAR_POINT.blockZ()), spawn);
    }

    @Test
    void aWorldWithoutASpawnPointSearchesFromReTerraForgedsCenter() {
        WorldSettings.Properties.spawnType = SpawnType.CONTINENT_CENTER;

        BlockPos spawn = search(SpawnPoint.DEFAULT);

        assertNear(SEARCH_CENTER, spawn);
    }

    @Test
    void theWorldOriginTypeSpawnsAtTheSpawnPoint() {
        WorldSettings.Properties.spawnType = SpawnType.WORLD_ORIGIN;

        assertEquals(new BlockPos(FAR_POINT.blockX(), 0, FAR_POINT.blockZ()), search(FAR_POINT));
    }

    @Test
    void theUserSelectedTypeKeepsReTerraForgedsCoordinates() {
        WorldSettings.Properties.spawnType = SpawnType.USER_SELECTED;
        WorldSettings.Properties.spawnX = USER_SPAWN.getX();
        WorldSettings.Properties.spawnZ = USER_SPAWN.getZ();

        assertEquals(USER_SPAWN, search(FAR_POINT));
    }

    private static BlockPos search(SpawnPoint point) {
        LoopedChunkGenerator generator = noiseGenerator(point);
        Climate.Sampler sampler = randomState(generator).sampler();
        ((RTFClimateSampler) (Object) sampler).setSpawnSearchCenter(SEARCH_CENTER);
        return SpawnSearch.from(generator, sampler::findSpawnPosition);
    }

    private static void assertNear(BlockPos expected, BlockPos spawn) {
        assertTrue(Math.abs(spawn.getX() - expected.getX()) <= SEARCH_REACH_BLOCKS
                        && Math.abs(spawn.getZ() - expected.getZ()) <= SEARCH_REACH_BLOCKS,
                "spawn " + spawn + " is not near " + expected);
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
                () -> RandomState.create(overworldSettings.value(), holders.lookupOrThrow(Registries.NOISE), SEED));
    }
}
