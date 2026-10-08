package com.exoticworlds;

import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

public final class BinderOrder {
    public static final int FOLD = InjectionInfo.InjectorOrder.LATE;

    private BinderOrder() {
    }
}
