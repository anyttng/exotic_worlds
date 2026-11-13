package com.exoticworlds.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.create.client.CreateClientFrame;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.trains.entity.CarriageContraptionEntityRenderer;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.world.phys.Vec3;

@Mixin(CarriageContraptionEntityRenderer.class)
public abstract class CarriageContraptionEntityRendererMixin {
    private static final String EXTRACT_RENDER_STATE = "extractRenderState("
            + "Lcom/zurrtum/create/content/trains/entity/CarriageContraptionEntity;"
            + "Lcom/zurrtum/create/client/content/trains/entity/CarriageContraptionEntityRenderer$CarriageContraptionState;"
            + "F)V";

    @ModifyExpressionValue(method = EXTRACT_RENDER_STATE,
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/content/trains/entity/CarriageBogey;"
                            + "getAnchorPosition()Lnet/minecraft/world/phys/Vec3;"),
            require = 2,
            allow = 2)
    private @Nullable Vec3 exotic_worlds$anchorInEntityFrame(@Nullable Vec3 anchor,
            @Local(argsOnly = true) CarriageContraptionEntity entity) {
        if (anchor == null) {
            return null;
        }

        return CreateClientFrame.nearestCopy(entity.position(), anchor);
    }
}
