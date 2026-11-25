package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.spells.CastingMobAimingData;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = CastingMobAimingData.class, remap = false)
public abstract class CastingMobAimingDataMixin {

    @WrapOperation(method = "updateAim(Lnet/minecraft/world/entity/Entity;F)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 exotic_worlds$followNearestCopy(Vec3 wanted, Vec3 aim, Operation<Vec3> original,
            @Local(argsOnly = true) Entity target) {
        return SeamAim.foldDelta(target, original.call(wanted, aim));
    }

    @ModifyExpressionValue(method = "getForward(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lio/redspace/ironsspellbooks/spells/CastingMobAimingData;"
                            + "aimPosition:Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$aimAtNearestCopy(Vec3 aimPosition, @Local(argsOnly = true) Entity host) {
        return SeamSteering.nearestCopy(host, aimPosition);
    }
}
