package com.exoticworlds.compat.electroenergetics.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.george_vi.electroenergetics.simulation.infrastructure.ConnectionEntry;
import com.george_vi.electroenergetics.simulation.infrastructure.WireElectrocutionModule;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.electroenergetics.WireSpan;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = WireElectrocutionModule.class, remap = false)
public abstract class WireElectrocutionModuleMixin {
    @Shadow
    @Final
    ServerLevel level;

    @ModifyVariable(method = "computeForWires", at = @At("STORE"), name = "bb2")
    private AABB toroidal$wireBoxBesideEntity(AABB bb2, @Local(name = "bb1") AABB entityBox) {
        return WireSpan.apply(WireSpan.wireToward(this.level, entityBox.getCenter(), bb2), bb2);
    }

    @ModifyVariable(method = "computeForWires", at = @At("STORE"), name = "points")
    private List<Vec3> toroidal$wirePointsBesideEntity(List<Vec3> points, @Local(name = "bb1") AABB entityBox,
            @Local(name = "connectionEntry") ConnectionEntry wire) {
        return WireSpan.apply(WireSpan.wireToward(this.level, entityBox.getCenter(), wire.dangerousBB), points);
    }
}
