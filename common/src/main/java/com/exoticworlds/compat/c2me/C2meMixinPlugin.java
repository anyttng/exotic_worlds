package com.exoticworlds.compat.c2me;

import org.spongepowered.asm.mixin.Mixins;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.mojang.logging.LogUtils;
import com.exoticworlds.compat.ModPresence;
import com.exoticworlds.compat.ModPresenceGatePlugin;

public class C2meMixinPlugin extends ModPresenceGatePlugin {
    static final String CONFIG = "exotic_worlds.compat.c2me.mixins.json";

    static final ModPresence AQUIFER_GATE = ModPresence.gate(LogUtils.getLogger(),
                    "[c2me-compat] gate aquifer_handler_present")
            .probing("com/ishland/c2me/opts/worldgen/vanilla/mixin/aquifer/MixinAquiferSamplerImpl.class")
            .checking(CONFIG, C2meAquifer.MIXIN::equals)
            .build();

    public C2meMixinPlugin() {
        super(C2meChunkSystem.GATE, C2meLightingLock.GATE, C2meNoTickVd.GATE, C2meOctaveNoise.GATE, C2meDfc.GATE,
                AQUIFER_GATE);
    }

    @Override
    public void onLoad(String mixinPackage) {
        MixinSquaredBootstrap.init();
        Mixins.registerErrorHandlerClass(C2meMixinErrorHandler.class.getName());
        super.onLoad(mixinPackage);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (AQUIFER_GATE.covers(mixinClassName)) {
            return C2meAquifer.optimizesAquifer();
        }

        return super.shouldApplyMixin(targetClassName, mixinClassName);
    }
}
