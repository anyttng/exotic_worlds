package com.exoticworlds.compat.sable.mixin;

import java.nio.file.Files;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.sable.SableChunkKeys;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Mixin(value = SubLevelHoldingChunkMap.class, remap = false)
public class SubLevelHoldingChunkMapMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private SubLevelStorage storage;

    @Unique
    private final LongSet exotic_worlds$absentRegions = new LongOpenHashSet();

    @Shadow
    private SubLevelHoldingChunk getOrLoadHoldingChunk(ChunkPos chunkPos, boolean create) {
        throw new AssertionError();
    }

    @ModifyVariable(method = "moveToUnloaded", at = @At("HEAD"), argsOnly = true)
    private ChunkPos exotic_worlds$holdInPhysicalChunk(ChunkPos pos) {
        return SableChunkKeys.physical(this.level, pos);
    }

    @ModifyExpressionValue(
            method = "saveAll",
            at = @At(value = "NEW", target = "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/ChunkPos;", remap = true))
    private ChunkPos exotic_worlds$saveInPhysicalChunk(ChunkPos pos) {
        return SableChunkKeys.physical(this.level, pos);
    }

    @Inject(method = "processLoad", at = @At("RETURN"))
    private void exotic_worlds$loadRawAliases(ChunkPos pos, CallbackInfo ci) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (fold == null) {
            return;
        }

        for (ChunkPos alias : SableChunkKeys.rawAliases(fold, pos)) {
            if (!exotic_worlds$regionStored(alias)) {
                continue;
            }

            SubLevelHoldingChunk holdingChunk = getOrLoadHoldingChunk(alias, false);
            if (holdingChunk != null) {
                holdingChunk.markKeepLoaded();
            }
        }
    }

    @Unique
    private boolean exotic_worlds$regionStored(ChunkPos chunk) {
        long region = ChunkPos.asLong(chunk.getRegionX(), chunk.getRegionZ());
        if (this.exotic_worlds$absentRegions.contains(region)) {
            return false;
        }

        if (Files.isRegularFile(SableChunkKeys.regionFile(this.storage.getFolder(), chunk))) {
            return true;
        }

        this.exotic_worlds$absentRegions.add(region);
        return false;
    }
}
