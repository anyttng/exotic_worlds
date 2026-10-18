package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.AquiferCellsHolder;
import com.exoticworlds.accessors.FoldedRandomState;
import com.exoticworlds.engine.noise.AquiferCells;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

@Mixin(NoiseChunk.class)
public class NoiseChunkAquiferCellsMixin {
    @Shadow
    @Final
    private RandomState randomState;

    @Shadow
    @Final
    private Aquifer aquifer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$aquiferCellsOnTheTilingGrid(CallbackInfo callback,
            @Local(argsOnly = true) NoiseGeneratorSettings settings) {
        FoldedRandomState state = (FoldedRandomState) (Object) this.randomState;
        FoldedCompileContext context = state.exotic_worlds$compileContext();
        if (context == null || !(this.aquifer instanceof AquiferCellsHolder holder) || settings.aquifers().isEmpty()) {
            return;
        }

        AquiferCells cells = state.exotic_worlds$aquiferCells();
        if (cells == null) {
            cells = AquiferCells.compile(context, settings.aquifers().get());
            state.exotic_worlds$aquiferCells(cells);
        }

        holder.exotic_worlds$aquiferCells(cells);
    }
}
