package com.exoticworlds.compat.electroenergetics.mixin;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.george_vi.electroenergetics.content.wire.WireSync;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.compat.electroenergetics.WireBox;
import com.exoticworlds.compat.electroenergetics.WireBoxJump;
import com.exoticworlds.compat.electroenergetics.WireSpan;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

@Mixin(value = WireSync.class, remap = false)
public abstract class WireSyncMixin {
    @Shadow
    @Final
    private static Map<UUID, ?> loadedChunks;

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    public abstract void unloadForPlayer(ServerPlayer player);

    @Inject(method = "handlePlayerEnterNewSection", at = @At("HEAD"))
    private void exotic_worlds$resendAfterAJump(ServerPlayer player, long newPos, CallbackInfo ci) {
        if (loadedChunks.get(player.getUUID()) instanceof WireBox box
                && WireBoxJump.mayReseat(WorldLoopAttachments.transformerOf(this.level), box.exotic_worlds$centre(),
                        box.exotic_worlds$radius(), new ChunkPos(newPos), exotic_worlds$boxRadius(WireSync.getViewDistance()))) {
            this.unloadForPlayer(player);
        }
    }

    @ModifyExpressionValue(method = "handlePlayerEnterNewSection", at = @At(value = "INVOKE",
            target = "Lcom/george_vi/electroenergetics/content/wire/WireSync;getViewDistance()I"))
    private int exotic_worlds$boxWithinHalfTheWorld(int viewDistance) {
        return exotic_worlds$boxRadius(viewDistance);
    }

    @Unique
    private int exotic_worlds$boxRadius(int viewDistance) {
        return WorldLoopAttachments.transformerOf(this.level).limitViewDistance(viewDistance);
    }

    @WrapOperation(method = "handlePlayerEnterNewSection",
            at = @At(value = "INVOKE", target = InjectionTargets.MAP_PUT))
    private Object exotic_worlds$bindBoxFold(Map<Object, Object> boxes, Object player, Object box,
            Operation<Object> original) {
        ((TransformerHolder) box).exotic_worlds$setTransformer(WorldLoopAttachments.transformerOf(this.level));
        return original.call(boxes, player, box);
    }

    @ModifyExpressionValue(
            method = {"handleWireRemoved", "handleWireAdded", "handleNodeLabelRename", "handleNodeCreate",
                    "handleNodeRemove"},
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_AS_LONG))
    private long exotic_worlds$canonicalChunk(long chunk) {
        return WireSpan.chunkKey(this.level, chunk);
    }

    @ModifyExpressionValue(
            method = {"handleCatenaryRemoved", "handleCatenaryAdded", "handleWireRepositioned", "handleNodeMoved",
                    "lambda$handlePlayerEnterNewSection$4"},
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_AS_LONG_BLOCK))
    private long exotic_worlds$canonicalBlockChunk(long chunk) {
        return WireSpan.chunkKey(this.level, chunk);
    }
}
