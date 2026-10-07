package com.toroidalworld.compat.electroenergetics.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.wire.interaction.WireInteractionHandler;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNodeConnection;
import com.george_vi.electroenergetics.simulation.infrastructure.WireData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.electroenergetics.ElectroEnergeticsInjectionTargets;
import com.toroidalworld.compat.electroenergetics.SecondEndWire;
import com.toroidalworld.compat.electroenergetics.WireCopies;
import com.toroidalworld.core.WorldLoopAttachments;

import net.createmod.catnip.data.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WireInteractionHandler.class, remap = false)
public abstract class WireInteractionHandlerMixin {
    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.ALL_WIRE_CONNECTIONS))
    private static List<Pair<InWorldNodeConnection, WireData>> toroidal$pickPerEnd(
            Operation<List<Pair<InWorldNodeConnection, WireData>>> original, @Local(name = "mc") Minecraft mc) {
        return SecondEndWire.withSecondEnds(mc.level, original.call());
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO, ordinal = 1))
    private static double toroidal$seatPickedPass(Vec3 from, Vec3 to, Operation<Double> original,
            @Local(name = "mc") Minecraft mc, @Local(name = "wire") Pair<?, ?> wire,
            @Local(name = "pos1") LocalRef<Vec3> pos1, @Local(name = "pos2") LocalRef<Vec3> pos2) {
        WireCopies.Span span = WireCopies.from(WorldLoopAttachments.transformerOfReader(mc.level), from, to,
                SecondEndWire.end(wire));
        pos1.set(span.pos1());
        pos2.set(span.pos2());
        return original.call(span.pos1(), span.pos2());
    }

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.WIRE_POS_AT))
    private static Vec3 toroidal$highlightOnTheAimedCopy(Vec3 from, Vec3 to, float point, float sag,
            Operation<Vec3> original, @Local(name = "mc") Minecraft mc,
            @Local(name = "pos1") LocalRef<Vec3> pos1, @Local(name = "pos2") LocalRef<Vec3> pos2) {
        WireCopies.Span span = WireCopies.around(WorldLoopAttachments.transformerOfReader(mc.level), from, to,
                point, mc.player.position());
        pos1.set(span.pos1());
        pos2.set(span.pos2());
        return original.call(span.pos1(), span.pos2(), point, sag);
    }
}
