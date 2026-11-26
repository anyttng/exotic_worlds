package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.seam.SeamCommandErrors;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(CloneCommands.class)
public class CloneCommandsMixin {
    @Inject(
            method = "clone",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/commands/CloneCommands$Mode;canOverlap()Z"))
    private static void exotic_worlds$refuseSourceAcrossSeam(CallbackInfoReturnable<Integer> cir,
            @Local(ordinal = 0) BoundingBox from,
            @Local(ordinal = 0) ServerLevel fromDimension) throws CommandSyntaxException {
        SeamCommandErrors.requireUnambiguousRegion(
                WorldLoopAttachments.getWrappedFold(fromDimension), from);
    }

    @WrapOperation(
            method = "clone",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;intersects(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)Z"))
    private static boolean exotic_worlds$measureOverlapThroughSeam(BoundingBox destination, BoundingBox from,
            Operation<Boolean> original, @Local(ordinal = 0) ServerLevel fromDimension) {
        WorldFold fold = WorldLoopAttachments.getWrappedFold(fromDimension);
        return fold == null
                ? original.call(destination, from)
                : fold.areRegionsOverlapping(destination, from);
    }
}
