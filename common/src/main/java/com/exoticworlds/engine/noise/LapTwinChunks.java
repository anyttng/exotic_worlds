package com.exoticworlds.engine.noise;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.mixin.NoiseBasedChunkGeneratorAccessor;
import com.exoticworlds.mixin.StructureManagerAccessor;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.blending.Blender;

final class LapTwinChunks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int QUARTS_PER_CHUNK = QuartPos.fromBlock(SectionPos.SECTION_SIZE);
    private static final ChunkStep CARVERS_STEP = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.CARVERS);
    private static final int TICKET_LEVEL = 0;
    private static final Set<Heightmap.Types> PRIMED_HEIGHTMAPS = EnumSet.of(Heightmap.Types.MOTION_BLOCKING,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE);

    record Inputs(ServerLevel level, NoiseBasedChunkGenerator generator, RandomState randomState,
            BiomeManager biomeManager, WorldFold fold, SeamCauseSearch.Lap lap) {
    }

    record Reading(boolean read, List<SeamVerdict.Break> breaks) {
        private static final Reading UNREAD = new Reading(false, List.of());
    }

    private static final class Twin {
        private final Inputs inputs;
        private final ProtoChunk chunk;
        private final NoiseChunk noise;

        private Twin(Inputs inputs, ChunkPos pos) {
            this.inputs = inputs;
            this.chunk = biomesOnly(inputs, pos);
            this.noise = noiseOf(inputs, this.chunk);
            NoiseSettings noiseSettings = inputs.generator().generatorSettings().value().noiseSettings()
                    .clampToHeightAccessor(this.chunk.getHeightAccessorForGeneration());
            int cellMinY = Mth.floorDiv(noiseSettings.minY(), noiseSettings.getCellHeight());
            int cellCountY = Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
            steps(inputs).exotic_worlds$doFill(Blender.empty(), inputs.level().structureManager(),
                    inputs.randomState(), this.chunk, cellMinY, cellCountY);
        }

        private void surface(SurfaceRules.RuleSource rule) {
            NoiseGeneratorSettings settings = this.inputs.generator().generatorSettings().value();
            this.inputs.randomState().surfaceSystem().buildSurface(this.inputs.randomState(),
                    this.inputs.biomeManager(), this.inputs.level().registryAccess().registryOrThrow(Registries.BIOME),
                    settings.useLegacyRandomSource(),
                    new WorldGenerationContext(this.inputs.generator(), this.chunk.getHeightAccessorForGeneration()),
                    this.chunk, this.noise, rule);
        }

        private CarverRecording carvers() {
            CarverRecording recording = new CarverRecording();
            WorldGenRegion region = this.region();
            CarverRecording.recording(recording, () -> {
                this.inputs.generator().applyCarvers(region, this.inputs.level().getSeed(),
                        this.inputs.randomState(), this.inputs.biomeManager(),
                        this.inputs.level().structureManager(), this.chunk, GenerationStep.Carving.AIR);
                return recording;
            });
            return recording;
        }

        private FeatureRecording features(ChunkAccess[] grid) {
            Heightmap.primeHeightmaps(this.chunk, PRIMED_HEIGHTMAPS);
            FeatureRecording recording = new FeatureRecording(this.chunk);
            ServerLevel level = this.inputs.level();
            LapTwinRegion region = new LapTwinRegion(level, this.inputs.randomState(),
                    this.inputs.fold().fold(this.chunk.getPos()), grid, recording);
            StructureManager structures = new StructureManager(region, new WorldOptions(level.getSeed(), false, false),
                    ((StructureManagerAccessor) level.structureManager()).exotic_worlds$structureCheck());
            FeatureRecording.recording(recording, () -> {
                this.inputs.generator().applyBiomeDecoration(region, this.chunk, structures);
                return recording;
            });
            return recording;
        }

        private WorldGenRegion region() {
            ChunkPos center = this.chunk.getPos();
            StaticCache2D<GenerationChunkHolder> cache = StaticCache2D.create(center.x, center.z,
                    CARVERS_STEP.getAccumulatedRadiusOf(ChunkStatus.EMPTY),
                    (x, z) -> x == center.x && z == center.z
                            ? new TwinHolder(center, this.chunk)
                            : new TwinHolder(new ChunkPos(x, z), null));
            return new WorldGenRegion(this.inputs.level(), cache, CARVERS_STEP, this.chunk);
        }

        private final class TwinHolder extends GenerationChunkHolder {
            private @Nullable ChunkAccess held;

            private TwinHolder(ChunkPos pos, @Nullable ChunkAccess held) {
                super(pos);
                this.held = held;
            }

            @Override
            public ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status) {
                ChunkAccess chunk = this.held;
                if (chunk == null) {
                    chunk = emptyChunk(Twin.this.inputs, this.pos);
                    this.held = chunk;
                }

                return chunk;
            }
            @Override
            public int getTicketLevel() {
                return TICKET_LEVEL;
            }

            @Override
            public int getQueueLevel() {
                return TICKET_LEVEL;
            }
        }
    }

    static Reading read(Inputs inputs, ChunkPos here) {
        try {
            return GenerationFoldContext.withFold(inputs.fold(), () -> readFolded(inputs, here));
        } catch (RuntimeException unread) {
            LOGGER.warn("[ew/world-loop] twins_unread level={} chunk_x={} chunk_z={} text={}",
                    inputs.level().dimension().location(), here.x, here.z, unread.toString());
            return Reading.UNREAD;
        }
    }

    private static Reading readFolded(Inputs inputs, ChunkPos herePos) {
        ChunkPos therePos = lapAway(inputs.lap(), herePos);
        SurfaceRules.RuleSource rule = inputs.generator().generatorSettings().value().surfaceRule();
        List<SeamVerdict.Break> breaks = new ArrayList<>();
        Twin here = new Twin(inputs, herePos);
        Twin there = new Twin(inputs, therePos);
        if (!biomesEqual(here.chunk, there.chunk) || differenceOf(here.chunk, there.chunk) != null) {
            return Reading.UNREAD;
        }

        here.surface(rule);
        there.surface(rule);
        SeamCauseSearch.Point surfaceAt = differenceOf(here.chunk, there.chunk);
        SeamCause carvers = CarverCauseSearch.causeOf(here.carvers().carved(), there.carvers().carved(),
                inputs.level().registryAccess().registryOrThrow(Registries.CONFIGURED_CARVER));
        if (carvers != null) {
            breaks.add(new SeamVerdict.Break(SeamField.CARVERS.id(), carvers));
        } else if (surfaceAt == null) {
            SeamCause features = featuresCause(inputs, here, there);
            if (features != null) {
                breaks.add(new SeamVerdict.Break(SeamField.FEATURES.id(), features));
            }
        }

        if (surfaceAt != null) {
            breaks.addFirst(new SeamVerdict.Break(SeamField.SURFACE.id(), surfaceCause(inputs, herePos, rule,
                    surfaceAt)));
        }

        return new Reading(true, breaks);
    }

    private static @Nullable SeamCause featuresCause(Inputs inputs, Twin here, Twin there) {
        SeamCauseSearch.Point carvedAt = differenceOf(here.chunk, there.chunk);
        if (carvedAt != null) {
            LOGGER.warn("[ew/world-loop] features_unread level={} chunk_x={} chunk_z={} reason=carved_blocks x={} y={} z={}",
                    inputs.level().dimension().location(), here.chunk.getPos().x, here.chunk.getPos().z,
                    carvedAt.x(), carvedAt.y(), carvedAt.z());
            return null;
        }

        ChunkAccess[] hereGrid = gridAround(inputs, here.chunk);
        ChunkAccess[] thereGrid = gridAround(inputs, there.chunk);
        for (int index = 0; index < hereGrid.length; index++) {
            if (!biomesEqual(hereGrid[index], thereGrid[index])) {
                LOGGER.warn("[ew/world-loop] features_unread level={} chunk_x={} chunk_z={} reason=neighbor_biomes",
                        inputs.level().dimension().location(), here.chunk.getPos().x, here.chunk.getPos().z);
                return null;
            }
        }

        RegistryAccess registries = inputs.level().registryAccess();
        return FeatureCauseSearch.causeOf(here.features(hereGrid), there.features(thereGrid),
                new FeatureCauseSearch.Lookups(registries.registryOrThrow(Registries.PLACED_FEATURE),
                        registries.registryOrThrow(Registries.CONFIGURED_FEATURE)));
    }

    private static ChunkAccess[] gridAround(Inputs inputs, ProtoChunk center) {
        ChunkAccess[] grid = new ChunkAccess[LapTwinRegion.SIDE * LapTwinRegion.SIDE];
        ChunkPos centerPos = center.getPos();
        for (int offsetZ = -LapTwinRegion.REACH; offsetZ <= LapTwinRegion.REACH; offsetZ++) {
            for (int offsetX = -LapTwinRegion.REACH; offsetX <= LapTwinRegion.REACH; offsetX++) {
                grid[(offsetZ + LapTwinRegion.REACH) * LapTwinRegion.SIDE + offsetX + LapTwinRegion.REACH] =
                        offsetX == 0 && offsetZ == 0
                                ? center
                                : biomesOnly(inputs, new ChunkPos(centerPos.x + offsetX, centerPos.z + offsetZ));
            }
        }

        return grid;
    }

    private static ProtoChunk biomesOnly(Inputs inputs, ChunkPos pos) {
        ProtoChunk chunk = emptyChunk(inputs, pos);
        noiseOf(inputs, chunk);
        steps(inputs).exotic_worlds$doCreateBiomes(Blender.empty(), inputs.randomState(),
                inputs.level().structureManager(), chunk);
        chunk.setPersistedStatus(ChunkStatus.BIOMES);
        return chunk;
    }

    private static NoiseChunk noiseOf(Inputs inputs, ProtoChunk chunk) {
        NoiseGeneratorSettings settings = inputs.generator().generatorSettings().value();
        return chunk.getOrCreateNoiseChunk(created -> NoiseChunk.forChunk(created, inputs.randomState(),
                DensityFunctions.BeardifierMarker.INSTANCE, settings,
                steps(inputs).exotic_worlds$globalFluidPicker().get(), Blender.empty()));
    }

    private static SeamCause surfaceCause(Inputs inputs, ChunkPos herePos, SurfaceRules.RuleSource rule,
            SeamCauseSearch.Point at) {
        MaterialCauseSearch.Recorder hereRecorder = new MaterialCauseSearch.Recorder(rule, at);
        MaterialCauseSearch.Recorder thereRecorder = new MaterialCauseSearch.Recorder(rule, inputs.lap().away(at));
        new Twin(inputs, herePos).surface(hereRecorder);
        new Twin(inputs, lapAway(inputs.lap(), herePos)).surface(thereRecorder);
        return MaterialCauseSearch.causeOf(rule, hereRecorder.answers(), thereRecorder.answers());
    }

    private static ProtoChunk emptyChunk(Inputs inputs, ChunkPos pos) {
        return new ProtoChunk(pos, UpgradeData.EMPTY, inputs.level(), inputs.level().registryAccess().registryOrThrow(Registries.BIOME),
                null);
    }

    private static SeamCauseSearch.@Nullable Point differenceOf(ProtoChunk here, ProtoChunk there) {
        BlockPos.MutableBlockPos herePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos therePos = new BlockPos.MutableBlockPos();
        int hereX = here.getPos().getMinBlockX();
        int hereZ = here.getPos().getMinBlockZ();
        int thereX = there.getPos().getMinBlockX();
        int thereZ = there.getPos().getMinBlockZ();
        for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
            for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                for (int y = here.getMaxBuildHeight() - 1; y >= here.getMinBuildHeight(); y--) {
                    BlockState hereState = here.getBlockState(herePos.set(hereX + x, y, hereZ + z));
                    BlockState thereState = there.getBlockState(therePos.set(thereX + x, y, thereZ + z));
                    if (hereState != thereState) {
                        return new SeamCauseSearch.Point(hereX + x, y, hereZ + z);
                    }
                }
            }
        }

        return null;
    }

    private static boolean biomesEqual(ChunkAccess here, ChunkAccess there) {
        int hereX = QuartPos.fromBlock(here.getPos().getMinBlockX());
        int hereZ = QuartPos.fromBlock(here.getPos().getMinBlockZ());
        int thereX = QuartPos.fromBlock(there.getPos().getMinBlockX());
        int thereZ = QuartPos.fromBlock(there.getPos().getMinBlockZ());
        int minY = QuartPos.fromBlock(here.getMinBuildHeight());
        int maxY = QuartPos.fromBlock(here.getMaxBuildHeight() - 1);
        for (int x = 0; x < QUARTS_PER_CHUNK; x++) {
            for (int z = 0; z < QUARTS_PER_CHUNK; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (!here.getNoiseBiome(hereX + x, y, hereZ + z)
                            .equals(there.getNoiseBiome(thereX + x, y, thereZ + z))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static ChunkPos lapAway(SeamCauseSearch.Lap lap, ChunkPos pos) {
        SeamCauseSearch.Point away = lap.away(new SeamCauseSearch.Point(pos.getMinBlockX(), 0, pos.getMinBlockZ()));
        return new ChunkPos(new BlockPos(away.x(), away.y(), away.z()));
    }

    private static NoiseBasedChunkGeneratorAccessor steps(Inputs inputs) {
        return (NoiseBasedChunkGeneratorAccessor) inputs.generator();
    }

    private LapTwinChunks() {
    }
}
