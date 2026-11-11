package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.logistics.stockTicker.StockKeeperCategoryMenu;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;

import net.minecraft.core.Position;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Mixin(StockKeeperCategoryMenu.class)
public class StockKeeperCategoryMenuMixin {
    @WrapOperation(method = "stillValid", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_CLOSER_THAN))
    private boolean exotic_worlds$keeperReachThroughSeam(Vec3 playerPosition, Position keeper, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Player player) {
        return original.call(playerPosition, SeamRange.toward(player, playerPosition, keeper), distance);
    }
}
