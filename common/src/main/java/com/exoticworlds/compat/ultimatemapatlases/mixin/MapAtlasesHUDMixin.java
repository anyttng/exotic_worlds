package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.ClientShapes;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.phys.Vec3;
import net.nanaky.ultimate_map_atlases.client.ui.MapAtlasesHUD;

@Mixin(MapAtlasesHUD.class)
public class MapAtlasesHUDMixin {
    @ModifyExpressionValue(
            method = "drawHudText",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$foldedReadout(Vec3 position) {
        return ClientShapes.fold(ClientShapes.current(), position);
    }
}
