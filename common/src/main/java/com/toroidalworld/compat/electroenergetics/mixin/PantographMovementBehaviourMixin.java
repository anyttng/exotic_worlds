package com.toroidalworld.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.george_vi.electroenergetics.content.railway_electrification.pantograph.PantographMovementBehaviour;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.toroidalworld.compat.electroenergetics.ElectroEnergeticsInjectionTargets;
import com.toroidalworld.compat.electroenergetics.WireSpan;

import net.minecraft.world.phys.Vec3;

@Mixin(value = PantographMovementBehaviour.class, remap = false)
public abstract class PantographMovementBehaviourMixin {
    @Unique
    private static final String WIRE_START = "toroidal$wireStart";
    private static final String BLOCK_BOTTOM_CENTER =
            "Lnet/minecraft/core/BlockPos;getBottomCenter()Lnet/minecraft/world/phys/Vec3;";

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = BLOCK_BOTTOM_CENTER, ordinal = 0))
    private Vec3 toroidal$catenaryStartBesidePantograph(Vec3 start, @Local(argsOnly = true) MovementContext context,
            @Local(name = "pantographPos") Vec3 head, @Share(WIRE_START) LocalRef<Vec3> seated) {
        seated.set(WireSpan.seatOnClientIfPresent(context.world, head, start));
        return seated.get();
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = BLOCK_BOTTOM_CENTER, ordinal = 1))
    private Vec3 toroidal$catenaryEndBesideStart(Vec3 end, @Local(argsOnly = true) MovementContext context,
            @Share(WIRE_START) LocalRef<Vec3> seated) {
        return WireSpan.seatOnClientIfPresent(context.world, seated.get(), end);
    }

    @ModifyExpressionValue(method = "tick",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.NODE_GET_POSITION, ordinal = 0))
    private Vec3 toroidal$wireStartBesidePantograph(Vec3 start, @Local(argsOnly = true) MovementContext context,
            @Local(name = "pantographPos") Vec3 head, @Share(WIRE_START) LocalRef<Vec3> seated) {
        seated.set(WireSpan.seatOnClientIfPresent(context.world, head, start));
        return seated.get();
    }

    @ModifyExpressionValue(method = "tick",
            at = @At(value = "INVOKE", target = ElectroEnergeticsInjectionTargets.NODE_GET_POSITION, ordinal = 1))
    private Vec3 toroidal$wireEndBesideStart(Vec3 end, @Local(argsOnly = true) MovementContext context,
            @Share(WIRE_START) LocalRef<Vec3> seated) {
        return WireSpan.seatOnClientIfPresent(context.world, seated.get(), end);
    }
}
