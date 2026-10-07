package com.toroidalworld.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.clamp_meter.ClampMeterItem;
import com.george_vi.electroenergetics.content.linemans_stick.LinemansStickItem;
import com.george_vi.electroenergetics.foundation.nodes.NodeConnectionPoint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.electroenergetics.WireCopies;
import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = {ClampMeterItem.class, LinemansStickItem.class}, remap = false)
public abstract class WireMeterMixin {
    @WrapOperation(method = "setMetering", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double toroidal$dotsOnTheAimedCopy(Vec3 from, Vec3 to, Operation<Double> original,
            @Local(name = "point") NodeConnectionPoint point, @Local(name = "level") Level level,
            @Local(name = "pos1") LocalRef<Vec3> pos1, @Local(name = "pos2") LocalRef<Vec3> pos2) {
        WireCopies.Span span = WireCopies.around(WorldLoopAttachments.transformerOfReader(level), from, to,
                point.point(), Minecraft.getInstance().player.position());
        pos1.set(span.pos1());
        pos2.set(span.pos2());
        return original.call(span.pos1(), span.pos2());
    }
}
