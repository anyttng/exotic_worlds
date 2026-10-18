package com.exoticworlds.compat.supplementaries.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.blocks.TurnTableBlock")
public class TurnTableOffsetSeatMixin {
    @ModifyExpressionValue(method = "stepOn", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$positionNearestTable(Vec3 position, @Local(argsOnly = true) Level level,
            @Local(ordinal = 0) Vec3 origin) {
        return WorldLoopAttachments.transformerOf(level).nearestCopy(origin, position);
    }
}
