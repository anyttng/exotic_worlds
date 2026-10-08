package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamMemory;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;

@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeGoToHiveGoal")
public class BeeGoToHiveGoalMixin {
    // From the constructor, not shadowed off this$0 — see BeeEnterHiveGoalMixin: the outer reference is javac's, not
    // any mapping set's, so a remapping loader has nothing to resolve it to.
    @Unique
    private Bee toroidal$bee;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V", at = @At("TAIL"))
    private void toroidal$captureBee(Bee bee, CallbackInfo ci) {
        this.toroidal$bee = bee;
    }

    @ModifyExpressionValue(
            method = "hasReachedTarget",
            at = @At(value = "INVOKE", target = InjectionTargets.PATH_GET_TARGET))
    private BlockPos toroidal$pathTargetCanonical(BlockPos target) {
        return SeamMemory.canonical(this.toroidal$bee, target);
    }
}
