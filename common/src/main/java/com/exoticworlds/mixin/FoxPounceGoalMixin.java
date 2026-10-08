package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.animal.Fox$FoxPounceGoal")
public class FoxPounceGoalMixin {
    // From the constructor, not shadowed off this$0 — see BeeEnterHiveGoalMixin: the outer reference is javac's, not
    // any mapping set's, so a remapping loader has nothing to resolve it to.
    @Unique
    private Fox toroidal$fox;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Fox;)V", at = @At("TAIL"))
    private void toroidal$captureFox(Fox fox, CallbackInfo ci) {
        this.toroidal$fox = fox;
    }

    @WrapOperation(
            method = "start",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 toroidal$pounceDeltaThroughSeam(double x, double y, double z, Operation<Vec3> original) {
        return SeamAim.foldDelta(this.toroidal$fox, original.call(x, y, z));
    }
}
