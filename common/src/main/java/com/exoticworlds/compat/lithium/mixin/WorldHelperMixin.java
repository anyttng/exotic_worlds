package com.exoticworlds.compat.lithium.mixin;

import java.util.List;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.SeamEntitySearch;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;

@Mixin(targets = "net.caffeinemc.mods.lithium.common.world.WorldHelper")
public class WorldHelperMixin {
    // This line's group walk takes no level, so the fold wraps the two walks that hold one.
    @WrapMethod(method = "getEntitiesForCollision")
    private static List<Entity> toroidal$collisionGroupThroughSeam(EntityGetter view, AABB box,
            Entity collidingEntity, Operation<List<Entity>> original) {
        return view instanceof Level level
                ? SeamEntitySearch.collect(WorldLoopAttachments.transformerOf(level), box,
                        piece -> original.call(view, piece, collidingEntity))
                : original.call(view, box, collidingEntity);
    }

    @WrapMethod(method = "getOtherEntitiesForCollision")
    private static List<Entity> toroidal$otherGroupThroughSeam(EntityGetter view, AABB box, Entity collidingEntity,
            Predicate<? super Entity> entityFilter, Operation<List<Entity>> original) {
        return view instanceof Level level
                ? SeamEntitySearch.collect(WorldLoopAttachments.transformerOf(level), box,
                        piece -> original.call(view, piece, collidingEntity, entityFilter))
                : original.call(view, box, collidingEntity, entityFilter);
    }

    @WrapMethod(method = "getPushableEntities")
    private static List<Entity> toroidal$pushableThroughSeam(Level level, EntitySectionStorage<Entity> cache,
            Entity except, AABB box, @Coerce Object pushablePredicate, Operation<List<Entity>> original) {
        return SeamEntitySearch.collect(WorldLoopAttachments.transformerOf(level), box,
                piece -> original.call(level, cache, except, piece, pushablePredicate));
    }
}
