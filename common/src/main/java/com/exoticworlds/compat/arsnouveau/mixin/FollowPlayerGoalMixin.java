package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.entity.goal.whirlisprig.FollowPlayerGoal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

@Mixin(value = FollowPlayerGoal.class, remap = false)
public abstract class FollowPlayerGoalMixin {
    @Shadow
    @Final
    private Mob entity;

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D"))
    private double exotic_worlds$followedX(Player followed, Operation<Double> original) {
        return SeamAim.nearestCoord(this.entity, followed, Direction.Axis.X, original.call(followed));
    }

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D"))
    private double exotic_worlds$followedZ(Player followed, Operation<Double> original) {
        return SeamAim.nearestCoord(this.entity, followed, Direction.Axis.Z, original.call(followed));
    }
}
