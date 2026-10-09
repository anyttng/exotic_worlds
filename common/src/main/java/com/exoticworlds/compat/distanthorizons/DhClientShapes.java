package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;

public final class DhClientShapes {
    public static @Nullable DhLattice current() {
        return DhShapes.latticeOf(Minecraft.getInstance().level);
    }

    private DhClientShapes() {
    }
}
