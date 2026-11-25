package com.exoticworlds.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.core.feature.ChunkDataFeature;
import com.exoticworlds.core.feature.ShapeFeatures;
import com.exoticworlds.migration.FormerNamespace;
import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.structure.Structure;

@Mixin(SerializableChunkData.class)
public class SerializableChunkDataMixin {
    @Unique
    private static final Logger exotic_worlds$LOGGER = LogUtils.getLogger();

    // Vanilla's own literal, restated because the code it lives in is not reachable from here.
    @Unique
    private static final int exotic_worlds$MAX_REFERENCE_DISTANCE_CHUNKS = 8;

    @Unique
    private static final String exotic_worlds$REFERENCES_KEY = "References";

    @Unique
    private static final String exotic_worlds$DATA_KEY_PREFIX = ExoticWorlds.MODID + ":";

    @Unique
    private Map<String, CompoundTag> exotic_worlds$featureData = Map.of();

    @Inject(method = "copyOf", at = @At("RETURN"))
    private static void exotic_worlds$snapshotFeatureData(ServerLevel level, ChunkAccess chunk,
            CallbackInfoReturnable<SerializableChunkData> callback) {
        Map<String, CompoundTag> saved = new HashMap<>();
        for (ChunkDataFeature feature : ShapeFeatures.onLevel(level, ChunkDataFeature.class)) {
            CompoundTag data = feature.getSavedData(level, chunk.getPos());
            if (data != null) {
                saved.put(feature.getDataKey(), data);
            }
        }
        ((SerializableChunkDataMixin) (Object) callback.getReturnValue()).exotic_worlds$featureData = saved;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void exotic_worlds$writeFeatureData(CallbackInfoReturnable<CompoundTag> callback) {
        this.exotic_worlds$featureData.forEach(
                (key, data) -> callback.getReturnValue().put(exotic_worlds$DATA_KEY_PREFIX + key, data));
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private static void exotic_worlds$parseFeatureData(LevelHeightAccessor levelHeight,
            PalettedContainerFactory containerFactory, CompoundTag chunkData,
            CallbackInfoReturnable<@Nullable SerializableChunkData> callback) {
        SerializableChunkData data = callback.getReturnValue();
        if (data == null) {
            return;
        }

        Map<String, CompoundTag> parsed = new HashMap<>();
        for (ChunkDataFeature feature : ShapeFeatures.registeredAt(ChunkDataFeature.class)) {
            FormerNamespace.compound(chunkData, exotic_worlds$DATA_KEY_PREFIX + feature.getDataKey())
                    .ifPresent(stored -> parsed.put(feature.getDataKey(), stored));
        }
        ((SerializableChunkDataMixin) (Object) data).exotic_worlds$featureData = parsed;
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void exotic_worlds$restoreFeatureData(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionInfo,
            ChunkPos pos, CallbackInfoReturnable<ProtoChunk> callback) {
        for (ChunkDataFeature feature : ShapeFeatures.onLevel(level, ChunkDataFeature.class)) {
            CompoundTag data = this.exotic_worlds$featureData.get(feature.getDataKey());
            if (data != null) {
                feature.restoreData(level, callback.getReturnValue(), data);
            }
        }
    }

    @WrapOperation(
            method = "read",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/storage/SerializableChunkData;unpackStructureReferences(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Ljava/util/Map;"))
    private Map<Structure, LongSet> exotic_worlds$keepReferencesAcrossTheSeam(
            RegistryAccess registryAccess,
            ChunkPos pos,
            CompoundTag tag,
            Operation<Map<Structure, LongSet>> original,
            @Local(argsOnly = true) ServerLevel level) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(level);
        if (fold == null) {
            return original.call(registryAccess, pos, tag);
        }

        Map<Structure, LongSet> references = Maps.newHashMap();
        Registry<Structure> structures = registryAccess.lookupOrThrow(Registries.STRUCTURE);
        tag.getCompoundOrEmpty(exotic_worlds$REFERENCES_KEY).forEach((key, entry) -> {
            Identifier structureId = Identifier.tryParse(key);
            Structure structure = structures.getValue(structureId);
            if (structure == null) {
                exotic_worlds$LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", structureId, pos);
                return;
            }

            Optional<long[]> stored = entry.asLongArray();
            if (stored.isEmpty()) {
                return;
            }

            references.put(structure, exotic_worlds$keptReferences(fold, pos, structureId, stored.get()));
        });

        return references;
    }

    @Unique
    private static LongSet exotic_worlds$keptReferences(WorldFold fold, ChunkPos pos, Identifier structureId,
            long[] stored) {
        LongSet kept = new LongOpenHashSet();
        for (long referenceKey : stored) {
            ChunkPos referencePos = ChunkPos.unpack(referenceKey);
            if (pos.getChessboardDistance(fold.nearestCopy(pos, referencePos)) > exotic_worlds$MAX_REFERENCE_DISTANCE_CHUNKS) {
                exotic_worlds$LOGGER.warn(
                        "Found invalid structure reference [ {} @ {} ] for chunk {}.", structureId, referencePos, pos);
                continue;
            }

            kept.add(referenceKey);
        }

        return kept;
    }
}
