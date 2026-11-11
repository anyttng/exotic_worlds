package com.exoticworlds.mixin;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.noise.FeatureRecording;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

@Mixin(PlacedFeature.class)
public class PlacedFeatureMixin {
    @WrapMethod(method = "placeWithContext")
    private boolean exotic_worlds$recordPlacedFeature(PlacementContext context, RandomSource random, BlockPos origin,
            Operation<Boolean> original) {
        FeatureRecording recording = FeatureRecording.current();
        if (recording == null) {
            return original.call(context, random, origin);
        }

        recording.enterPlaced((PlacedFeature) (Object) this);
        try {
            return original.call(context, random, origin);
        } finally {
            recording.exitPlaced();
        }
    }

    @WrapOperation(
            method = "lambda$placeWithContext$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/placement/PlacementModifier;getPositions(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Ljava/util/stream/Stream;"))
    private static Stream<BlockPos> exotic_worlds$recordModifier(PlacementModifier modifier, PlacementContext context,
            RandomSource random, BlockPos origin, Operation<Stream<BlockPos>> original) {
        Stream<BlockPos> outputs = original.call(modifier, context, random, origin);
        FeatureRecording recording = FeatureRecording.current();
        return recording != null ? recording.modifying(modifier, origin, outputs) : outputs;
    }

    @WrapOperation(
            method = "lambda$placeWithContext$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/feature/ConfiguredFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean exotic_worlds$recordFeature(ConfiguredFeature<?, ?> feature, WorldGenLevel level,
            ChunkGenerator generator, RandomSource random, BlockPos pos, Operation<Boolean> original) {
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
