package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.kinetics.gearbox.GearboxRenderer;
import com.zurrtum.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(GearboxRenderer.class)
public class GearboxRendererMixin {
    @WrapOperation(
            method = "extractRenderState(Lcom/zurrtum/create/content/kinetics/gearbox/GearboxBlockEntity;"
                    + "Lcom/zurrtum/create/client/content/kinetics/gearbox/GearboxRenderer$GearboxRenderState;"
                    + "FLnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_SUBTRACT))
    private BlockPos exotic_worlds$foldSourceDelta(BlockPos sourcePos, Vec3i anchorPos, Operation<BlockPos> original,
            @Local(argsOnly = true) GearboxBlockEntity be) {
        return CreateSeamFold.foldDelta(be.getLevel(), be.getBlockPos(), sourcePos,
                original.call(sourcePos, anchorPos));
    }
}
