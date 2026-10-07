package com.toroidalworld.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.client.WireEffects;
import com.george_vi.electroenergetics.foundation.WirePoints;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.client.engine.ClientFrame;
import com.toroidalworld.compat.electroenergetics.WireCopies;
import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WireEffects.class, remap = false)
public abstract class WireEffectsMixin {
    private static final String WIRE_POINT =
            "Lcom/george_vi/electroenergetics/foundation/WirePoints;get(I)Lnet/minecraft/world/phys/Vec3;";

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO, ordinal = 0))
    private static double toroidal$wireInOnePiece(Vec3 from, Vec3 to, Operation<Double> original,
            @Local(name = "mc") Minecraft mc, @Local(name = "pos2") LocalRef<Vec3> pos2) {
        Vec3 seated = WireCopies.from(WorldLoopAttachments.transformerOfReader(mc.level), from, to,
                WireCopies.FIRST_END).pos2();
        pos2.set(seated);
        return original.call(from, seated);
    }

    @WrapOperation(method = "spawnDrippingWater", at = @At(value = "INVOKE", target = WIRE_POINT, ordinal = 0))
    private static Vec3 toroidal$dripNearestThePlayer(WirePoints points, int index, Operation<Vec3> original) {
        return ClientFrame.nearestToPlayer(original.call(points, index));
    }

    @WrapOperation(method = "spawnDrippingWater", at = @At(value = "INVOKE", target = WIRE_POINT, ordinal = 1))
    private static Vec3 toroidal$nextPointBesideIt(WirePoints points, int index, Operation<Vec3> original,
            @Local(name = "point") Vec3 point) {
        return ClientFrame.nearestCopy(point, original.call(points, index));
    }
}
