package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.exoticworlds.engine.noise.periodic.PeriodicSimplexSampler;
import com.exoticworlds.engine.noise.periodic.SimplexSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

@Mixin(value = Biome.class, priority = 1100)
public class BiomeGrassColorMixin {
    @Unique
    private static final double exotic_worlds$SWAMP_DARK_THRESHOLD = -0.1;

    @Unique
    private static final int exotic_worlds$SWAMP_DARK_GRASS = -11766212;

    @Unique
    private static final int exotic_worlds$SWAMP_LIGHT_GRASS = -9801671;

    @Unique
    private static final SimplexSlot exotic_worlds$SWAMP_SLOT = new SimplexSlot();

    @SuppressWarnings("removal")
    @WrapOperation(
            method = "getGrassColor(DD)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/BiomeSpecialEffects$GrassColorModifier;modifyColor(DDI)I"))
    private int exotic_worlds$periodicSwampGrass(BiomeSpecialEffects.GrassColorModifier modifier, double x,
            double z, int baseColor, Operation<Integer> original) {
        if (modifier != BiomeSpecialEffects.GrassColorModifier.SWAMP) {
            return original.call(modifier, x, z, baseColor);
        }

        WorldFold fold = exotic_worlds$getTintFold();
        if (fold == null) {
            return original.call(modifier, x, z, baseColor);
        }

        double groundValue = PeriodicSimplexSampler.sample(exotic_worlds$SWAMP_SLOT,
                (SimplexNoise) Biome.BIOME_INFO_NOISE, fold, NoiseConstants.SWAMP_GRASS_SCALE, x, z);
        return groundValue < exotic_worlds$SWAMP_DARK_THRESHOLD
                ? exotic_worlds$SWAMP_DARK_GRASS
                : exotic_worlds$SWAMP_LIGHT_GRASS;
    }

    @Unique
    private static @Nullable WorldFold exotic_worlds$getTintFold() {
        WorldFold bound = GenerationFoldContext.getContext().getWrappedFold();
        if (bound != null) {
            return bound;
        }

        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server != null && server.isSameThread()) {
            return null;
        }

        return WorldLoopAttachments.getWrappedClientBoundsFold(minecraft.level);
    }
}
