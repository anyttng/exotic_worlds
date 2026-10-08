package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.reterraforged.LapCarrier;
import com.exoticworlds.compat.reterraforged.RtfLap;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.RTFRandomState;

// ReTerraForged builds its generator context at the tail of this constructor; the higher priority seats this tail
// after its own, and still before the stronghold rings read the biome source during level construction.
@Mixin(value = ChunkMap.class, priority = 1100)
public abstract class ChunkMapMixin {
    @Shadow
    @Final
    private RandomState randomState;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$carryFoldToReTerraForged(CallbackInfo callback,
            @Local(argsOnly = true) ChunkGenerator generator) {
        WorldFold fold = ShapedChunkGenerator.wrappedTransformerOf(generator);
        Object state = this.randomState;
        if (fold == null || !(state instanceof RTFRandomState reterraforged)) {
            return;
        }

        GeneratorContext context = reterraforged.generatorContext();
        if (context == null) {
            return;
        }

        ((LapCarrier) (Object) context.generator.getHeightmap()).toroidal$carryFold(fold);
        RtfLap.activate();
    }
}
