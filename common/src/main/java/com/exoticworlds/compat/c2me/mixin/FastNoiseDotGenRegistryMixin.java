package com.exoticworlds.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.c2me.C2meFoldedFastNoiseDotEmitter;
import com.exoticworlds.compat.c2me.C2meFoldedFastNoiseNode;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGenRegistry;

@Mixin(DotGenRegistry.class)
public class FastNoiseDotGenRegistryMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void exotic_worlds$registerFoldedFastNoise(CallbackInfo ci) {
        DotGenRegistry.REGISTRY.registerExactMatch(C2meFoldedFastNoiseNode.class,
                C2meFoldedFastNoiseDotEmitter.INSTANCE);
    }
}
