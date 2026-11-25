package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.redspace.ironsspellbooks.entity.spells.echoing_strikes.EchoingSword;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EchoingSword.class, remap = false)
public abstract class EchoingSwordMixin {
    @ModifyVariable(method = "moveAndRotateTowards(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"),
            argsOnly = true)
    private Vec3 exotic_worlds$moveTowardNearestCopy(Vec3 target) {
        return SeamSteering.nearestCopy((Entity) (Object) this, target);
    }

    @ModifyExpressionValue(method = "performHit()V",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lio/redspace/ironsspellbooks/entity/spells/echoing_strikes/EchoingSword;"
                            + "lastHomingPos:Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$strikeTowardNearestCopy(Vec3 lastHomingPos) {
        return SeamSteering.nearestCopy((Entity) (Object) this, lastHomingPos);
    }
}
