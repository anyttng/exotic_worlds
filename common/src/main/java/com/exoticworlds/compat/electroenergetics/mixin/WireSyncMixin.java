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
    private void toroidal$resendAfterAJump(ServerPlayer player, long newPos, CallbackInfo ci) {
        if (loadedChunks.get(player.getUUID()) instanceof WireBox box
                && WireBoxJump.mayReseat(WorldLoopAttachments.transformerOf(this.level), box.toroidal$centre(),
                        box.toroidal$radius(), new ChunkPos(newPos), toroidal$boxRadius(WireSync.getViewDistance()))) {
            this.unloadForPlayer(player);
        }
    }

    @ModifyExpressionValue(method = "handlePlayerEnterNewSection", at = @At(value = "INVOKE",
            target = "Lcom/george_vi/electroenergetics/content/wire/WireSync;getViewDistance()I"))
    private int toroidal$boxWithinHalfTheWorld(int viewDistance) {
        return toroidal$boxRadius(viewDistance);
    }

    @Unique
    private int toroidal$boxRadius(int viewDistance) {
        return WorldLoopAttachments.transformerOf(this.level).limitViewDistance(viewDistance);
    }

    @WrapOperation(method = "handlePlayerEnterNewSection",
            at = @At(value = "INVOKE", target = InjectionTargets.MAP_PUT))
    private Object toroidal$bindBoxFold(Map<Object, Object> boxes, Object player, Object box,
            Operation<Object> original) {
        ((TransformerHolder) box).toroidal$setTransformer(WorldLoopAttachments.transformerOf(this.level));
        return original.call(boxes, player, box);
    }

    @ModifyExpressionValue(
            method = {"handleWireRemoved", "handleWireAdded", "handleNodeLabelRename", "handleNodeCreate",
                    "handleNodeRemove"},
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_AS_LONG))
    private long toroidal$canonicalChunk(long chunk) {
        return WireSpan.chunkKey(this.level, chunk);
    }

    @ModifyExpressionValue(
            method = {"handleCatenaryRemoved", "handleCatenaryAdded", "handleWireRepositioned", "handleNodeMoved",
                    "lambda$handlePlayerEnterNewSection$4"},
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_AS_LONG_BLOCK))
    private long toroidal$canonicalBlockChunk(long chunk) {
        return WireSpan.chunkKey(this.level, chunk);
    }
}
