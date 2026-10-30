package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.seam.SeamRange;
import com.hollingsworth.arsnouveau.api.source.SourceManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

@Mixin(value = SourceManager.class, remap = false)
public abstract class SourceManagerMixin {
    @WrapOperation(
            method = {
                    "takeSourceNearby(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;II)Lcom/hollingsworth/arsnouveau/api/source/ISpecialSourceProvider;",
                    "hasSourceNearby(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;II)Lcom/hollingsworth/arsnouveau/api/source/ISpecialSourceProvider;",
                    "canGiveSourceNearby(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;I)Ljava/util/List;",
                    "canTakeSourceNearby(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;I)Ljava/util/List;"
            },
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN))
    private boolean exotic_worlds$providerRangeThroughSeam(BlockPos provider, Vec3i asked, double range,
            Operation<Boolean> original, @Local(argsOnly = true) Level level) {
        return SeamRange.sqr(WorldLoopAttachments.wrappedFoldOfReader(level), provider, asked)
                < Mth.square(range);
    }
}
