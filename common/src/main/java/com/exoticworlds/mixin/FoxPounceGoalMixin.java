package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.animal.fox.Fox$FoxPounceGoal")
public class FoxPounceGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Fox fox;

    @WrapOperation(
            method = "start",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 toroidal$pounceDeltaThroughSeam(double x, double y, double z, Operation<Vec3> original) {
        return SeamAim.foldDelta(this.fox, original.call(x, y, z));
    }
}
