package com.exoticworlds.compat.simpleclouds.mixin;

import org.joml.Matrix2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.exoticworlds.compat.simpleclouds.CloudShape;
import com.exoticworlds.compat.simpleclouds.SimpleCloudsShapes;

import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;

import net.minecraft.world.phys.Vec2;

@Mixin(value = CloudRegion.class, remap = false)
public abstract class CloudRegionMixin {
    @Unique
    private static final float CURRENT_TICK = 1.0F;
    @Unique
    private static final int REGION_ARG = 0;
    @Unique
    private static final int X_ARG = 1;
    @Unique
    private static final int Z_ARG = 2;

    @ModifyArgs(method = "calculateAt", at = @At(value = "INVOKE",
            target = "Ldev/nonamecrackers2/simpleclouds/common/cloud/region/CloudRegion;circle(Ldev/nonamecrackers2/simpleclouds/common/cloud/region/CloudRegion;FF)Ldev/nonamecrackers2/simpleclouds/common/cloud/region/CloudRegion$CompositeResult;"))
    private static void toroidal$seatQuery(Args args) {
        CloudShape shape = SimpleCloudsShapes.current();
        if (shape == null) {
            return;
        }

        CloudRegion region = args.get(REGION_ARG);
        Matrix2f transform = region.createTransform(CURRENT_TICK);
        Vec2 seated = SimpleCloudsShapes.latticeOf(shape, transform.m00, transform.m01, transform.m10, transform.m11)
                .seated(region.getPosX(), region.getPosZ(), transform.m00, transform.m01, transform.m10,
                        transform.m11, args.get(X_ARG), args.get(Z_ARG));
        args.set(X_ARG, seated.x);
        args.set(Z_ARG, seated.y);
    }
}
