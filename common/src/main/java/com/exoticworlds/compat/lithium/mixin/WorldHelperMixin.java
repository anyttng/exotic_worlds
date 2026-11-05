package com.exoticworlds.compat.lithium.mixin;

import java.util.List;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFold.Folded;
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
    private static List<Entity> exotic_worlds$collisionGroupThroughSeam(EntityGetter view, AABB box,
            Entity collidingEntity, Operation<List<Entity>> original) {
        if (!(view instanceof Level level)) {
            return original.call(view, box, collidingEntity);
        }

        WorldFold fold = WorldLoopAttachments.foldOf(level);
        List<Folded<AABB>> pieces = SeamEntitySearch.piecesAcrossSeam(fold, box);
        if (pieces == null) {
            return original.call(view, box, collidingEntity);
        }

        SeamEntitySearch.Found<Entity> found = new SeamEntitySearch.Found<>(fold, box);
        for (Folded<AABB> piece : pieces) {
            found.add(original.call(view, piece.value(), collidingEntity));
        }

        return found.entities();
    }

    @WrapMethod(method = "getOtherEntitiesForCollision")
    private static List<Entity> exotic_worlds$otherGroupThroughSeam(EntityGetter view, AABB box, Entity collidingEntity,
            Predicate<? super Entity> entityFilter, Operation<List<Entity>> original) {
        if (!(view instanceof Level level)) {
            return original.call(view, box, collidingEntity, entityFilter);
        }

        WorldFold fold = WorldLoopAttachments.foldOf(level);
        List<Folded<AABB>> pieces = SeamEntitySearch.piecesAcrossSeam(fold, box);
        if (pieces == null) {
            return original.call(view, box, collidingEntity, entityFilter);
        }

        SeamEntitySearch.Found<Entity> found = new SeamEntitySearch.Found<>(fold, box);
        for (Folded<AABB> piece : pieces) {
            found.add(original.call(view, piece.value(), collidingEntity, entityFilter));
        }

        return found.entities();
    }

    @WrapMethod(method = "getPushableEntities")
    private static List<Entity> exotic_worlds$pushableThroughSeam(Level level, EntitySectionStorage<Entity> cache,
            Entity except, AABB box, @Coerce Object pushablePredicate, Operation<List<Entity>> original) {
        WorldFold fold = WorldLoopAttachments.foldOf(level);
        List<Folded<AABB>> pieces = SeamEntitySearch.piecesAcrossSeam(fold, box);
        if (pieces == null) {
            return original.call(level, cache, except, box, pushablePredicate);
        }

        SeamEntitySearch.Found<Entity> found = new SeamEntitySearch.Found<>(fold, box);
        for (Folded<AABB> piece : pieces) {
            found.add(original.call(level, cache, except, piece.value(), pushablePredicate));
        }

        return found.entities();
    }
}
