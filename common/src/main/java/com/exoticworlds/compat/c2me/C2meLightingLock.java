package com.exoticworlds.compat.c2me;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModSymbol;

public final class C2meLightingLock {
    public static final String OVERWRITE_RESOURCE =
            "com/ishland/c2me/threading/lighting/mixin/scalablelux/MixinSchedulingUtil.class";

    static final ModSymbol SCHEDULE_TASK = new ModSymbol(
            "ca/spottedleaf/starlight/common/thread/SchedulingUtil", "scheduleTask", "(ILjava/lang/Runnable;III)V");

    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(), OVERWRITE_RESOURCE,
            "[c2me-compat] gate lighting_lock_present", SCHEDULE_TASK);

    public static boolean present() {
        return GATE.present();
    }

    private C2meLightingLock() {
    }
}
