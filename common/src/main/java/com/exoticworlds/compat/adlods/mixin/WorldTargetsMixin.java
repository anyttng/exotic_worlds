package com.exoticworlds.compat.adlods.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.endertech.minecraft.mods.adlods.world.WorldTargets;
import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.compat.adlods.DeferredEntryKeys;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Mixin(WorldTargets.class)
public abstract class WorldTargetsMixin implements FoldHolder {
    @Shadow
    @Final
    protected Map<ChunkPos, Map<BlockPos, BlockPos>> deferredEntryPoints;

    @Unique
    private volatile WorldFold exotic_worlds$fold = WorldFolds.NOOP;

    @Inject(method = "get", at = @At("RETURN"))
    private static void exotic_worlds$bindTheLevelsFold(ServerLevel level, CallbackInfoReturnable<WorldTargets> cir) {
        ((FoldHolder) cir.getReturnValue()).exotic_worlds$setFold(WorldLoopAttachments.foldOf(level));
    }

    @ModifyExpressionValue(method = "saveDeferredEntryPoint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ChunkPos;containing(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos exotic_worlds$keyByTheChunkThatContinues(ChunkPos raw) {
        return this.exotic_worlds$fold.fold(raw);
    }

    @Override
    public WorldFold exotic_worlds$fold() {
        return this.exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$setFold(WorldFold fold) {
        if (this.exotic_worlds$fold == fold) {
            return;
        }

        synchronized (this) {
            if (this.exotic_worlds$fold != fold) {
                DeferredEntryKeys.fold(this.deferredEntryPoints, fold);
                this.exotic_worlds$fold = fold;
            }
        }
    }
}
