package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.hollingsworth.arsnouveau.api.item.inv.FilterSet;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = FilterSet.class, remap = false)
public abstract class FilterSetMixin {
    @WrapOperation(
            method = "forPosition(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lcom/hollingsworth/arsnouveau/api/item/inv/FilterSet;",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_EQUALS))
    private static boolean exotic_worlds$attachedThroughSeam(BlockPos attachedTo, Object pos,
            Operation<Boolean> original, @Local(argsOnly = true) Level level) {
        if (!(pos instanceof BlockPos block)) {
            return original.call(attachedTo, pos);
        }

        WorldFold fold = WorldLoopAttachments.transformerOfReader(level);
        return original.call(fold.fold(attachedTo), fold.fold(block));
    }
}
