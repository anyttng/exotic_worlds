package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.ultraterraforged.LapCarrier;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.ShapedChunkGenerator;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.UTFRandomState;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

// UltraTerraForged builds its generator context at the tail of this constructor; the higher priority runs this after.
@Mixin(value = ChunkMap.class, priority = 1100)
public abstract class ChunkMapMixin {
    @Shadow
    @Final
    private RandomState randomState;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void exotic_worlds$carryFoldToUltraTerraForged(CallbackInfo callback,
            @Local(argsOnly = true) ChunkGenerator generator) {
        WorldFold fold = ShapedChunkGenerator.wrappedFoldOf(generator);
        Object state = this.randomState;
        if (fold == null || !(state instanceof UTFRandomState ultraterraforged)) {
            return;
        }

        GeneratorContext context = ultraterraforged.generatorContext();
        if (context != null) {
            LapCarrier.carry(context, fold);
        }
    }
}
