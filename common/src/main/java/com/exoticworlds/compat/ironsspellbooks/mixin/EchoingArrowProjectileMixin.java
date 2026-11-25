package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.spells.echoing_strikes.EchoingArrowProjectile;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EchoingArrowProjectile.class, remap = false)
public abstract class EchoingArrowProjectileMixin {
    // The first read is the null test, which a seat would dereference.
    @ModifyExpressionValue(method = "tick()V",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 1,
                    target = "Lio/redspace/ironsspellbooks/entity/spells/echoing_strikes/EchoingArrowProjectile;"
                            + "lastHomingPosition:Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$launchTowardNearestCopy(Vec3 lastHomingPosition) {
        return SeamSteering.nearestCopy((Entity) (Object) this, lastHomingPosition);
    }
}
