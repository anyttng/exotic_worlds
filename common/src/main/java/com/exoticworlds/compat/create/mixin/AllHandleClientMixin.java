package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.AllHandle;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.gantry.GantryContraptionEntity;
import com.exoticworlds.compat.create.CreateContraptionFold;

import net.minecraft.world.phys.Vec3;

@Mixin(AllHandle.class)
public class AllHandleClientMixin {
    @WrapOperation(method = "onContraptionStall",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;"
                            + "handleStallInformation(DDDF)V"))
    private void exotic_worlds$stallInTheContraptionFrame(AbstractContraptionEntity contraption, double x, double y,
            double z, float angle, Operation<Void> original) {
        Vec3 folded = CreateContraptionFold.inFrameOf(contraption, new Vec3(x, y, z));
        original.call(contraption, folded.x, folded.y, folded.z, angle);
    }

    @ModifyExpressionValue(method = "onGantryContraptionUpdate",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/infrastructure/packet/s2c/GantryContraptionUpdatePacket;coord()D"))
    private double exotic_worlds$syncedCoordInTheGantryFrame(double canonicalCoord,
            @Local GantryContraptionEntity gantry) {
        return CreateContraptionFold.axisInFrameOf(gantry,
                ((GantryContraptionEntityAccessor) gantry).exotic_worlds$movementAxis().getAxis(), canonicalCoord);
    }
}
