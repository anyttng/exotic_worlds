package com.exoticworlds.engine.noise;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.mixin.ChunkGeneratorAccessor;
import com.exoticworlds.mixin.NoiseBasedChunkGeneratorAccessor;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

final class LapTwinChunks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int QUARTS_PER_CHUNK = QuartPos.fromBlock(SectionPos.SECTION_SIZE);

    record Inputs(ServerLevel level, NoiseBasedChunkGenerator generator, RandomState randomState,
            BiomeManager biomeManager, WorldFold fold, SeamCauseSearch.Lap lap) {
    }

    record Reading(boolean read, List<SeamVerdict.Break> breaks) {
        private static final Reading UNREAD = new Reading(false, List.of());
    }

    private static final class Twin implements AutoCloseable {
        private final Inputs inputs;
        private final ProtoChunk chunk;
        private final NoiseChunk noise;

        private Twin(Inputs inputs, ChunkPos pos) {
            this.inputs = inputs;
            this.chunk = new ProtoChunk(pos, UpgradeData.EMPTY, inputs.level(),
                    inputs.level().palettedContainerFactory(), null);
            ((ChunkGeneratorAccessor) inputs.generator()).exotic_worlds$doCreateBiomes(Blender.empty(),
                    inputs.randomState(), this.chunk);
            this.chunk.setPersistedStatus(ChunkStatus.BIOMES);
            NoiseGeneratorSettings settings = inputs.generator().generatorSettings().value();
            NoiseSettings noiseSettings = settings.noiseSettings()
                    .clampToHeightAccessor(this.chunk.getHeightAccessorForGeneration());
            this.noise = new NoiseChunk(inputs.randomState(), null, settings,
                    steps(inputs).exotic_worlds$globalFluidPicker().get(), Blender.empty(),
                    NoiseBasedChunkGeneratorAccessor.exotic_worlds$chunkVolume(this.chunk, noiseSettings));
            steps(inputs).exotic_worlds$doFill(this.noise, this.chunk);
        }

        private void surface(MaterialRule rule) {
            steps(this.inputs).exotic_worlds$buildSurface(this.chunk, this.noise, this.inputs.randomState(),
                    this.inputs.biomeManager(), null, rule);
        }

        private CarverRecording carvers(MaterialRule rule) {
            CarverRecording recording = new CarverRecording();
            CarverRecording.recording(recording, () -> {
                steps(this.inputs).exotic_worlds$generateCarvers(this.chunk, Blender.empty(), this.noise,
                        this.inputs.randomState(), this.inputs.biomeManager(), null, rule);
                return recording;
            });
            return recording;
        }

        @Override
        public void close() {
            this.noise.close();
        }
    }

    static Reading read(Inputs inputs, ChunkPos here) {
        try {
            return GenerationFoldContext.withFold(inputs.fold(), () -> readFolded(inputs, here));
        } catch (RuntimeException unread) {
            LOGGER.warn("[ew/world-loop] twins_unread level={} chunk_x={} chunk_z={} text={}",
                    inputs.level().dimension().identifier(), here.x(), here.z(), unread.toString());
            return Reading.UNREAD;
        }
    }

    private static Reading readFolded(Inputs inputs, ChunkPos herePos) {
        ChunkPos therePos = lapAway(inputs.lap(), herePos);
        MaterialRule rule = inputs.generator().generatorSettings().value().materialRule().value();
        List<SeamVerdict.Break> breaks = new ArrayList<>();
        SeamCauseSearch.Point surfaceAt = null;
        try (Twin here = new Twin(inputs, herePos); Twin there = new Twin(inputs, therePos)) {
            if (!biomesEqual(here.chunk, there.chunk) || differenceOf(here.chunk, there.chunk) != null) {
                return Reading.UNREAD;
            }

            here.surface(rule);
            there.surface(rule);
            surfaceAt = differenceOf(here.chunk, there.chunk);
            SeamCause carvers = CarverCauseSearch.causeOf(here.carvers(rule).carved(), there.carvers(rule).carved(),
                    inputs.level().registryAccess().lookupOrThrow(Registries.CARVER));
            if (carvers != null) {
                breaks.add(new SeamVerdict.Break(SeamField.CARVERS.id(), carvers));
            }
        }

        if (surfaceAt != null) {
            breaks.addFirst(new SeamVerdict.Break(SeamField.SURFACE.id(), surfaceCause(inputs, herePos, rule,
                    surfaceAt)));
        }

        return new Reading(true, breaks);
    }

    private static SeamCause surfaceCause(Inputs inputs, ChunkPos herePos, MaterialRule rule,
            SeamCauseSearch.Point at) {
        MaterialCauseSearch.Recorder hereRecorder = new MaterialCauseSearch.Recorder(rule, at);
        MaterialCauseSearch.Recorder thereRecorder = new MaterialCauseSearch.Recorder(rule, inputs.lap().away(at));
        try (Twin here = new Twin(inputs, herePos); Twin there = new Twin(inputs, lapAway(inputs.lap(), herePos))) {
            here.surface(hereRecorder);
            there.surface(thereRecorder);
        }

        return MaterialCauseSearch.causeOf(rule, hereRecorder.answers(), thereRecorder.answers());
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
                for (int y = here.getMaxY(); y >= here.getMinY(); y--) {
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

    private static boolean biomesEqual(ProtoChunk here, ProtoChunk there) {
        int hereX = QuartPos.fromBlock(here.getPos().getMinBlockX());
        int hereZ = QuartPos.fromBlock(here.getPos().getMinBlockZ());
        int thereX = QuartPos.fromBlock(there.getPos().getMinBlockX());
        int thereZ = QuartPos.fromBlock(there.getPos().getMinBlockZ());
        int minY = QuartPos.fromBlock(here.getMinY());
        int maxY = QuartPos.fromBlock(here.getMaxY());
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
        return ChunkPos.containing(new BlockPos(away.x(), away.y(), away.z()));
    }

    private static NoiseBasedChunkGeneratorAccessor steps(Inputs inputs) {
        return (NoiseBasedChunkGeneratorAccessor) inputs.generator();
    }

    private LapTwinChunks() {
    }
}
