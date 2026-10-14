package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.lib.math.Pos3D;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Pos3D.class, remap = false)
public class Pos3DMixin {
    @ModifyReturnValue(method = "adjustPosition", at = @At("RETURN"))
    private Pos3D toroidal$stopOnOwnAxis(Pos3D stop, @Local(argsOnly = true) Direction direction,
            @Local(argsOnly = true) Entity entity) {
        Vec3 seated = MekanismSeam.stopOnAxis(entity.level(), (Vec3) (Object) this, direction.getAxis(),
                entity.position());
        return seated.equals(stop) ? stop : new Pos3D(seated);
    }
}
