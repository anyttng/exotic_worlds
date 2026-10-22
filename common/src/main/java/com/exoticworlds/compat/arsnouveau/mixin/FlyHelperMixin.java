package com.exoticworlds.compat.arsnouveau.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamSteering;
import com.hollingsworth.arsnouveau.common.entity.WildenStalker;
import com.hollingsworth.arsnouveau.common.entity.goal.stalker.FlyHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.phys.Vec3;

@Mixin(value = FlyHelper.class, remap = false)
public abstract class FlyHelperMixin {
    @WrapOperation(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lcom/hollingsworth/arsnouveau/common/entity/WildenStalker;orbitOffset:Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$orbitThroughSeam(WildenStalker stalker, Operation<Vec3> original) {
        return SeamSteering.nearestCopy(stalker, original.call(stalker));
    }
}
