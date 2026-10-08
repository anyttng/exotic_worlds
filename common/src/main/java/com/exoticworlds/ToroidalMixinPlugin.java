package com.exoticworlds;

import com.exoticworlds.compat.c2me.C2meAquifer;

public class ToroidalMixinPlugin extends MixinGatePlugin {
    private static final String AQUIFER_SEAM_MIXIN = "com.exoticworlds.mixin.AquiferSeamMixin";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (AQUIFER_SEAM_MIXIN.equals(mixinClassName)) {
            return !C2meAquifer.optimizesAquifer();
        }

        return true;
    }
}
