package com.exoticworlds.compat.mapfrontiers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.api.v1.ExoticWorldsApi;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.mapfrontiers.ExoticWorldsGeometry;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@Pseudo
@Mixin(targets = {
        "games.alejandrocoria.mapfrontiers.platform.NeoForgePlatformHelper",
        "games.alejandrocoria.mapfrontiers.platform.FabricPlatformHelper"
}, remap = false)
public class PlatformHelperMixin {
    @ModifyReturnValue(method = "getWorldGeometry", at = @At("RETURN"))
    private WorldGeometry exotic_worlds$serverGeometry(WorldGeometry original, @Local(argsOnly = true) Level level) {
        return ExoticWorldsApi.shapeOf(level).<WorldGeometry>map(ExoticWorldsGeometry::new).orElse(original);
    }

    @ModifyReturnValue(method = "getClientWorldGeometry", at = @At("RETURN"))
    private WorldGeometry exotic_worlds$clientGeometry(WorldGeometry original,
            @Local(argsOnly = true) ResourceKey<Level> dimension) {
        ToroidalShape shape = ClientShapes.of(dimension);
        return shape == null ? original : new ExoticWorldsGeometry(shape);
    }
}
