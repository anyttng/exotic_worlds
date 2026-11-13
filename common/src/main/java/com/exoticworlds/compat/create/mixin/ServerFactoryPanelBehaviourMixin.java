package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.logistics.factoryBoard.ServerFactoryPanelBehaviour;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(ServerFactoryPanelBehaviour.class)
public abstract class ServerFactoryPanelBehaviourMixin {
    private static final int RANGE_TESTS = 3;

    @WrapOperation(method = "moveTo",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN),
            require = RANGE_TESTS)
    private boolean exotic_worlds$foldRelocationRange(BlockPos stored, Vec3i destination, double range,
            Operation<Boolean> original) {
        ServerFactoryPanelBehaviour panel = (ServerFactoryPanelBehaviour) (Object) this;
        return original.call(CreateSeamFold.nearestCopy(panel.getLevel(), destination, stored), destination,
                range);
    }
}
