package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.engine.seam.SeamSteering;
import com.hollingsworth.arsnouveau.common.entity.WildenChimera;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

@Mixin(value = WildenChimera.class, remap = false)
public abstract class WildenChimeraMixin {
    @WrapMethod(method = "faceBlock")
    private static void exotic_worlds$faceThroughSeam(BlockPos block, LivingEntity citizen, Operation<Void> original) {
        original.call(SeamSteering.nearestCopy(citizen, block), citizen);
    }
}
