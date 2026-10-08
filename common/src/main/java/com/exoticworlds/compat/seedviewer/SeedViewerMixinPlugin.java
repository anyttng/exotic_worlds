package com.exoticworlds.compat.seedviewer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;
import com.exoticworlds.compat.ModSymbol;

public class SeedViewerMixinPlugin extends ModPresenceGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String BIOME_SAMPLER = "net/acenia/seedviewer/client/worldgen/BiomeSampler";

    private static final String NOISE_SAMPLER = "net/acenia/seedviewer/client/worldgen/NoiseSampler";

    private static final String POINT_SAMPLER = NOISE_SAMPLER + "$PointSampler";

    private static final String PREVIEW_CONTEXT =
            "net/acenia/seedviewer/client/screen/createworld/WorldCreationPreviewContext";

    private static final String PREVIEW_WIDGET = "net/acenia/seedviewer/client/screen/createworld/SeedPreviewWidget";

    private static final String SAMPLE = "sample";

    static final ModSymbol BIOME_FROM_LEVEL = new ModSymbol(BIOME_SAMPLER, SeedViewerInjectionTargets.CONSTRUCTOR,
            SeedViewerInjectionTargets.FROM_LEVEL);

    static final ModSymbol BIOME_FROM_SOURCE = new ModSymbol(BIOME_SAMPLER, SeedViewerInjectionTargets.CONSTRUCTOR,
            SeedViewerInjectionTargets.FROM_BIOME_SOURCE);

    static final ModSymbol BIOME_SAMPLE = new ModSymbol(BIOME_SAMPLER, SAMPLE, "(III)Lnet/minecraft/core/Holder;");

    static final ModSymbol NOISE_FROM_LEVEL = new ModSymbol(NOISE_SAMPLER, SeedViewerInjectionTargets.CONSTRUCTOR,
            SeedViewerInjectionTargets.FROM_LEVEL);

    static final ModSymbol NOISE_FROM_RANDOM_STATE = new ModSymbol(NOISE_SAMPLER,
            SeedViewerInjectionTargets.CONSTRUCTOR, SeedViewerInjectionTargets.FROM_RANDOM_STATE);

    static final ModSymbol POINT_OWNER = new ModSymbol(POINT_SAMPLER, "this$0", "L" + NOISE_SAMPLER + ";");

    static final ModSymbol POINT_SAMPLE = new ModSymbol(POINT_SAMPLER, SAMPLE,
            "(Lnet/acenia/seedviewer/client/worldgen/NoiseMapType;III)F");

    static final ModSymbol CONTEXT = new ModSymbol(PREVIEW_CONTEXT, SeedViewerInjectionTargets.CONSTRUCTOR,
            SeedViewerInjectionTargets.CONTEXT_DESCRIPTOR);

    static final ModSymbol FINGERPRINT = new ModSymbol(PREVIEW_WIDGET, "settingsFingerprint", "()Ljava/lang/String;");

    static final ModSymbol[] SYMBOLS = {BIOME_FROM_LEVEL, BIOME_FROM_SOURCE, BIOME_SAMPLE, NOISE_FROM_LEVEL,
            NOISE_FROM_RANDOM_STATE, POINT_OWNER, POINT_SAMPLE, CONTEXT, FINGERPRINT};

    private static final ModPresence SEED_VIEWER = ModPresence.of(LOGGER, BIOME_SAMPLER + ".class",
            "[seedviewer-compat] gate seedviewer_present", SYMBOLS);

    public SeedViewerMixinPlugin() {
        super(SEED_VIEWER);
    }
}
