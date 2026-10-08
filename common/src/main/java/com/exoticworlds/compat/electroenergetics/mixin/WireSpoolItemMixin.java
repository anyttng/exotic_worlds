package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.wire_spool.WireSpoolItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

@Mixin(value = WireSpoolItem.class, remap = false)
public abstract class WireSpoolItemMixin {
    @WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double toroidal$placedSpan(BlockPos from, Vec3i to, Operation<Double> original,
            @Local(argsOnly = true) UseOnContext context) {
        return original.call(from, WireSpan.seat(context.getLevel(), from, to));
    }

    @WrapOperation(method = "interactDetachedNode",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double toroidal$hungSpan(BlockPos from, Vec3i to, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return original.call(from, WireSpan.seat(level, from, to));
    }
}
