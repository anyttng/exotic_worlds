package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(SubLevelAssemblyHelper.class)
public abstract class SubLevelAssemblyHelperMixin {
    @WrapOperation(
            method = "moveOtherStuff",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/HangingEntity;calculateSupportBox()Lnet/minecraft/world/phys/AABB;"))
    private static AABB exotic_worlds$supportBesideGathered(HangingEntity entity, Operation<AABB> original,
            @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BoundingBox3ic bounds) {
        return exotic_worlds$seatBesideGathered(level, bounds, entity).apply(original.call(entity));
    }

    @WrapOperation(method = "moveOtherStuff", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private static Vec3 exotic_worlds$positionBesideGathered(Entity entity, Operation<Vec3> original,
            @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BoundingBox3ic bounds) {
        return exotic_worlds$seatBesideGathered(level, bounds, entity).apply(original.call(entity));
    }

    @Unique
    private static DeckTransformation exotic_worlds$seatBesideGathered(ServerLevel level, BoundingBox3ic bounds,
            Entity entity) {
        return WorldLoopAttachments.transformerOf(level)
                .nearestCopyTransformation(bounds.toAABB().getCenter(), entity.position());
    }
}
