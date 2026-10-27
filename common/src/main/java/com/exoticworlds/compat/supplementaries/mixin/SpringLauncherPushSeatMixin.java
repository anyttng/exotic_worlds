package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.tiles.SpringLauncherArmBlockTile")
public class SpringLauncherPushSeatMixin {
    @ModifyExpressionValue(method = "moveCollidedEntity",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_BOUNDING_BOX))
    private AABB exotic_worlds$boxNearestArm(AABB box, @Local(argsOnly = true) Entity entity,
            @Local(argsOnly = true) AABB arm) {
        return FoldedBoxQuery.toward(WorldLoopAttachments.foldOf(entity.level()), arm.getCenter(), box);
    }
}
