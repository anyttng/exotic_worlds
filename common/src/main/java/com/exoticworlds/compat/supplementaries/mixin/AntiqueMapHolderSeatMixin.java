package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.misc.map_data.WeatheredHandler$WeatheredMapData")
public class AntiqueMapHolderSeatMixin {
    @WrapOperation(method = "onItemUpdate", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double exotic_worlds$holderNearestCenterX(Entity holder, Operation<Double> original,
            @Local(argsOnly = true) MapItemSavedData data) {
        return exotic_worlds$seated(data, holder, original.call(holder), holder.getZ()).x;
    }

    @WrapOperation(method = "onItemUpdate", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double exotic_worlds$holderNearestCenterZ(Entity holder, Operation<Double> original,
            @Local(argsOnly = true) MapItemSavedData data) {
        return exotic_worlds$seated(data, holder, holder.getX(), original.call(holder)).z;
    }

    @Unique
    private static Vec3 exotic_worlds$seated(MapItemSavedData data, Entity holder, double x, double z) {
        return WorldLoopAttachments.transformerOf(holder.level())
                .nearestCopy(new Vec3(data.centerX, 0.0, data.centerZ), new Vec3(x, holder.getY(), z));
    }
}
