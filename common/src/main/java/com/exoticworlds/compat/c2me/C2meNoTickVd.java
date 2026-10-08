package com.exoticworlds.compat.c2me;

import java.util.List;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class C2meNoTickVd {
    static final List<String> MIXINS = List.of("PlayerNoTickLoaderMixin", "ServerAccessibleChunkSendingMixin");

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[c2me-compat] gate notickvd_present")
            .probing("com/ishland/c2me/notickvd/common/PlayerNoTickLoader.class")
            .checking(C2meMixinPlugin.CONFIG, mixin -> MIXINS.contains(mixin))
            .build();

    public static boolean present() {
        return GATE.present();
    }

    private C2meNoTickVd() {
    }
}
