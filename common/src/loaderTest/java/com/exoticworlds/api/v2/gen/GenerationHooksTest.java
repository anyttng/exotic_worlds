package com.exoticworlds.api.v2.gen;

import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.settingsOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v2.geometry.WorldGeometry;
import com.exoticworlds.api.v2.option.ShapeOption;
import com.exoticworlds.api.v2.option.ShapeOptions;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.OptionValues;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.gen.structure.AddedStructureStarts;
import com.exoticworlds.engine.noise.ClimateScanFixture;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;
import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasureStructure;

class GenerationHooksTest {
    private static final String KEY_PREFIX = "v2_generation_hooks_test_";

    private static final String GEOMETRY_KEY = KEY_PREFIX + "geometry";

    private static final String FIRST_KEY = KEY_PREFIX + "order_a";

    private static final String SECOND_KEY = KEY_PREFIX + "order_b";

    private static final String THIRD_KEY = KEY_PREFIX + "order_c";

    private static final String STARTS_KEY = KEY_PREFIX + "starts";

    private static final String RANDOM_STATE_DUPLICATE_KEY = KEY_PREFIX + "random_state_duplicate";

    private static final String STARTS_DUPLICATE_KEY = KEY_PREFIX + "starts_duplicate";

    private static final int TORUS_WIDTH_CHUNKS = 32;

    private static final int RING_DISTANCE = 4;

    private static final int RING_SPREAD = 2;

    private static final int RING_COUNT = 3;

    private static final int SECOND_SET_WEIGHT = 2;

    private static final ChunkPos ADDED_CHUNK = new ChunkPos(3, -5);

    private static final int ANY_POSITION = 0;

    private static final ShapeOption<Boolean> NETHER_COMPLEXES =
            new ShapeOption<>(GuaranteedNetherComplexes.KEY, ANY_POSITION, Codec.BOOL, false);

    private static final ShapeOption<Boolean> LAND =
            new ShapeOption<>(GuaranteedLand.KEY, ANY_POSITION, Codec.BOOL, false);

    private static final List<Seen> SEEN = new CopyOnWriteArrayList<>();

    private static final List<String> ORDER = new CopyOnWriteArrayList<>();

    private static final List<Holder<StructureSet>> ASKED = new CopyOnWriteArrayList<>();

    private static final List<Holder<StructureSet>> TEST_SETS = new CopyOnWriteArrayList<>();

    private record Seen(WorldGeometry geometry, ShapeOptions options) {
    }

    @BeforeAll
    static void registerHooks() {
        ClimateScanFixture.bootstrapVanilla();
        GenerationHooks.atRandomState(GEOMETRY_KEY,
                (randomState, geometry, options, seaLevel) -> SEEN.add(new Seen(geometry, options)));
        GenerationHooks.atRandomState(THIRD_KEY, (randomState, geometry, options, seaLevel) -> ORDER.add(THIRD_KEY));
        com.exoticworlds.api.v1.gen.GenerationHooks.atRandomState(SECOND_KEY,
                (randomState, shape, options, seaLevel) -> ORDER.add(SECOND_KEY));
        GenerationHooks.atRandomState(FIRST_KEY, (randomState, geometry, options, seaLevel) -> ORDER.add(FIRST_KEY));
        GenerationHooks.atStructureStarts(STARTS_KEY, GenerationHooksTest::starts);
    }

    @BeforeEach
    void forgetEarlierRuns() {
        SEEN.clear();
        ORDER.clear();
        ASKED.clear();
        TEST_SETS.clear();
    }

    private static List<StructureStarts.Added> starts(StructureStarts set) {
        if (!TEST_SETS.contains(set.structureSet())) {
            return List.of();
        }

        ASKED.add(set.structureSet());
        if (set.structureSet() != TEST_SETS.get(0)) {
            return List.of();
        }

        Holder<Structure> structure = set.structureSet().value().structures().get(0).structure();
        return List.of(new StructureStarts.Added(structure, ADDED_CHUNK));
    }

    @Test
    void aRandomStateHookRunsOncePerRandomStateWithTheFoldsGeometryAndOptions() {
        WorldFold fold = torus(OptionValues.DEFAULT.with(GuaranteedNetherComplexes.OPTION, true)).fold();
        randomState(settingsOf(NoiseGeneratorSettings.OVERWORLD), fold, SEED_BASE);

        assertEquals(1, SEEN.size());
        assertSame(GeometryBridge.of(fold), SEEN.get(0).geometry());
        assertEquals(true, SEEN.get(0).options().get(NETHER_COMPLEXES));
        assertEquals(false, SEEN.get(0).options().get(LAND));
    }

    @Test
    void hooksOfBothVersionsRunInOneOrderOfTheirKeys() {
        randomState(settingsOf(NoiseGeneratorSettings.OVERWORLD), torus().fold(), SEED_BASE);

        assertEquals(List.of(FIRST_KEY, SECOND_KEY, THIRD_KEY), ORDER);
    }

    @Test
    void aRandomStateKeyTakenThroughV1IsRefusedThroughV2() {
        com.exoticworlds.api.v1.gen.GenerationHooks.atRandomState(RANDOM_STATE_DUPLICATE_KEY,
                (randomState, shape, options, seaLevel) -> {
                });

        assertThrows(IllegalStateException.class, () -> GenerationHooks.atRandomState(RANDOM_STATE_DUPLICATE_KEY,
                (randomState, geometry, options, seaLevel) -> {
                }));
    }

    @Test
    void aStructureStartsKeyTakenThroughV1IsRefusedThroughV2() {
        com.exoticworlds.api.v1.gen.GenerationHooks.atStructureStarts(STARTS_DUPLICATE_KEY, set -> List.of());

        assertThrows(IllegalStateException.class,
                () -> GenerationHooks.atStructureStarts(STARTS_DUPLICATE_KEY, set -> List.of()));
    }

    @Test
    void aStructureStartsHookIsAskedOncePerSetAndItsStartIsAccepted() {
        Holder<Biome> plains = ClimateScanFixture.biome(Biomes.PLAINS);
        HolderSet<Biome> biomes = HolderSet.direct(plains);
        Holder<Structure> structure =
                Holder.direct(new BuriedTreasureStructure(new Structure.StructureSettings(biomes)));
        StructurePlacement rings = new ConcentricRingsStructurePlacement(RING_DISTANCE, RING_SPREAD, RING_COUNT,
                biomes);
        Holder<StructureSet> first = Holder.direct(new StructureSet(structure, rings));
        Holder<StructureSet> second = Holder.direct(new StructureSet(
                List.of(new StructureSet.StructureSelectionEntry(structure, SECOND_SET_WEIGHT)), rings));
        TEST_SETS.addAll(List.of(first, second));
        CarriedShape carried = torus();
        RandomState randomState = randomState(settingsOf(NoiseGeneratorSettings.OVERWORLD), carried.fold(),
                SEED_BASE);
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForFlat(randomState, SEED_BASE,
                new ChunkPos(0, 0), new FixedBiomeSource(plains), Stream.of(first, second));

        List<StructureSet.StructureSelectionEntry> added =
                AddedStructureStarts.of(state, null, carried).at(ADDED_CHUNK);

        assertEquals(List.of(first, second), ASKED);
        assertEquals(1, added.size());
        assertSame(structure, added.get(0).structure());
    }

    private static CarriedShape torus() {
        return torus(OptionValues.DEFAULT);
    }

    private static CarriedShape torus(OptionValues options) {
        return new CarriedShape(ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_CHUNKS)), options);
    }
}
