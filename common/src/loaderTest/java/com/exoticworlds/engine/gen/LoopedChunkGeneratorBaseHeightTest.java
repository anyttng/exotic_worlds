package com.exoticworlds.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.shape.StartupRegistrations;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;

class LoopedChunkGeneratorBaseHeightTest {
    private static final long SEED_A = 1383L;
    private static final long SEED_B = 90_210L;
    private static final int LAP_CHUNKS = 64;
    private static final int LAP_BLOCKS = LAP_CHUNKS * 16;
    private static final Heightmap.Types TYPE = Heightmap.Types.WORLD_SURFACE_WG;
    private static final List<int[]> COLUMNS = List.of(
            new int[] {8, 8}, new int[] {200, 620}, new int[] {410, 90}, new int[] {700, 333},
            new int[] {950, 870}, new int[] {123, 456});

    private static HolderLookup.Provider holders;
    private static Holder<NoiseGeneratorSettings> overworldSettings;
    private static LevelHeightAccessor heightAccessor;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        StartupRegistrations.options();
        StartupRegistrations.generationHooks();
        holders = VanillaRegistries.createWorldLookup();
        overworldSettings = holders.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        NoiseSettings noise = overworldSettings.value().noiseSettings();
        heightAccessor = LevelHeightAccessor.create(noise.minY(), noise.height());
    }

    @Test
    void eachStateReadsItsOwnHeightSeedAFirst() {
        assertEachStateReadsItsOwnHeight(SEED_A, SEED_B);
    }

    @Test
    void eachStateReadsItsOwnHeightSeedBFirst() {
        assertEachStateReadsItsOwnHeight(SEED_B, SEED_A);
    }

    @Test
    void aStateWithoutAFoldReadsEachLapItsOwnHeight() {
        LoopedChunkGenerator shared = generator();
        RandomState unfolded = unfoldedState(SEED_A);
        int differing = 0;
        for (int[] column : COLUMNS) {
            int here = shared.getBaseHeight(column[0], column[1], TYPE, heightAccessor, unfolded);
            int lapAway = shared.getBaseHeight(column[0] + LAP_BLOCKS, column[1], TYPE, heightAccessor, unfolded);

            assertEquals(freshHeight(column[0], column[1], unfoldedState(SEED_A)), here,
                    "column " + column[0] + ", " + column[1]);
            assertEquals(freshHeight(column[0] + LAP_BLOCKS, column[1], unfoldedState(SEED_A)), lapAway,
                    "column " + (column[0] + LAP_BLOCKS) + ", " + column[1]);
            differing += here == lapAway ? 0 : 1;
        }

        assertNotEquals(0, differing, "no column of the unbounded field differs from its copy a lap away");
    }

    private static void assertEachStateReadsItsOwnHeight(long readFirst, long readSecond) {
        int[] firstExpected = freshHeights(readFirst);
        int[] secondExpected = freshHeights(readSecond);

        LoopedChunkGenerator shared = generator();
        RandomState first = foldedState(shared, readFirst);
        RandomState second = foldedState(shared, readSecond);
        int differing = 0;
        for (int i = 0; i < COLUMNS.size(); i++) {
            int[] column = COLUMNS.get(i);
            int firstHeight = shared.getBaseHeight(column[0], column[1], TYPE, heightAccessor, first);
            int secondHeight = shared.getBaseHeight(column[0], column[1], TYPE, heightAccessor, second);

            assertEquals(firstExpected[i], firstHeight, "seed " + readFirst + " at " + column[0] + ", " + column[1]);
            assertEquals(secondExpected[i], secondHeight, "seed " + readSecond + " at " + column[0] + ", " + column[1]);
            differing += firstExpected[i] == secondExpected[i] ? 0 : 1;
        }

        assertNotEquals(0, differing, "the two seeds give the same height at every column");
    }

    private static int[] freshHeights(long seed) {
        LoopedChunkGenerator fresh = generator();
        RandomState state = foldedState(fresh, seed);
        int[] heights = new int[COLUMNS.size()];
        for (int i = 0; i < COLUMNS.size(); i++) {
            heights[i] = fresh.getBaseHeight(COLUMNS.get(i)[0], COLUMNS.get(i)[1], TYPE, heightAccessor, state);
        }

        return heights;
    }

    private static int freshHeight(int x, int z, RandomState state) {
        return generator().getBaseHeight(x, z, TYPE, heightAccessor, state);
    }

    private static LoopedChunkGenerator generator() {
        return new LoopedChunkGenerator(
                new FixedBiomeSource(holders.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                overworldSettings,
                new CarriedShape(ShapeFixtures.torus(WorldLoopBounds.ofWidth(LAP_CHUNKS)), GenerationOptions.DEFAULT));
    }

    private static RandomState foldedState(LoopedChunkGenerator generator, long seed) {
        return GenerationFoldContext.withRouterBuild(generator.carriedShape().fold(),
                () -> RandomState.create(holders.lookupOrThrow(Registries.NOISE), seed, overworldSettings.value()));
    }

    private static RandomState unfoldedState(long seed) {
        return RandomState.create(holders.lookupOrThrow(Registries.NOISE), seed, overworldSettings.value());
    }
}
