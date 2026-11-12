package com.exoticworlds.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.noise.FeatureRecording;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

@Mixin(FeaturePlacer.class)
public class FeaturePlacerMixin {
    @Unique
    private static final String PLACE =
            "place(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;"
                    + "Lnet/minecraft/core/BlockPos;Z)Z";

    @WrapMethod(method = PLACE)
    private boolean exotic_worlds$recordPlacedFeature(PlacedFeature placed, RandomSource random, BlockPos origin,
            boolean biomeCheck, Operation<Boolean> original) {
        FeatureRecording recording = FeatureRecording.current();
        if (recording == null) {
            return original.call(placed, random, origin, biomeCheck);
        }

        recording.enterPlaced(placed);
        try {
            return original.call(placed, random, origin, biomeCheck);
        } finally {
            recording.exitPlaced();
        }
    }

    @WrapOperation(
            method = PLACE,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/placement/PlacementModifier;modify(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/Consumer;)V"))
    private void exotic_worlds$recordModifier(PlacementModifier modifier, PlacementContext context,
            RandomSource random, BlockPos origin, Consumer<BlockPos> output, Operation<Void> original) {
        FeatureRecording recording = FeatureRecording.current();
        if (recording == null) {
            original.call(modifier, context, random, origin, output);
            return;
        }

        FeatureRecording.Outputs outputs = recording.modifying(modifier, origin, output);
        original.call(modifier, context, random, origin, outputs);
        recording.modified(outputs);
    }

    @WrapOperation(
            method = PLACE,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/feature/Feature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean exotic_worlds$recordFeature(Feature feature, WorldGenLevel level, ChunkGenerator generator,
            RandomSource random, BlockPos pos, Operation<Boolean> original) {
        FeatureRecording recording = FeatureRecording.current();
        if (recording == null) {
            return original.call(feature, level, generator, random, pos);
        }

        recording.enterFeature();
        boolean answer = false;
        try {
            answer = original.call(feature, level, generator, random, pos);
        } finally {
            recording.exitFeature(feature, pos, answer);
        }

        return answer;
    }
}
