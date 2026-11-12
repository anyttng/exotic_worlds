package com.exoticworlds.compat.create.mixin;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.zurrtum.create.content.contraptions.chassis.ChassisBlockEntity;
import com.exoticworlds.compat.create.ChassisWalkFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(ChassisBlockEntity.class)
public class ChassisBlockEntityMixin {
    @ModifyExpressionValue(
            method = {"addAttachedChasses", "getIncludedBlockPositionsLinear", "getIncludedBlockPositionsRadial"},
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/zurrtum/create/content/contraptions/chassis/ChassisBlockEntity;"
                            + "worldPosition:Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$walkFromTheCallersFrame(BlockPos worldPosition) {
        return ChassisWalkFrame.fold(worldPosition);
    }

    @WrapMethod(method = "collectChassisGroup")
    private List<ChassisBlockEntity> exotic_worlds$collectInTheGroupOriginsFrame(
            Operation<List<ChassisBlockEntity>> original) {
        BlockEntity self = (BlockEntity) (Object) this;
        return ChassisWalkFrame.withAnchor(self.getLevel(), self.getBlockPos(), original::call);
    }
}
