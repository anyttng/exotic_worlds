package com.toroidalworld.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.rotor.AlternatorBrushesBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.electroenergetics.WireSpan;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = AlternatorBrushesBlockEntity.class, remap = false)
public abstract class AlternatorBrushesBlockEntityMixin {
    @ModifyExpressionValue(method = "getRotors",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_RELATIVE_BY))
    private BlockPos toroidal$canonicalAlongRotor(BlockPos along) {
        return WireSpan.block(((BlockEntity) (Object) this).getLevel(), along);
    }
}
