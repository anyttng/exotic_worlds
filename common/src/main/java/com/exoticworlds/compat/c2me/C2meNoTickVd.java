package com.exoticworlds.compat.c2me;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModSymbol;

public final class C2meNoTickVd {
    private static final String NO_TICK_LOADER = "com/ishland/c2me/notickvd/common/PlayerNoTickLoader";

    static final ModSymbol NO_TICK_LOADER_VIEW_DISTANCE =
            new ModSymbol(NO_TICK_LOADER, "setViewDistance", "(I)V");

    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(), NO_TICK_LOADER + ".class",
            "[c2me-compat] gate notickvd_present", NO_TICK_LOADER_VIEW_DISTANCE);

    public static boolean present() {
        return GATE.present();
    }

    private C2meNoTickVd() {
    }
}
