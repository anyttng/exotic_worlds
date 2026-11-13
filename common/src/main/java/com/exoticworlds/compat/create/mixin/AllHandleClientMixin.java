package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.AllHandle;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.gantry.GantryContraptionEntity;
import com.zurrtum.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.exoticworlds.compat.create.CreateContraptionFold;
import com.exoticworlds.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;
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

    @ModifyArg(method = "onFactoryPanelEffect",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState"
                            + "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockPos exotic_worlds$sourceGaugeInTheViewerFrame(BlockPos canonical) {
        return CreateClientFrame.inViewerFrame(canonical);
    }

    @ModifyExpressionValue(method = "onFactoryPanelEffect",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/infrastructure/packet/s2c/FactoryPanelEffectPacket;"
                            + "toPos()Lcom/zurrtum/create/content/logistics/factoryBoard/FactoryPanelPosition;"))
    private FactoryPanelPosition exotic_worlds$targetPanelInTheViewerFrame(FactoryPanelPosition canonical) {
        BlockPos folded = CreateClientFrame.inViewerFrame(canonical.pos());
        return folded.equals(canonical.pos()) ? canonical : new FactoryPanelPosition(folded, canonical.slot());
    }
}
