package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.entities.PignataEntity")
public class PignataRopeHoldSeatMixin {
    @ModifyExpressionValue(
            method = { "setLeashedTo", "isYankedByLeash", "isLeashTaut", "getLeashConstraintPull" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getRopeHoldPosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$ropeNearestPignata(Vec3 rope) {
        return SeamSteering.nearestCopy((Entity) (Object) this, rope);
    }
}
