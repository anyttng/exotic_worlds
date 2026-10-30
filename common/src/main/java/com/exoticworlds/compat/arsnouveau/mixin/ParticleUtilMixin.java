package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = ParticleUtil.class, remap = false)
public abstract class ParticleUtilMixin {
    @WrapMethod(method = "beam(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;)V")
    private static void exotic_worlds$beamEndNearStart(BlockPos to, BlockPos from, Level level,
            Operation<Void> original) {
        original.call(NearestCopy.toward(WorldLoopAttachments.wrappedFoldOfReader(level), from, to), from, level);
    }
}
