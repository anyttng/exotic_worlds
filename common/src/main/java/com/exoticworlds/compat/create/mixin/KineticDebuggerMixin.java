package com.exoticworlds.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.kinetics.KineticDebugger;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.exoticworlds.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(KineticDebugger.class)
public abstract class KineticDebuggerMixin {
    @ModifyExpressionValue(method = "tick",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/zurrtum/create/content/kinetics/base/KineticBlockEntity;"
                            + "source:Lnet/minecraft/core/BlockPos;"))
    private static @Nullable BlockPos exotic_worlds$sourceInTheBlockEntityFrame(@Nullable BlockPos canonical,
            @Local KineticBlockEntity blockEntity) {
        if (canonical == null) {
            return null;
        }

        return CreateClientFrame.nearestCopy(blockEntity.getLevel(), canonical);
    }
}
