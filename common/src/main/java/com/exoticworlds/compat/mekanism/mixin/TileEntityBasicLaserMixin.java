package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

@Mixin(targets = "mekanism.common.tile.laser.TileEntityBasicLaser", remap = false)
public class TileEntityBasicLaserMixin {
    @WrapOperation(method = "onUpdateServer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;intersects(Lnet/minecraft/world/phys/AABB;)Z"))
    private boolean toroidal$refractedBeamAtNearestCopy(AABB entityBox, AABB beamBox, Operation<Boolean> original,
            @Local Level level) {
        return original.call(entityBox, MekanismSeam.nearestCopy(level, entityBox.getCenter(), beamBox));
    }
}
