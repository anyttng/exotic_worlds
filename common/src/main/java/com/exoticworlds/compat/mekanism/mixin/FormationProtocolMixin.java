package com.exoticworlds.compat.mekanism.mixin;

import java.util.Iterator;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.lib.multiblock.FormationProtocol;
import mekanism.common.lib.multiblock.IMultiblock;

import net.minecraft.core.BlockPos;

@Mixin(value = FormationProtocol.class, remap = false)
public class FormationProtocolMixin {
    @Shadow
    @Final
    private IMultiblock<?> pointer;

    @WrapOperation(method = "doUpdate",
            at = @At(value = "INVOKE", target = InjectionTargets.SET_CONTAINS))
    private boolean toroidal$pointerInCuboidFrame(Set<BlockPos> locations, Object pointerPos,
            Operation<Boolean> original) {
        Iterator<BlockPos> any = locations.iterator();
        if (!any.hasNext() || !(pointerPos instanceof BlockPos pos)) {
            return original.call(locations, pointerPos);
        }

        return original.call(locations, MekanismSeam.nearestCopy(this.pointer.getLevel(), any.next(), pos));
    }
}
