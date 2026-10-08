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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;

@Mixin(targets = "net.caffeinemc.mods.lithium.common.world.WorldHelper")
public class WorldHelperMixin {
    @WrapMethod(method = "getEntitiesOfEntityGroupPlusDragonPieces")
    private static List<Entity> toroidal$groupThroughSeam(Level level, EntitySectionStorage<Entity> cache,
            Entity excludedEntity, @Coerce Object entityClassGroup, AABB box, Predicate<? super Entity> entityFilter,
            Operation<List<Entity>> original) {
        return SeamEntitySearch.collect(WorldLoopAttachments.transformerOf(level), box,
                piece -> original.call(level, cache, excludedEntity, entityClassGroup, piece, entityFilter));
    }

    @WrapMethod(method = "getPushableEntities")
    private static List<Entity> toroidal$pushableThroughSeam(Level level, EntitySectionStorage<Entity> cache,
            Entity except, AABB box, @Coerce Object pushablePredicate, Operation<List<Entity>> original) {
        return SeamEntitySearch.collect(WorldLoopAttachments.transformerOf(level), box,
                piece -> original.call(level, cache, except, piece, pushablePredicate));
    }
}
