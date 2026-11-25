package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.loot.BossLootHandler;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

@Mixin(value = BossLootHandler.class, remap = false)
public abstract class BossLootHandlerMixin {
    @ModifyExpressionValue(method = "spawnPreparedDrops(Lnet/minecraft/world/entity/Mob;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$playerBesideMob(Vec3 player, @Local(argsOnly = true) Mob mob) {
        return SeamSteering.nearestCopy(mob, player);
    }
}
