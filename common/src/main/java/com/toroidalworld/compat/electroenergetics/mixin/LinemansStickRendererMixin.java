package com.toroidalworld.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.linemans_stick.LinemansStickRenderer;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.client.engine.ClientFrame;
import com.toroidalworld.compat.electroenergetics.ElectroEnergeticsInjectionTargets;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = LinemansStickRenderer.class, remap = false)
public abstract class LinemansStickRendererMixin {
    @WrapOperation(method = "renderFirstPerson",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.NODE_GET_POSITION, ordinal = 1))
    private static Vec3 toroidal$secondEndBesideFirst(InWorldNode node, Level level, Operation<Vec3> original,
            @Local(name = "pos1") Vec3 pos1) {
        return ClientFrame.nearestCopy(pos1, original.call(node, level));
    }
}
