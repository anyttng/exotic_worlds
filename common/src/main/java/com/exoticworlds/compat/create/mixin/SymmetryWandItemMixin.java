package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.zurrtum.create.infrastructure.component.SymmetryMirror;
import com.exoticworlds.compat.create.SymmetryMirrorFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(SymmetryWandItem.class)
public class SymmetryWandItemMixin {
    @ModifyVariable(method = "apply", at = @At("STORE"), ordinal = 0)
    private static SymmetryMirror exotic_worlds$foldApplyMirror(SymmetryMirror mirror,
            @Local(argsOnly = true) ServerLevel world, @Local(argsOnly = true) BlockPos pos) {
        return SymmetryMirrorFold.nearestTo(world, mirror, pos);
    }

    @ModifyVariable(method = "remove", at = @At("STORE"), ordinal = 0)
    private static SymmetryMirror exotic_worlds$foldRemoveMirror(SymmetryMirror mirror,
            @Local(argsOnly = true) ServerLevel world, @Local(argsOnly = true) BlockPos pos) {
        return SymmetryMirrorFold.nearestTo(world, mirror, pos);
    }
}
