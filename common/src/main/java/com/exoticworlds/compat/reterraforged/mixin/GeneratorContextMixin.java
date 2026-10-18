package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.reterraforged.LapCarrier;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

import raccoonman.reterraforged.world.worldgen.GeneratorContext;

@Mixin(value = GeneratorContext.class, remap = false)
public abstract class GeneratorContextMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$carryRouterBuildFold(CallbackInfo callback) {
        WorldFold fold = GenerationTransformerContext.context().routerBuildTransformer();
        if (fold != null && fold.isWrapped()) {
            LapCarrier.carry((GeneratorContext) (Object) this, fold);
        }
    }
}
