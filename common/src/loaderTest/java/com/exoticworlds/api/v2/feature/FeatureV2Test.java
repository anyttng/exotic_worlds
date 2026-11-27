package com.exoticworlds.api.v2.feature;

import static com.exoticworlds.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.exoticworlds.engine.noise.ClimateScanFixture.randomState;
import static com.exoticworlds.engine.noise.ClimateScanFixture.settingsOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v2.geometry.WorldGeometryV2;
import com.exoticworlds.api.v2.shape.DimensionScopeV2;
import com.exoticworlds.api.v2.shape.ShapesV2;
import com.exoticworlds.bridge.v2.feature.FeatureBridge;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.core.feature.CarvedChunkFeature;
import com.exoticworlds.core.feature.ChunkDataFeature;
import com.exoticworlds.core.feature.GenerationFeature;
import com.exoticworlds.core.feature.LevelFeature;
import com.exoticworlds.core.feature.LitChunkFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.core.feature.SpawnFeature;
import com.exoticworlds.core.feature.SpawnMoment;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CarriedShape;
import com.exoticworlds.core.world.CuboidShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.SpawnPoint;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.engine.gen.structure.AddedStructureStarts;
import com.exoticworlds.engine.noise.ClimateScanFixture;
import com.exoticworlds.shape.dimension.DeclaredShapes;
import com.exoticworlds.shape.feature.OptionFeature;
import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasureStructure;

class FeatureV2Test {
    private static final String NAMESPACE = "example_mod";

    private static final Identifier SHAPE = Identifier.fromNamespaceAndPath(NAMESPACE, "feature_v2_test_shape");

    private static final Identifier NETHER_ONLY_SHAPE =
            Identifier.fromNamespaceAndPath(NAMESPACE, "feature_v2_test_nether_only_shape");

    private static final int TORUS_WIDTH_CHUNKS = 32;

    private static final int CUBOID_EDGE_CHUNKS = 16;

    private static final int STORED_DEFAULT = 0;

    private static final int NEW_WORLD_VALUE = 3;

    private static final int CHOSEN_VALUE = 7;

    private static final int RING_DISTANCE = 4;

    private static final int RING_SPREAD = 2;

    private static final int RING_COUNT = 3;

    private static final ChunkPos ADDED_CHUNK = new ChunkPos(3, -5);

    private static final ChunkPos SPAWN_CHUNK = new ChunkPos(11, -4);

    private static final List<Seen> SEEN = new CopyOnWriteArrayList<>();

    private static final List<Holder<StructureSet>> TEST_SETS = new CopyOnWriteArrayList<>();

    private static final Counted COUNTED = new Counted();

    private static final Switch SWITCH = new Switch();

    private static final Plain PLAIN = new Plain("feature_v2_test_plain");

    private static final ChunkWork CHUNK_WORK = new ChunkWork();

    private record Seen(WorldGeometryV2 geometry, ResourceKey<LevelStem> dimension, int value, int seaLevel) {
    }

    @BeforeAll
    static void registerFeatures() {
        ClimateScanFixture.bootstrapVanilla();
        for (FeatureV2 feature : List.of(COUNTED, SWITCH, PLAIN, CHUNK_WORK)) {
            FeaturesV2.register(feature);
            ShapesV2.attach(SHAPE, feature, DimensionScopeV2.ALL);
        }
    }

    @BeforeEach
    void forgetEarlierRuns() {
        SEEN.clear();
        TEST_SETS.clear();
    }

    @Test
    void aRandomStateMomentCarriesTheGeometryTheDimensionAndTheFeaturesOwnValue() {
        CarriedShape carried = torus(ShapeOptions.DEFAULT.with(optionOf(COUNTED), CHOSEN_VALUE));

        randomState(settingsOf(NoiseGeneratorSettings.OVERWORLD), carried.getFold(), SEED_BASE, LevelStem.NETHER,
                ShapeFeatures.activeFeatures(carried, LevelStem.NETHER));

        assertEquals(1, SEEN.size());
        assertSame(GeometryBridge.of(carried.getFold()), SEEN.get(0).geometry());
        assertEquals(LevelStem.NETHER, SEEN.get(0).dimension());
        assertEquals(CHOSEN_VALUE, SEEN.get(0).value());
        assertEquals(settingsOf(NoiseGeneratorSettings.OVERWORLD).seaLevel(), SEEN.get(0).seaLevel());
    }

    @Test
    void aCuboidRunsNoFeatureAttachedToAnotherShape() {
        CarriedShape cuboid = new CarriedShape(DeclaredShapes.CUBOID,
                new CuboidShape(CUBOID_EDGE_CHUNKS, CUBOID_EDGE_CHUNKS, CUBOID_EDGE_CHUNKS),
                ShapeOptions.DEFAULT.with(optionOf(COUNTED), CHOSEN_VALUE));

        randomState(settingsOf(NoiseGeneratorSettings.OVERWORLD), cuboid.getFold(), SEED_BASE, LevelStem.OVERWORLD,
                ShapeFeatures.activeFeatures(cuboid, LevelStem.OVERWORLD));

        assertTrue(SEEN.isEmpty());
    }

