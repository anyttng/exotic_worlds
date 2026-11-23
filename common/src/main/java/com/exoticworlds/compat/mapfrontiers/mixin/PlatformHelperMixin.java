package com.exoticworlds.compat.mapfrontiers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.mapfrontiers.ExoticWorldsGeometry;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
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
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(level);
        return fold == null ? original : new ExoticWorldsGeometry(fold);
    }

    @ModifyReturnValue(method = "getClientWorldGeometry", at = @At("RETURN"))
    private WorldGeometry exotic_worlds$clientGeometry(WorldGeometry original,
            @Local(argsOnly = true) ResourceKey<Level> dimension) {
        WorldFold fold = ClientFolds.of(dimension);
        return fold == null ? original : new ExoticWorldsGeometry(fold);
    }
}
