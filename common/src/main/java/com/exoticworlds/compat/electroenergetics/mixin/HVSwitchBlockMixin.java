package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.transmission_distribution.hv_switch.HVSwitchBlock;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(value = HVSwitchBlock.class, remap = false)
public abstract class HVSwitchBlockMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_RELATIVE_BY))
    private BlockPos toroidal$canonicalTarget(BlockPos target, @Local(argsOnly = true) ServerLevel level) {
        return WireSpan.block(level, target);
    }
}
