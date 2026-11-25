package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.block.ice_spider_egg.IceSpiderEggBlock;
import io.redspace.ironsspellbooks.entity.mobs.ice_spider.IceSpiderEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

@Mixin(value = IceSpiderEggBlock.class, remap = false)
public abstract class IceSpiderEggBlockMixin {
    @Unique
    private static final String PLACE_SPIDER = "tryPlaceSpiderInWorld(Lio/redspace/ironsspellbooks/entity/mobs/"
            + "ice_spider/IceSpiderEntity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/player/Player;)Z";

    @ModifyExpressionValue(method = PLACE_SPIDER, at = @At(value = "INVOKE", target = InjectionTargets.PLAYER_GET_X))
    private double exotic_worlds$playerXBesideSpider(double x, @Local(argsOnly = true) IceSpiderEntity spider,
            @Local(argsOnly = true) Player player) {
        return SeamAim.nearestCoord(spider, player, Direction.Axis.X, x);
    }

    @ModifyExpressionValue(method = PLACE_SPIDER, at = @At(value = "INVOKE", target = InjectionTargets.PLAYER_GET_Z))
    private double exotic_worlds$playerZBesideSpider(double z, @Local(argsOnly = true) IceSpiderEntity spider,
            @Local(argsOnly = true) Player player) {
        return SeamAim.nearestCoord(spider, player, Direction.Axis.Z, z);
    }
}
