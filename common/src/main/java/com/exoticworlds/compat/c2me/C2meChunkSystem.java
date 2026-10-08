package com.exoticworlds.compat.c2me;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModSymbol;

public final class C2meChunkSystem {
    private static final String CHUNK_SYSTEM = "com/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem";

    static final ModSymbol CHUNK_SYSTEM_SCHEDULING_MANAGER = new ModSymbol(CHUNK_SYSTEM, "schedulingManager",
            "Lcom/ishland/c2me/base/common/scheduler/SchedulingManager;");

    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(), CHUNK_SYSTEM + ".class",
            "[c2me-compat] gate chunk_system_present", CHUNK_SYSTEM_SCHEDULING_MANAGER);

    public static boolean present() {
        return GATE.present();
    }

    private C2meChunkSystem() {
    }
}
