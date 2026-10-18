package com.exoticworlds.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.exoticworlds.compat.create.BezierCurveFold;
import com.exoticworlds.compat.create.CreateSeamFold;
import com.exoticworlds.core.WorldFold;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = BezierConnection.class, remap = false)
public abstract class BezierConnectionMixin implements BezierCurveFold {
    @Shadow
    @Final
    public Couple<BlockPos> bePositions;

    @Shadow
    @Final
    public Couple<Vec3> starts;

    @Unique
    private @Nullable ResourceKey<Level> exotic_worlds$dimension;

    @Override
    public void exotic_worlds$foldCurve(@Nullable Level level, @Nullable ResourceKey<Level> dimension) {
        if (dimension != null) {
            this.exotic_worlds$dimension = dimension;
        }

        WorldFold transformer = CreateSeamFold.transformerOf(level, this.exotic_worlds$dimension);
        if (transformer == null) {
            return;
        }

        exotic_worlds$canonicaliseEnd(transformer, true);
        exotic_worlds$canonicaliseEnd(transformer, false);

        Vec3 ownerCenter = Vec3.atCenterOf(this.bePositions.getFirst());
        Vec3 rawNearEnd = this.starts.getFirst();
        Vec3 nearEnd = transformer.nearestCopy(ownerCenter, rawNearEnd);
        if (nearEnd != rawNearEnd) {
            this.starts.setFirst(nearEnd);
        }

        Vec3 rawFarEnd = this.starts.getSecond();
        Vec3 farEnd = transformer.nearestCopy(nearEnd, rawFarEnd);
        if (farEnd != rawFarEnd) {
            this.starts.setSecond(farEnd);
        }
    }

    @Unique
    private void exotic_worlds$canonicaliseEnd(WorldFold transformer, boolean first) {
        BlockPos rawEnd = this.bePositions.get(first);
        BlockPos end = transformer.fold(rawEnd);
        if (end != rawEnd) {
            this.bePositions.set(first, end);
        }
    }

    @Inject(method = "secondary", at = @At("RETURN"))
    private void exotic_worlds$foldSwappedCopy(CallbackInfoReturnable<BezierConnection> cir) {
        ((BezierCurveFold) cir.getReturnValue()).exotic_worlds$foldCurve(null, this.exotic_worlds$dimension);
    }

    @Inject(method = "clone()Lcom/simibubi/create/content/trains/track/BezierConnection;", at = @At("RETURN"))
    private void exotic_worlds$carryDimensionToCopy(CallbackInfoReturnable<BezierConnection> cir) {
        ((BezierCurveFold) cir.getReturnValue()).exotic_worlds$foldCurve(null, this.exotic_worlds$dimension);
    }
}
