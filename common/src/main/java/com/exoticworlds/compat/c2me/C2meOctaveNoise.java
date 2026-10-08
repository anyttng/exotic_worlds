package com.exoticworlds.compat.c2me;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModSymbol;

public final class C2meOctaveNoise {
    private static final String OCTAVE_SAMPLER = "com/ishland/c2me/opts/math/mixin/MixinOctavePerlinNoiseSampler";

    static final ModSymbol OCTAVE_SAMPLER_INIT_HANDLER = new ModSymbol(OCTAVE_SAMPLER, "onInit",
            "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");

    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(), OCTAVE_SAMPLER + ".class",
            "[c2me-compat] gate octave_noise_present", OCTAVE_SAMPLER_INIT_HANDLER);

    public static boolean present() {
        return GATE.present();
    }

    private C2meOctaveNoise() {
    }
}
