package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeWanderGoal")
public class BeeWanderGoalMixin {
    // From the constructor, not shadowed off this$0 — see BeeEnterHiveGoalMixin: the outer reference is javac's, not
    // any mapping set's, so a remapping loader has nothing to resolve it to.
    @Unique
    private Bee exotic_worlds$bee;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V", at = @At("TAIL"))
    private void exotic_worlds$captureBee(Bee bee, CallbackInfo ci) {
        this.exotic_worlds$bee = bee;
    }

    @ModifyExpressionValue(
            method = "findPos",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.VEC3_AT_CENTER_OF))
    private Vec3 exotic_worlds$hiveBiasThroughSeam(Vec3 hiveCenter) {
        return SeamSteering.nearestCopy(this.exotic_worlds$bee, hiveCenter);
    }
}