    @Test
    void aWorldSavedBeforeTheFeatureReadsItsStoredDefault() {
        CarriedShape carried = torus(ShapeOptions.DEFAULT.with(optionOf(SWITCH), true));

        randomState(settingsOf(NoiseGeneratorSettings.OVERWORLD), carried.getFold(), SEED_BASE,
                List.of(FeatureBridge.internalOf(COUNTED)));

        assertEquals(STORED_DEFAULT, SEEN.get(0).value());
    }

    @Test
    void aFeatureWhoseValueIsOffStaysOutOfTheWorld() {
        CarriedShape off = torus(ShapeOptions.DEFAULT);
        CarriedShape on = torus(ShapeOptions.DEFAULT.with(optionOf(SWITCH), true));

        assertEquals(false, activeKeys(off).contains(SWITCH.getId().toString()));
        assertEquals(true, activeKeys(on).contains(SWITCH.getId().toString()));
        assertEquals(true, activeKeys(off).contains(PLAIN.getId().toString()));
    }

    @Test
    void aStructureStartsMomentIsAskedPerSetAndItsStartIsAccepted() {
        Holder<Biome> plains = ClimateScanFixture.biome(Biomes.PLAINS);
        HolderSet<Biome> biomes = HolderSet.direct(plains);
        Holder<Structure> structure =
                Holder.direct(new BuriedTreasureStructure(new Structure.StructureSettings(biomes)));
        StructurePlacement rings = new ConcentricRingsStructurePlacement(RING_DISTANCE, RING_SPREAD, RING_COUNT,
                biomes);
        Holder<StructureSet> set = Holder.direct(new StructureSet(structure, rings));
        TEST_SETS.add(set);
        CarriedShape carried = torus(ShapeOptions.DEFAULT.with(optionOf(COUNTED), CHOSEN_VALUE));
        RandomState state = randomState(settingsOf(NoiseGeneratorSettings.OVERWORLD), carried.getFold(), SEED_BASE,
                List.of());
        ChunkGeneratorStructureState structures = ChunkGeneratorStructureState.createForFlat(state, SEED_BASE,
                new ChunkPos(0, 0), new FixedBiomeSource(plains), Stream.of(set));

        List<StructureSet.StructureSelectionEntry> added =
                AddedStructureStarts.of(structures, null, carried, Level.OVERWORLD).getEntriesAt(ADDED_CHUNK);

        assertEquals(1, added.size());
        assertSame(structure, added.get(0).structure());
    }

    @Test
    void aSpawnMomentsChunkBecomesTheSpawnPoint() {
        CarriedShape chosen = torus(ShapeOptions.DEFAULT.with(optionOf(COUNTED), CHOSEN_VALUE));
        CarriedShape unchosen = torus(ShapeOptions.DEFAULT);
        SpawnFeature spawn = FeatureBridge.internalOf(COUNTED).asMoment(SpawnFeature.class);

        assertNotNull(spawn);
        assertEquals(new SpawnPoint(SPAWN_CHUNK.x(), SPAWN_CHUNK.z()),
                spawn.spawnPointIn(new SpawnMoment(chosen.getFold(), chosen.getOptions())));
        assertEquals(SpawnPoint.DEFAULT,
                spawn.spawnPointIn(new SpawnMoment(unchosen.getFold(), unchosen.getOptions())));
    }

    @Test
    void aFeatureIsOfferedOnlyAtTheMomentsItImplements() {
        GenerationFeature plain = FeatureBridge.internalOf(PLAIN);
        GenerationFeature chunkWork = FeatureBridge.internalOf(CHUNK_WORK);

        assertNull(plain.asMoment(SpawnFeature.class));
        assertNull(plain.asMoment(LevelFeature.class));
        assertNotNull(chunkWork.asMoment(CarvedChunkFeature.class));
        assertNotNull(chunkWork.asMoment(LitChunkFeature.class));
        assertNull(FeatureBridge.internalOf(COUNTED).asMoment(CarvedChunkFeature.class));
    }

    @Test
    void chunkDataIsSavedUnderTheFeaturesId() {
        ChunkDataFeature data = FeatureBridge.internalOf(CHUNK_WORK).asMoment(ChunkDataFeature.class);

        assertNotNull(data);
        assertEquals(CHUNK_WORK.getId().toString(), data.getDataKey());
        assertEquals(CHUNK_WORK.getId().toString(), data.getKey());
    }

    @Test
    void aSecondFeatureUnderOneIdIsRefused() {
        assertThrows(IllegalStateException.class, () -> FeaturesV2.register(new Plain("feature_v2_test_plain")));
    }

