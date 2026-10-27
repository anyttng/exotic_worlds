package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.SeamDelta;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {
    @ModifyExpressionValue(
            method = "fillEntityList",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 exotic_worlds$entityOffsetInScanFrame(Vec3 offset, @Local(argsOnly = true) Level level) {
        return SeamDelta.fold(WorldLoopAttachments.foldOf(level), offset);
    }

    @ModifyExpressionValue(
            method = "fillEntityList",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$paintingOffsetInScanFrame(BlockPos offset, @Local(argsOnly = true) Level level) {
        return SeamDelta.fold(WorldLoopAttachments.foldOf(level), offset);
    }
}
