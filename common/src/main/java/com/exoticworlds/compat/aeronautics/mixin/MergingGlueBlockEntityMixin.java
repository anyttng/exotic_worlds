package com.exoticworlds.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.aeronautics.MergingGlueSeamFrame;
import com.exoticworlds.compat.sable.SeamFrame;

import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = MergingGlueBlockEntity.class, remap = false)
public class MergingGlueBlockEntityMixin {
    @WrapMethod(method = "startControlling")
    private void exotic_worlds$controlInTheSeamFrame(MergingGlueBlockEntity partner, Operation<Void> original) {
        Object entered = MergingGlueSeamFrame.enter((BlockEntity) (Object) this, partner);
        try {
            original.call(partner);
        } finally {
            SeamFrame.exit(entered);
        }
    }
}
