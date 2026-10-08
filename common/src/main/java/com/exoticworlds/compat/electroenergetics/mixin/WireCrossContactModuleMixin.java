package com.exoticworlds.compat.electroenergetics.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.george_vi.electroenergetics.simulation.infrastructure.ConnectionEntry;
import com.george_vi.electroenergetics.simulation.infrastructure.WireCrossContactModule;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WireCrossContactModule.class, remap = false)
public abstract class WireCrossContactModuleMixin {
    @Shadow
    @Final
    ServerLevel level;

    @ModifyVariable(method = "computeCrossContactFor", at = @At("STORE"), name = "bb2")
    private AABB toroidal$otherBoxBesideWire(AABB bb2, @Local(name = "bb") AABB bb) {
        return WireSpan.apply(WireSpan.wireToward(this.level, bb.getCenter(), bb2), bb2);
    }

    @ModifyVariable(method = "computeCrossContactFor", at = @At("STORE"), name = "points2")
    private List<Vec3> toroidal$otherPointsBesideWire(List<Vec3> points2, @Local(name = "bb") AABB bb,
            @Local(name = "connectionData2") ConnectionEntry other) {
        return WireSpan.apply(WireSpan.wireToward(this.level, bb.getCenter(), other.bb), points2);
    }
}
