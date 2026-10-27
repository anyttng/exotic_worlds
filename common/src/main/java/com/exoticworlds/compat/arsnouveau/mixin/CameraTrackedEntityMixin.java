package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.bawnorton.mixinsquared.TargetHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net/minecraft/server/level/ChunkMap$TrackedEntity", priority = 1200)
public abstract class CameraTrackedEntityMixin {
    @Shadow
    @Final
    Entity entity;

    @WrapOperation(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    @TargetHandler(mixin = "com.hollingsworth.arsnouveau.common.mixin.camera.TrackedEntityMixin", name = "an$modifyFlag")
    private Vec3 exotic_worlds$cameraDeltaThroughSeam(Vec3 cameraPosition, Vec3 entityPosition,
            Operation<Vec3> original) {
        return original.call(NearestCopy.toward(((FoldSource) this.entity).exotic_worlds$wrappedFold(),
                entityPosition, cameraPosition), entityPosition);
    }
}
