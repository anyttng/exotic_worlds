package com.exoticworlds.client.engine;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.NearestCopy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class SpawnPacketSeat {
    public static Vec3 seated(Vec3 stored) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !minecraft.isSameThread()) {
            return stored;
        }

        LocalPlayer player = minecraft.player;
        return seated(ClientFrame.fold(), player == null ? null : player.position(), stored);
    }

    static Vec3 seated(@Nullable WorldFold fold, @Nullable Vec3 anchor, Vec3 stored) {
        return anchor == null ? stored : NearestCopy.toward(fold, anchor, stored);
    }

    private SpawnPacketSeat() {
    }
}
