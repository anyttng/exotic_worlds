package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectRedstone;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = EffectRedstone.class, remap = false)
public abstract class EffectRedstoneMixin {
    @WrapOperation(
            method = "onResolveBlock(Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;immutable()Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$canonicalSignalKey(BlockPos pos, Operation<BlockPos> original,
            @Local(argsOnly = true) Level level) {
        return WorldLoopAttachments.transformerOf(level).fold(original.call(pos));
    }
}
