package com.exoticworlds;

import java.util.Set;

import com.exoticworlds.compat.c2me.C2meAquifer;
import com.exoticworlds.compat.sable.SableMod;

public class ToroidalMixinPlugin extends MixinGatePlugin {
    private static final String AQUIFER_SEAM_MIXIN = "com.exoticworlds.mixin.AquiferSeamMixin";

    private static final String PARROT_MIXIN = "com.exoticworlds.mixin.ParrotMixin";

    private static final String GAME_EVENT_LISTENER_RANGE_MIXIN =
            "com.exoticworlds.mixin.EuclideanGameEventListenerRegistryMixin";

    private static final String SIGN_FACING_MIXIN = "com.exoticworlds.mixin.SignFacingMixin";

    private static final Set<String> SABLE_CLAIMED_MIXINS =
            Set.of(PARROT_MIXIN, GAME_EVENT_LISTENER_RANGE_MIXIN, SIGN_FACING_MIXIN);

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (AQUIFER_SEAM_MIXIN.equals(mixinClassName)) {
            return !C2meAquifer.optimizesAquifer();
        }

        if (SABLE_CLAIMED_MIXINS.contains(mixinClassName)) {
            return !SableMod.installed();
        }

        return true;
    }
}
