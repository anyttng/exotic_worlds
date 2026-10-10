package com.exoticworlds.compat.immersiveportals;

import com.exoticworlds.compat.ModPresence;

public final class ImmersivePortalsMod {
    private static final String TRACKED_ENTITY_OVERWRITE =
            "qouteall/imm_ptl/core/mixin/common/entity_sync/MixinTrackedEntity.class";

    // Immersive Portals empties updatePlayer, so the vanilla fold over it has nothing left to wrap.
    public static boolean overwritesEntityTracking() {
        return ModPresence.probe(TRACKED_ENTITY_OVERWRITE);
    }

    private ImmersivePortalsMod() {
    }
}
