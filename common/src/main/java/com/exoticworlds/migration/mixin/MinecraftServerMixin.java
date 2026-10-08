package com.exoticworlds.migration.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.migration.FormerPlayerFiles;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void toroidal$rewriteFormerPlayerFiles(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        FormerPlayerFiles.migrate(server.getWorldPath(LevelResource.PLAYER_DATA_DIR),
                server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR));
    }
}
