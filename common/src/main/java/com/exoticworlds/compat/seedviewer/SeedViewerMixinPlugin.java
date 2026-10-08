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

    static final ModSymbol NOISE_POINT_SAMPLER = new ModSymbol(NOISE_SAMPLER, "pointSampler",
            "()L" + POINT_SAMPLER + ";");

    static final ModSymbol POINT_OWNER = new ModSymbol(POINT_SAMPLER, "this$0", "L" + NOISE_SAMPLER + ";");

    static final ModSymbol POINT_SAMPLE = new ModSymbol(POINT_SAMPLER, SAMPLE,
            "(Lnet/acenia/seedviewer/client/worldgen/NoiseMapType;III)F");

    static final ModSymbol CONTEXT = new ModSymbol(PREVIEW_CONTEXT, SeedViewerInjectionTargets.CONSTRUCTOR,
            SeedViewerInjectionTargets.CONTEXT_DESCRIPTOR);

    static final ModSymbol FINGERPRINT = new ModSymbol(PREVIEW_WIDGET, "settingsFingerprint", "()Ljava/lang/String;");

    // A game type is spelled per loader on this line, so a target class is gated by members naming none; every member
    // of BiomeSampler names one, so its presence alone gates it.
    static final ModSymbol[] SYMBOLS = {NOISE_POINT_SAMPLER, POINT_OWNER, POINT_SAMPLE, CONTEXT, FINGERPRINT};

    private static final ModPresence SEED_VIEWER = ModPresence.of(LOGGER, BIOME_SAMPLER + ".class",
            "[seedviewer-compat] gate seedviewer_present", SYMBOLS);

    public SeedViewerMixinPlugin() {
        super(SEED_VIEWER);
    }
}
