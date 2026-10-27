package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.entities.goals.UseCannonBlockGoal")
public class UseCannonGoalSeatMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 exotic_worlds$deltaFromCannonThroughSeam(Vec3 position, Vec3 cannon, Operation<Vec3> original,
            @Local Level level) {
        return original.call(WorldLoopAttachments.foldOf(level).nearestCopy(cannon, position), cannon);
    }
}
