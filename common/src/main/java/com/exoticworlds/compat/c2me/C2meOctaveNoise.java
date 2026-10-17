package com.exoticworlds.compat.c2me;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;

public final class C2meOctaveNoise {
    static final String MIXIN = "PerlinNoiseMixin";

    static final ModPresence GATE = ModPresence.gate(LogUtils.getLogger(), "[ew/c2me-compat] gate octave_noise_present")
            .probing("com/ishland/c2me/opts/math/mixin/MixinOctavePerlinNoiseSampler.class")
            .checking(C2meMixinPlugin.CONFIG, MIXIN::equals)
            .build();

    private C2meOctaveNoise() {
    }
}
