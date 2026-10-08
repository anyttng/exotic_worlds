package com.exoticworlds.compat.c2me;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class C2meChunkSystem {
    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[c2me-compat] gate chunk_system_present")
            .probing("com/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem.class")
            .checking(C2meMixinPlugin.CONFIG, mixin -> !mixin.equals(C2meAquifer.MIXIN)
                    && !mixin.equals(C2meLightingLock.MIXIN) && !C2meNoTickVd.MIXINS.contains(mixin))
            .build();

    public static boolean present() {
        return GATE.present();
    }

    private C2meChunkSystem() {
    }
}
