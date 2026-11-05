package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.sable.SeamFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@Mixin(Player.class)
public abstract class PlayerReachFrameMixin {
    @WrapMethod(method = "canInteractWithBlock")
    private boolean exotic_worlds$frameOnBlockReach(BlockPos pos, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        Object entered = SeamFrame.enter(player.level(), player, Entity::position);
        try {
            return original.call(pos, buffer);
        } finally {
            SeamFrame.exit(entered);
        }
    }

    @WrapMethod(method = "canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean exotic_worlds$frameOnEntityReach(AABB box, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        Object entered = SeamFrame.enter(player.level(), player, Entity::position);
        try {
            return original.call(box, buffer);
        } finally {
            SeamFrame.exit(entered);
        }
    }
}
