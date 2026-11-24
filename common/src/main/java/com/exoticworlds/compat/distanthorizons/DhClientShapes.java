package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;

public final class DhClientShapes {
    public static @Nullable DhLattice getCurrent() {
        return DhShapes.getClientLattice(Minecraft.getInstance().level);
    }

    private DhClientShapes() {
    }
}
