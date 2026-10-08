package com.exoticworlds.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamMemory;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeePollinateGoal")
public class BeePollinateGoalMixin {
    // From the constructor, not shadowed off this$0 — see BeeEnterHiveGoalMixin: the outer reference is javac's, not
    // any mapping set's, so a remapping loader has nothing to resolve it to.
    @Unique
    private Bee toroidal$bee;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V", at = @At("TAIL"))
    private void toroidal$captureBee(Bee bee, CallbackInfo ci) {
        this.toroidal$bee = bee;
    }

    @ModifyReturnValue(method = "findNearbyFlower", at = @At("RETURN"))
    private Optional<BlockPos> toroidal$foundFlowerCanonical(Optional<BlockPos> found) {
        return found.map(pos -> SeamMemory.canonical(this.toroidal$bee, pos));
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_AT_BOTTOM_CENTER_OF))
    private Vec3 toroidal$flowerThroughSeam(Vec3 flowerPos) {
        return SeamSteering.nearestCopy(this.toroidal$bee, flowerPos);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO, ordinal = 1))
    private double toroidal$hoverArrivalThroughSeam(Vec3 beePos, Vec3 hoverPos, Operation<Double> original) {
        return original.call(beePos, SeamSteering.nearestCopy(this.toroidal$bee, hoverPos));
    }
}
