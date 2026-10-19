package com.exoticworlds.compat.c2me;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class C2meMixinErrorHandler implements IMixinErrorHandler {
    @Override
    public @Nullable ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action) {
        return escalate(config);
    }

    @Override
    public @Nullable ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
        return escalate(mixin.getConfig());
    }

    private static @Nullable ErrorAction escalate(IMixinConfig config) {
        return config.getPlugin() instanceof C2meMixinPlugin ? ErrorAction.ERROR : null;
    }
}
