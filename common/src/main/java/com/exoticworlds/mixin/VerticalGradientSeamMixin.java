package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.gen.CanonicalRandomFactory;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.level.levelgen.PositionalRandomFactory;

@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$VerticalGradientConditionSource")
public class VerticalGradientSeamMixin {
    @ModifyExpressionValue(
            method = "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)"
                    + "Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/RandomState;getOrCreateRandomFactory("
                            + "Lnet/minecraft/resources/ResourceLocation;)"
                            + "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;"))
    private PositionalRandomFactory toroidal$seedGradientFromCanonical(PositionalRandomFactory factory) {
        WorldFold fold = GenerationTransformerContext.context().wrappedTransformer();
        return fold == null ? factory : new CanonicalRandomFactory(factory, fold);
    }
}
