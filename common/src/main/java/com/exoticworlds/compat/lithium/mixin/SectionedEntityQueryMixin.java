package com.exoticworlds.compat.lithium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

@Mixin(targets = {
        "net.caffeinemc.mods.lithium.common.tracking.entity.SectionedInventoryEntityMovementTracker",
        "net.caffeinemc.mods.lithium.common.tracking.entity.SectionedItemEntityMovementTracker"
})
public class SectionedEntityQueryMixin {
    @WrapOperation(
            method = "getEntities(Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_BOUNDING_BOX))
    private AABB toroidal$entityBoxTowardQuery(Entity entity, Operation<AABB> original,
            @Local(argsOnly = true) AABB query) {
        return FoldedBoxQuery.toward(((TransformerSource) entity).toroidal$wrappedTransformer(), query.getCenter(),
                original.call(entity));
    }
}
