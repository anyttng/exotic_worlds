package com.exoticworlds.api.v1.gen;

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

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.option.WorldOptions;
import com.exoticworlds.bridge.v1.geometry.GeometryBridge;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.engine.gen.structure.AddedStructureStarts;
import com.exoticworlds.engine.noise.ClimateScanFixture;
import com.exoticworlds.shape.feature.guaranteedland.GuaranteedLand;
import com.exoticworlds.shape.feature.guaranteednethercomplexes.GuaranteedNetherComplexes;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasureStructure;

class GenerationHooksTest {
    private static final String KEY_PREFIX = "v1_generation_hooks_test_";

    private static final String GEOMETRY_KEY = KEY_PREFIX + "geometry";

    private static final String FIRST_KEY = KEY_PREFIX + "order_a";

    private static final String SECOND_KEY = KEY_PREFIX + "order_b";

    private static final String STARTS_KEY = KEY_PREFIX + "starts";

    private static final String DUPLICATE_KEY = KEY_PREFIX + "duplicate";

    private static final int TORUS_WIDTH_CHUNKS = 32;

    private static final int RING_DISTANCE = 4;

    private static final int RING_SPREAD = 2;

    private static final int RING_COUNT = 3;

    private static final int SECOND_SET_WEIGHT = 2;

    private static final ChunkPos ADDED_CHUNK = new ChunkPos(3, -5);

    private static final List<Seen> SEEN = new CopyOnWriteArrayList<>();

    private static final List<String> ORDER = new CopyOnWriteArrayList<>();

    private static final List<Holder<StructureSet>> ASKED = new CopyOnWriteArrayList<>();

    private static final List<Holder<StructureSet>> TEST_SETS = new CopyOnWriteArrayList<>();

    private record Seen(ToroidalShape shape, GenerationOptions options) {
    }

    @BeforeAll
    static void registerHooks() {
        ClimateScanFixture.bootstrapVanilla();
        GenerationHooks.atRandomState(GEOMETRY_KEY,
                (randomState, shape, options, seaLevel) -> SEEN.add(new Seen(shape, options)));
        GenerationHooks.atRandomState(SECOND_KEY, (randomState, shape, options, seaLevel) -> ORDER.add(SECOND_KEY));
        GenerationHooks.atRandomState(FIRST_KEY, (randomState, shape, options, seaLevel) -> ORDER.add(FIRST_KEY));
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
    void aRandomStateHookRunsOncePerRandomStateWithTheFoldsShapeAndOptions() {
        WorldFold fold = torus(ShapeOptions.DEFAULT.with(GuaranteedNetherComplexes.FEATURE.getOption(), true)).getFold();

        randomState(settingsOf(ClimateScanFixture.TYPES.getFirst()), fold, SEED_BASE);

        assertEquals(1, SEEN.size());
        assertSame(GeometryBridge.of(fold), SEEN.get(0).shape());
        assertEquals(true, SEEN.get(0).options().get(registered(GuaranteedNetherComplexes.KEY)));
        assertEquals(false, SEEN.get(0).options().get(registered(GuaranteedLand.KEY)));
    }

    @Test
    void hooksRunInTheOrderOfTheirKeys() {
        randomState(settingsOf(ClimateScanFixture.TYPES.getFirst()), torus(ShapeOptions.DEFAULT).getFold(), SEED_BASE);

        assertEquals(List.of(FIRST_KEY, SECOND_KEY), ORDER);
    }

    @Test
    void aSecondHookUnderOneKeyIsRefused() {
        GenerationHooks.atRandomState(DUPLICATE_KEY, (randomState, shape, options, seaLevel) -> {
        });
        GenerationHooks.atStructureStarts(DUPLICATE_KEY, set -> List.of());

        assertThrows(IllegalStateException.class,
                () -> GenerationHooks.atRandomState(DUPLICATE_KEY, (randomState, shape, options, seaLevel) -> {
                }));
        assertThrows(IllegalStateException.class,
                () -> GenerationHooks.atStructureStarts(DUPLICATE_KEY, set -> List.of()));
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
        CarriedShape carried = torus(ShapeOptions.DEFAULT);
        RandomState randomState = randomState(settingsOf(ClimateScanFixture.TYPES.getFirst()), carried.getFold(),
                SEED_BASE);
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForFlat(randomState, SEED_BASE, new FixedBiomeSource(plains), Stream.of(first, second));

        List<StructureSet.StructureSelectionEntry> added =
                AddedStructureStarts.of(state, null, carried, Level.OVERWORLD).getEntriesAt(ADDED_CHUNK);

        assertEquals(List.of(first, second), ASKED);
        assertEquals(1, added.size());
        assertSame(structure, added.get(0).structure());
    }

    @SuppressWarnings("unchecked")
    private static WorldOption<Boolean> registered(String key) {
        return (WorldOption<Boolean>) WorldOptions.all().stream()
                .filter(option -> option.key().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private static CarriedShape torus(ShapeOptions options) {
        return new CarriedShape(null, ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_CHUNKS)), options);
    }
}
