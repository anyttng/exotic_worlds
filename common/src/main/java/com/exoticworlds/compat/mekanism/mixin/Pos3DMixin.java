package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.lib.math.Pos3D;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Pos3D.class, remap = false)
public class Pos3DMixin {
    @ModifyReturnValue(method = "adjustPosition", at = @At("RETURN"))
    private Pos3D toroidal$stopAtNearestCopy(Pos3D stop, @Local(argsOnly = true) Entity entity) {
        Vec3 copy = MekanismSeam.nearestCopy(entity.level(), (Vec3) (Object) this, stop);
        return copy.equals(stop) ? stop : new Pos3D(copy);
    }
}
