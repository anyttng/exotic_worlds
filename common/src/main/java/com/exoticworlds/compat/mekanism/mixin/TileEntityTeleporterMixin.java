package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.DimensionMapping;
import com.exoticworlds.engine.fold.SeamDelta;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "mekanism.common.tile.TileEntityTeleporter", remap = false)
public class TileEntityTeleporterMixin {
    @WrapOperation(
            method = "calculateEnergyCost(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/core/GlobalPos;)J",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;length(DDD)D"))
    private static double toroidal$crossDimensionDistanceTheShortWayRound(
            double xDifference,
            double yDifference,
            double zDifference,
            Operation<Double> original,
            @Local(argsOnly = true) Entity entity,
            @Local(argsOnly = true) Level targetWorld,
            @Local(argsOnly = true) GlobalPos coords) {
        Level entityWorld = entity.level();
        DimensionType entityType = entityWorld.dimensionType();
        DimensionType targetType = targetWorld.dimensionType();
        WorldFold entityFold = WorldLoopAttachments.transformerOf(entityWorld);
        WorldFold targetFold = WorldLoopAttachments.transformerOf(targetWorld);
        Vec3 target = Vec3.atLowerCornerOf(coords.pos());

        WorldFold frame;
        Vec3 difference;
        if (entityType.coordinateScale() <= targetType.coordinateScale()) {
            frame = targetFold;
            difference = DimensionMapping.map(entityFold, targetFold, entity.position(),
                    DimensionType.getTeleportationScale(entityType, targetType)).subtract(target);
        } else {
            frame = entityFold;
            difference = entity.position().subtract(DimensionMapping.map(targetFold, entityFold, target,
                    DimensionType.getTeleportationScale(targetType, entityType)));
        }

        Vec3 folded = SeamDelta.fold(frame, difference);
        return original.call(folded.x, yDifference, folded.z);
    }
}
