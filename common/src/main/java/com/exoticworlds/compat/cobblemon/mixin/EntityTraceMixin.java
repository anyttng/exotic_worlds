package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(PlayerExtensionsKt.class)
public class EntityTraceMixin {
    @WrapOperation(
            method = "traceEntityCollision(Lnet/minecraft/world/entity/LivingEntity;FFLjava/lang/Class;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Fluid;Lnet/minecraft/world/phys/Vec3;)Lcom/cobblemon/mod/common/util/EntityTraceResult;",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_CONTAINS))
    private static boolean exotic_worlds$traceThroughSeam(AABB box, Vec3 location, Operation<Boolean> original,
            @Local(argsOnly = true) LivingEntity tracer) {
        return original.call(exotic_worlds$seated(tracer, location, box), location);
    }

    @WrapOperation(
            method = "isLookingAt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;FF)Z",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_CONTAINS))
    private static boolean exotic_worlds$lookThroughSeam(AABB box, Vec3 location, Operation<Boolean> original,
            @Local(argsOnly = true, ordinal = 0) Entity looker) {
        return original.call(exotic_worlds$seated(looker, location, box), location);
    }

    @Unique
    private static AABB exotic_worlds$seated(Entity tracer, Vec3 location, AABB box) {
        return FoldedBoxQuery.toward(((TransformerSource) tracer).exotic_worlds$wrappedTransformer(), location, box);
    }
}
