package com.exoticworlds.compat.betterend.mixin;

import org.betterx.betterend.blocks.HelixTreeLeavesBlock;
import org.betterx.betterend.blocks.JellyshroomCapBlock;
import org.betterx.betterend.blocks.UmbrellaTreeMembraneBlock;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.betterend.BetterEndInjectionTargets;
import com.exoticworlds.compat.wover.LapNoise;
import com.exoticworlds.compat.wover.StandInHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.item.context.BlockPlaceContext;

@Mixin({HelixTreeLeavesBlock.class, JellyshroomCapBlock.class, UmbrellaTreeMembraneBlock.class})
public class ColourBlockPlacementMixin {
    private static final double SCALE = 0.1;

    private static final String PLACEMENT_DESCRIPTOR =
            "(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;";

    // The client predicts the placement too, so it reads the same lap noise through its own fold. The Fabric jar
    // spells this override in intermediary, and the remapper cannot resolve an override in a foreign class.
    @WrapOperation(
            method = {"getStateForPlacement" + PLACEMENT_DESCRIPTOR, "method_9605" + PLACEMENT_DESCRIPTOR},
            at = @At(value = "INVOKE", target = BetterEndInjectionTargets.NOISE_EVAL_3D))
    private double toroidal$lapColourNoise(OpenSimplexNoise noise, double x, double y, double z,
            Operation<Double> original, @Local(argsOnly = true) BlockPlaceContext context) {
        WorldFold fold = WorldLoopAttachments.transformerOfReader(context.getLevel());
        return fold.isWrapped()
                ? LapNoise.eval(((StandInHolder) (Object) noise).toroidal$standIn(), fold, x, y, z, SCALE, SCALE)
                : original.call(noise, x, y, z);
    }
}
