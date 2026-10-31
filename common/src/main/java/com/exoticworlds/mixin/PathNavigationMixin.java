package com.exoticworlds.mixin;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.NavigationShifter;
import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedCopies;
import com.exoticworlds.engine.fold.SeamDelta;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

@Mixin(PathNavigation.class)
public class PathNavigationMixin implements NavigationShifter {
    @Unique
    private static final String MATH_ABS = "Ljava/lang/Math;abs(D)D";

    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    protected @Nullable Path path;

    @Shadow
    private @Nullable BlockPos targetPos;

    @Shadow
    protected Vec3i timeoutCachedNode;

    @Shadow
    protected Vec3 lastStuckCheckPos;

    @ModifyVariable(
            method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At("HEAD"), argsOnly = true)
    private Set<BlockPos> exotic_worlds$targetsThroughSeam(Set<BlockPos> targets) {
        WorldFold fold = exotic_worlds$wrappedFold();
        if (fold == null || targets.isEmpty()) {
            return targets;
        }

        BlockPos from = this.mob.blockPosition();
        return FoldedCopies.of(targets, target -> fold.nearestCopy(from, target));
    }

    @WrapOperation(method = "followThePath", at = @At(value = "INVOKE", target = MATH_ABS, ordinal = 0))
    private double exotic_worlds$nodeDistanceX(double delta, Operation<Double> original, @Local Vec3i node,
            @Share("lapZ") LocalDoubleRef lapZ) {
        WorldFold fold = exotic_worlds$wrappedFold();
        if (fold == null) {
            return original.call(delta);
        }

        double nodeOffset = this.mob.getX() - delta - node.getX();
        double deltaZ = this.mob.getZ() - (node.getZ() + nodeOffset);
        Vec3 folded = SeamDelta.fold(fold, delta, deltaZ);
        lapZ.set(folded.z - deltaZ);
        return Math.abs(folded.x);
    }

    @WrapOperation(method = "followThePath", at = @At(value = "INVOKE", target = MATH_ABS, ordinal = 2))
    private double exotic_worlds$nodeDistanceZ(double delta, Operation<Double> original,
            @Share("lapZ") LocalDoubleRef lapZ) {
        return original.call(delta + lapZ.get());
    }

    @WrapOperation(
            method = "shouldRecomputePath",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean exotic_worlds$replanRangeThroughSeam(BlockPos changedPos, Position middlePos, double distance,
            Operation<Boolean> original) {
        return original.call(changedPos, SeamRange.towardCenterOf(this.mob, changedPos, middlePos), distance);
    }

    @Override
    public void exotic_worlds$shiftBy(int shiftX, int shiftZ) {
        if (this.targetPos != null) {
            this.targetPos = this.targetPos.offset(shiftX, 0, shiftZ);
        }

        this.lastStuckCheckPos = this.lastStuckCheckPos.add(shiftX, 0, shiftZ);
        if (!Vec3i.ZERO.equals(this.timeoutCachedNode)) {
            this.timeoutCachedNode = this.timeoutCachedNode.offset(shiftX, 0, shiftZ);
        }

        if (this.path != null && !this.path.isDone()) {
            ((NavigationShifter) (Object) this.path).exotic_worlds$shiftBy(shiftX, shiftZ);
        }
    }

    @Unique
    private @Nullable WorldFold exotic_worlds$wrappedFold() {
        return ((FoldSource) this.mob).exotic_worlds$wrappedFold();
    }
}
