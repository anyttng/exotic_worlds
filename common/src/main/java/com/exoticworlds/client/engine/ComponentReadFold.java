package com.exoticworlds.client.engine;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.net.ComponentPositions;
import com.exoticworlds.engine.net.FoldedValue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.phys.Vec3;

public final class ComponentReadFold {
    public static @Nullable Object seated(DataComponentType<?> type, @Nullable Object stored) {
        ComponentPositions.Mover mover = ComponentPositions.moverOf(type);
        if (mover == null || stored == null) {
            return stored;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !minecraft.isSameThread()) {
            return stored;
        }

        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        WorldFold fold = ClientFrame.fold();
        if (level == null || player == null || fold == null) {
            return stored;
        }

        Vec3 anchor = player.position();
        return mover.moved(stored, value -> FoldedValue.toward(fold, level.dimension(), anchor, value));
    }

    private ComponentReadFold() {
    }
}
