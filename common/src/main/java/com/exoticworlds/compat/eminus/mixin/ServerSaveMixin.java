package com.exoticworlds.compat.eminus.mixin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.eminus.ingest.saved.SavedChunk;
import com.eminus.ingest.saved.ServerSave;
import com.exoticworlds.compat.eminus.SavedChunkOwner;
import com.exoticworlds.compat.eminus.SavedLevelSource;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Mixin(ServerSave.class)
public class ServerSaveMixin implements SavedLevelSource {
    @Shadow
    @Final
    private ServerLevel level;

    @Override
    public ServerLevel exotic_worlds$getSavedLevel() {
        return level;
    }

    @WrapOperation(
            method = "read",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;read("
                    + "Lnet/minecraft/world/level/ChunkPos;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Optional<CompoundTag>> exotic_worlds$readStoredChunk(ChunkMap storage, ChunkPos pos,
            Operation<CompletableFuture<Optional<CompoundTag>>> original) {
        return original.call(storage, WorldLoopAttachments.getFold(level).fold(pos));
    }

    @Inject(method = "parse", at = @At("RETURN"))
    private void exotic_worlds$ownedByTheSavedLevel(CompoundTag chunkTag, CallbackInfoReturnable<SavedChunk> cir) {
        SavedChunkOwner.stamp(cir.getReturnValue(), level);
    }
}
