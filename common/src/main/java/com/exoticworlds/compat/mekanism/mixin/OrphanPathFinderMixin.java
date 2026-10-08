package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(targets = "mekanism.common.lib.transmitter.TransmitterNetworkRegistry$OrphanPathFinder", remap = false)
public class OrphanPathFinderMixin {
    @Shadow
    @Final
    private Level world;

    @ModifyVariable(method = "iterate", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$foldWalkStep(BlockPos from) {
        return WorldLoopAttachments.transformerOf(this.world).fold(from);
    }
}
