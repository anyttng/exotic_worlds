package com.exoticworlds.compat.arsnouveau.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.accessors.NavigationShifter;
import com.exoticworlds.accessors.NavigationStateShifter;
import com.exoticworlds.engine.seam.SeamSteering;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.AbstractAdvancedPathNavigate;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.IStuckHandler;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.MinecoloniesAdvancedPathNavigate;
import com.hollingsworth.arsnouveau.common.entity.pathfinding.PathResult;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

@Mixin(value = MinecoloniesAdvancedPathNavigate.class, remap = false)
public abstract class AdvancedPathNavigateMixin extends AbstractAdvancedPathNavigate implements NavigationStateShifter {
    @Shadow
    private PathResult<?> pathResult;

    @Shadow
    private BlockPos desiredPos;

    @Shadow
    private IStuckHandler stuckHandler;

    @Unique
    private int exotic_worlds$pendingShiftX;

    @Unique
    private int exotic_worlds$pendingShiftZ;

    private AdvancedPathNavigateMixin(Mob entity, Level level) {
        super(entity, level);
    }

    @WrapMethod(method = "moveToXYZ")
    private PathResult<?> exotic_worlds$targetThroughSeam(double x, double y, double z, double speedFactor,
            Operation<PathResult<?>> original) {
        BlockPos target = BlockPos.containing(x, y, z);
        BlockPos seated = SeamSteering.nearestCopy(this.ourEntity, target);
        return original.call(x + (seated.getX() - target.getX()), y, z + (seated.getZ() - target.getZ()),
                speedFactor);
    }

    @Inject(method = "setPathJob", at = @At("HEAD"))
    private void exotic_worlds$clearPendingShift(CallbackInfoReturnable<PathResult<?>> cir) {
        this.exotic_worlds$pendingShiftX = 0;
        this.exotic_worlds$pendingShiftZ = 0;
    }

    @ModifyExpressionValue(method = "processCompletedCalculationResult", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/common/entity/pathfinding/PathResult;getPath()Lnet/minecraft/world/level/pathfinder/Path;"))
    private @Nullable Path exotic_worlds$adoptInCurrentFrame(@Nullable Path computed) {
        if (computed != null && (this.exotic_worlds$pendingShiftX != 0 || this.exotic_worlds$pendingShiftZ != 0)) {
            ((NavigationShifter) (Object) computed).exotic_worlds$shiftBy(this.exotic_worlds$pendingShiftX,
                    this.exotic_worlds$pendingShiftZ);
        }

        this.exotic_worlds$pendingShiftX = 0;
        this.exotic_worlds$pendingShiftZ = 0;
        return computed;
    }

    @Override
    public void exotic_worlds$shiftOwnState(int shiftX, int shiftZ) {
        this.destination = exotic_worlds$shifted(this.destination, shiftX, shiftZ);
        this.originalDestination = exotic_worlds$shifted(this.originalDestination, shiftX, shiftZ);
        this.desiredPos = exotic_worlds$shifted(this.desiredPos, shiftX, shiftZ);
        if (this.pathResult != null && this.pathResult.isComputing()) {
            this.exotic_worlds$pendingShiftX += shiftX;
            this.exotic_worlds$pendingShiftZ += shiftZ;
        }

        if (this.stuckHandler instanceof NavigationShifter handler) {
            handler.exotic_worlds$shiftBy(shiftX, shiftZ);
        }
    }

    @Unique
    private static @Nullable BlockPos exotic_worlds$shifted(@Nullable BlockPos pos, int shiftX, int shiftZ) {
        return pos == null ? null : pos.offset(shiftX, 0, shiftZ);
    }
}
