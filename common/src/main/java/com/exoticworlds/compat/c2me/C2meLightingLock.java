package com.exoticworlds.compat.c2me;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class C2meLightingLock {
    public static final String OVERWRITE_CLASS =
            "com/ishland/c2me/threading/lighting/mixin/scalablelux/MixinSchedulingUtil";

    public static final String OVERWRITE_RESOURCE = OVERWRITE_CLASS + ".class";

    static final String MIXIN = "SchedulingUtilLockMixin";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[c2me-compat] gate lighting_lock_present")
            .probing(OVERWRITE_RESOURCE, "ca/spottedleaf/starlight/common/thread/SchedulingUtil.class")
            .checking(C2meMixinPlugin.CONFIG, MIXIN::equals)
            .bodyFrom(MIXIN, OVERWRITE_CLASS)
            .build();

    public static boolean present() {
        return GATE.present();
    }

    private C2meLightingLock() {
    }
}
