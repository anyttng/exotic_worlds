package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.exoticworlds.compat.create.CreateContraptionFold;

import net.minecraft.world.phys.Vec3;

@Mixin(LinearActuatorBlockEntity.class)
public abstract class LinearActuatorBlockEntityMixin {
    @WrapOperation(method = "resetContraptionToOffset",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;setPos(DDD)V"))
    private void exotic_worlds$reAnchorInTheContraptionFrame(AbstractContraptionEntity moved, double x, double y,
            double z, Operation<Void> original) {
        Vec3 folded = CreateContraptionFold.inFrameOf(moved, new Vec3(x, y, z));
        original.call(moved, folded.x, folded.y, folded.z);
    }
}