    @Test
    void anUnregisteredFeatureIsNamedWhenItsValueIsAsked() {
        Counted unregistered = new Counted("feature_v2_test_unregistered");

        IllegalStateException refused =
                assertThrows(IllegalStateException.class, () -> FeatureBridge.internalOf(unregistered));

        assertEquals(true, refused.getMessage().contains(unregistered.getId().toString()));
    }

    @Test
    void aFeatureAttachedToTheNamedDimensionsStaysOutOfTheOthers() {
        ShapesV2.attach(NETHER_ONLY_SHAPE, PLAIN, DimensionScopeV2.of(List.of(LevelStem.NETHER)));
        CarriedShape carried = new CarriedShape(NETHER_ONLY_SHAPE,
                ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_CHUNKS)), ShapeOptions.DEFAULT);

        assertEquals(List.of(PLAIN.getId().toString()), ShapeFeatures.activeFeatures(carried, LevelStem.NETHER)
                .stream().map(GenerationFeature::getKey).toList());
        assertEquals(List.of(), ShapeFeatures.activeFeatures(carried, LevelStem.OVERWORLD));
    }

    @Test
    void aFeatureIsAttachedToAShapeOnce() {
        assertThrows(IllegalStateException.class, () -> ShapesV2.attach(SHAPE, PLAIN, DimensionScopeV2.ALL));
    }

    @Test
    void anUnregisteredFeatureIsNotAttached() {
        assertThrows(IllegalStateException.class,
                () -> ShapesV2.attach(SHAPE, new Plain("feature_v2_test_never_registered"), DimensionScopeV2.ALL));
    }

    @Test
    void aScopeNamesAtLeastOneDimension() {
        assertThrows(IllegalArgumentException.class, () -> DimensionScopeV2.of(List.of()));
    }

    private static List<String> activeKeys(CarriedShape carried) {
        return ShapeFeatures.activeFeatures(carried, LevelStem.OVERWORLD).stream()
                .map(GenerationFeature::getKey)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> ShapeOption<T> optionOf(OptionFeatureV2<T> feature) {
        return ((OptionFeature<T>) FeatureBridge.internalOf(feature)).getOption();
    }

    private static CarriedShape torus(ShapeOptions options) {
        return new CarriedShape(SHAPE, ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_CHUNKS)), options);
    }

    private static final class Counted extends OptionFeatureV2<Integer>
            implements RandomStateMomentV2, StructureStartsMomentV2, SpawnMomentV2 {
        Counted() {
            this("feature_v2_test_counted");
        }

        Counted(String path) {
            super(Identifier.fromNamespaceAndPath(NAMESPACE, path), Codec.INT, STORED_DEFAULT, NEW_WORLD_VALUE,
                    "example_mod.feature_v2_test.counted", List.of());
        }

        @Override
        public void atRandomState(RandomStateV2 state) {
            SEEN.add(new Seen(state.getGeometry(), state.getDimension(), state.getValue(this), state.getSeaLevel()));
        }

        @Override
        public List<StructureStartsV2.Added> atStructureStarts(StructureStartsV2 starts) {
            if (!TEST_SETS.contains(starts.getStructureSet()) || starts.getValue(this) != CHOSEN_VALUE) {
                return List.of();
            }

            Holder<Structure> structure = starts.getStructureSet().value().structures().get(0).structure();
            return List.of(new StructureStartsV2.Added(structure, ADDED_CHUNK));
        }

        @Override
        public Optional<ChunkPos> spawnChunkIn(SpawnV2 spawn) {
            return spawn.getValue(this) == CHOSEN_VALUE ? Optional.of(SPAWN_CHUNK) : Optional.empty();
        }
    }

    private static final class Switch extends ToggleFeatureV2 {
        Switch() {
            super(Identifier.fromNamespaceAndPath(NAMESPACE, "feature_v2_test_switch"),
                    "example_mod.feature_v2_test.switch");
        }
    }

    private static final class Plain extends FeatureV2 {
        Plain(String path) {
            super(Identifier.fromNamespaceAndPath(NAMESPACE, path));
        }
    }

    private static final class ChunkWork extends FeatureV2
            implements CarvedChunkMomentV2, LitChunkMomentV2, ChunkDataMomentV2 {
        ChunkWork() {
            super(Identifier.fromNamespaceAndPath(NAMESPACE, "feature_v2_test_chunk_work"));
        }

        @Override
        public boolean isInstalledOn(ServerLevel level) {
            return true;
        }

        @Override
        public void atCarved(ChunkV2 chunk) {
        }

        @Override
        public void atLit(LitChunkV2 chunk) {
        }

        @Override
        public CompoundTag atChunkSave(ChunkSlotV2 slot) {
            return null;
        }

        @Override
        public void atChunkLoad(ChunkV2 chunk, CompoundTag data) {
        }

        @Override
        public void atChunkRelease(ChunkSlotV2 slot) {
        }
    }
}
