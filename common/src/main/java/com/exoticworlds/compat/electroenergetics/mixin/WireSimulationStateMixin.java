package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.george_vi.electroenergetics.simulation.infrastructure.WireSimulationState;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.core.Position;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WireSimulationState.class, remap = false)
public abstract class WireSimulationStateMixin {
    @Shadow
    @Final
    public Level level;

    @ModifyVariable(method = {"addConnection", "addCatenaryConnection", "relocateConnection"},
            at = @At("STORE"), name = "pos2")
    private Vec3 toroidal$secondEndBesideFirst(Vec3 pos2, @Local(name = "pos1") Vec3 pos1) {
        return WireSpan.seat(this.level, pos1, pos2);
    }

    @WrapOperation(method = {"addConnection", "removeConnection", "addCatenaryConnection", "relocateConnection"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/core/Position;)Lnet/minecraft/core/SectionPos;"))
    private SectionPos toroidal$canonicalSection(Position point, Operation<SectionPos> original) {
        return WireSpan.section(this.level, original.call(point));
    }

    @ModifyExpressionValue(method = "relocateConnection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;asLong(III)J"))
    private long toroidal$canonicalSectionKey(long section) {
        return WireSpan.sectionKey(this.level, section);
    }

    @ModifyVariable(method = "getConnectionsInSection", at = @At("HEAD"), argsOnly = true)
    private long toroidal$canonicalLookup(long section) {
        return WireSpan.sectionKey(this.level, section);
    }
}
