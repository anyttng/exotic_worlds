package com.exoticworlds.compat.c2me;

import java.util.List;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class C2meChunkSystem {
    private static final List<String> SCANNED_BY_SIBLINGS = List.of(C2meAquifer.class.getSimpleName(),
            C2meLightingLock.class.getSimpleName(), C2meNoTickVd.class.getSimpleName(),
            C2meOctaveNoise.class.getSimpleName());

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[ew/c2me-compat] gate chunk_system_present")
            .probing("com/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem.class")
            .checking(C2meMixinPlugin.CONFIG, mixin -> !mixin.equals(C2meAquifer.MIXIN)
                    && !mixin.equals(C2meLightingLock.MIXIN) && !C2meNoTickVd.MIXINS.contains(mixin)
                    && !mixin.equals(C2meOctaveNoise.MIXIN) && !C2meDfc.MIXINS.contains(mixin))
            .modPackages(C2meMixinPlugin.C2ME_PACKAGE, C2meMixinPlugin.FLOWSCHED_PACKAGE)
            .scanning(name -> !SCANNED_BY_SIBLINGS.contains(name) && !C2meDfc.CLASSES.contains(name))
            .build();

    private C2meChunkSystem() {
    }
}
