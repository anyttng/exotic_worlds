package com.exoticworlds.compat.lithium.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.lithium.LithiumInjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEventDispatcher;

@Mixin(value = GameEventDispatcher.class, priority = 1200)
public class GameEventDispatchKeyMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_AS_LONG))
    @TargetHandler(
            mixin = LithiumInjectionTargets.GAME_EVENT_DISPATCHER_MIXIN,
            name = LithiumInjectionTargets.NULL_DISPATCHER_HANDLER)
    private long toroidal$foldStoreKey(int chunkX, int chunkZ, Operation<Long> original) {
        return WorldLoopAttachments.transformerOf(this.level).foldChunkKey(original.call(chunkX, chunkZ));
    }
}
