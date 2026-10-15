package com.exoticworlds.compat.sable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class SableReachFrame {
    public static boolean blockReach(Player player, BlockPos pos, double buffer, Operation<Boolean> original) {
        return SeamFrame.with(player.level(), player, Entity::position, original, pos, buffer);
    }

    public static boolean entityReach(Player player, AABB box, double buffer, Operation<Boolean> original) {
        return SeamFrame.with(player.level(), player, Entity::position, original, box, buffer);
    }

    public static boolean bedReach(ServerPlayer player, BlockPos pos, Operation<Boolean> original) {
        return SeamFrame.with(player.level(), player, Entity::position, original, pos);
    }

    private SableReachFrame() {
    }
}
