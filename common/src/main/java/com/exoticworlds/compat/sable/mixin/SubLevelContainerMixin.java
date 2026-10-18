package com.exoticworlds.compat.sable.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.exoticworlds.compat.sable.SableSeamQuery;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SubLevelContainer.class, remap = false)
public abstract class SubLevelContainerMixin {
    @Shadow
    @Final
    private List<SubLevel> allSubLevels;

    @Shadow
    @Final
    private Level level;

    @ModifyReturnValue(method = "queryIntersecting", at = @At("RETURN"))
    private Iterable<SubLevel> exotic_worlds$intersectAcrossSeam(Iterable<SubLevel> original, BoundingBox3dc bounds) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(this.level);
        if (fold == null) {
            return original;
        }

        return () -> {
            Vec3 queryCenter = SableSeamQuery.centerOf(bounds);
            return this.allSubLevels.stream()
                    .filter(subLevel -> SableSeamQuery.intersects(fold, queryCenter, subLevel.boundingBox(), bounds))
                    .iterator();
        };
    }
}
