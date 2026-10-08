package com.exoticworlds.compat;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.ExoticWorldsClientApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

public final class ClientShapes {
    public static @Nullable ToroidalShape current() {
        return of(Minecraft.getInstance().level);
    }

    public static @Nullable ToroidalShape of(Level level) {
        return level instanceof ClientLevel client ? ExoticWorldsClientApi.shapeOf(client).orElse(null) : null;
    }

    private ClientShapes() {
    }
}
