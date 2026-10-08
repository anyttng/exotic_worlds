package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.simulation.infrastructure.detached_nodes.DetachedNodeEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = DetachedNodeEntity.class, remap = false)
public abstract class DetachedNodeEntityMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/george_vi/electroenergetics/simulation/infrastructure/InWorldNodeData;getGlobalPos()"
                    + "Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$neighbourBesideNode(Vec3 neighbour, @Local(name = "targetPos") Vec3 targetPos) {
        return WireSpan.seat(((Entity) (Object) this).level(), targetPos, neighbour);
    }
}
