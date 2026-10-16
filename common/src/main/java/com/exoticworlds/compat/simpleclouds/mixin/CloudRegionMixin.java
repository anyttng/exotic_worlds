package com.exoticworlds.compat.simpleclouds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.simpleclouds.CloudRegionSeat;
import com.exoticworlds.compat.simpleclouds.CloudShape;
import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;

import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;

import net.minecraft.world.phys.Vec2;

@Mixin(value = CloudRegion.class, remap = false)
public abstract class CloudRegionMixin {
    @Unique
    private static final float CURRENT_TICK = 1.0F;

    @Unique
    private @Nullable CloudRegionSeat toroidal$seat;

    @WrapOperation(method = "calculateAt", at = @At(value = "INVOKE",
            target = "Ldev/nonamecrackers2/simpleclouds/common/cloud/region/CloudRegion;circle(Ldev/nonamecrackers2/simpleclouds/common/cloud/region/CloudRegion;FF)Ldev/nonamecrackers2/simpleclouds/common/cloud/region/CloudRegion$CompositeResult;"))
    private static @Coerce Object toroidal$seatQuery(CloudRegion region, float x, float z,
            Operation<Object> original) {
        CloudShape shape = SimpleCloudsShapes.current();
        if (shape == null) {
            return original.call(region, x, z);
        }

        CloudRegionMixin seats = (CloudRegionMixin) (Object) region;
        float stretch = region.getStretch(CURRENT_TICK);
        float rotation = region.getRotation(CURRENT_TICK);
        CloudRegionSeat seat = seats.toroidal$seat;
        if (seat == null || !seat.holds(shape, stretch, rotation)) {
            seat = CloudRegionSeat.of(shape, stretch, rotation, region.createTransform(CURRENT_TICK));
            seats.toroidal$seat = seat;
        }

        Vec2 seated = seat.seated(region.getPosX(), region.getPosZ(), x, z);
        return original.call(region, seated.x, seated.y);
    }
}
