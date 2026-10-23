package com.exoticworlds.compat.arsnouveau.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.hollingsworth.arsnouveau.common.world.saved_data.RedstoneUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(value = RedstoneUtil.class, remap = false)
public abstract class RedstoneUtilMixin {
    private static final String GET_ARS_SIGNAL = "getArsSignal(Lnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;"
            + "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";

    @WrapOperation(
            method = GET_ARS_SIGNAL,
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object exotic_worlds$canonicalSignalRead(Map<?, ?> signals, Object pos, Operation<Object> original,
            @Local(argsOnly = true) ServerLevel level) {
        return original.call(signals, exotic_worlds$canonical(level, pos));
    }

    @WrapOperation(
            method = GET_ARS_SIGNAL,
            at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z"))
    private static boolean exotic_worlds$canonicalSignalLookup(Map<?, ?> signals, Object pos,
            Operation<Boolean> original, @Local(argsOnly = true) ServerLevel level) {
        return original.call(signals, exotic_worlds$canonical(level, pos));
    }

    @Unique
    private static Object exotic_worlds$canonical(ServerLevel level, Object pos) {
        return pos instanceof BlockPos block ? WorldLoopAttachments.transformerOf(level).fold(block) : pos;
    }
}
