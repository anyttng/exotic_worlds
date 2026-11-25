package com.exoticworlds.compat.ironsspellbooks.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.api.util.Utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Utils.class, remap = false)
public abstract class UtilsMixin {
    @WrapOperation(method = "checkEntityIntersecting(Lnet/minecraft/world/entity/Entity;"
            + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_CLIP))
    private static Optional<Vec3> exotic_worlds$targetBoxBesideRay(AABB box, Vec3 start, Vec3 end,
            Operation<Optional<Vec3>> original, @Local(argsOnly = true, ordinal = 0) Entity target) {
        return original.call(FoldedBoxQuery.toward(target, start, box), start, end);
    }

    @WrapMethod(method = "hasLineOfSight(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;"
            + "Lnet/minecraft/world/phys/Vec3;Z)Z")
    private static boolean exotic_worlds$sightEndBesideStart(Level level, Vec3 start, Vec3 end,
            boolean checkForCollidableEntities, Operation<Boolean> original) {
        Vec3 seatedEnd = NearestCopy.toward(WorldLoopAttachments.wrappedFoldOf(level), start, end);
        return original.call(level, start, seatedEnd, checkForCollidableEntities);
    }
}
