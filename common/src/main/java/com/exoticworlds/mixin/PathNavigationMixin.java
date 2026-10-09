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
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedCopies;
import com.exoticworlds.engine.fold.SeamDelta;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    private Set<BlockPos> toroidal$targetsThroughSeam(Set<BlockPos> targets) {
        WorldFold transformer = toroidal$wrappedTransformer();
        if (transformer == null || targets.isEmpty()) {
            return targets;
        }

        BlockPos from = this.mob.blockPosition();
        return FoldedCopies.of(targets, target -> transformer.nearestCopy(from, target));
    }

    @WrapOperation(method = "followThePath", at = @At(value = "INVOKE", target = MATH_ABS, ordinal = 0))
    private double toroidal$nodeDistanceX(double delta, Operation<Double> original, @Local Vec3i node) {
        return toroidal$nodeDistance(Direction.Axis.X, delta, node, original);
    }

    @WrapOperation(method = "followThePath", at = @At(value = "INVOKE", target = MATH_ABS, ordinal = 2))
    private double toroidal$nodeDistanceZ(double delta, Operation<Double> original, @Local Vec3i node) {
        return toroidal$nodeDistance(Direction.Axis.Z, delta, node, original);
    }

    @Unique
    private double toroidal$nodeDistance(Direction.Axis axis, double delta, Vec3i node, Operation<Double> original) {
        WorldFold transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(delta);
        }

        Direction.Axis other = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        double nodeOffset = this.mob.position().get(axis) - delta - node.get(axis);
        double otherDelta = this.mob.position().get(other) - (node.get(other) + nodeOffset);
        Vec3 pair = axis == Direction.Axis.X ? new Vec3(delta, 0.0, otherDelta) : new Vec3(otherDelta, 0.0, delta);
        return Math.abs(SeamDelta.fold(transformer, pair).get(axis));
    }

    @WrapOperation(
            method = "shouldRecomputePath",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$replanRangeThroughSeam(BlockPos changedPos, Position middlePos, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.mob, changedPos, middlePos, distance);
    }

    @Override
    public void toroidal$shiftBy(int shiftX, int shiftZ) {
        if (this.targetPos != null) {
            this.targetPos = this.targetPos.offset(shiftX, 0, shiftZ);
        }

        this.lastStuckCheckPos = this.lastStuckCheckPos.add(shiftX, 0, shiftZ);
        if (!Vec3i.ZERO.equals(this.timeoutCachedNode)) {
            this.timeoutCachedNode = this.timeoutCachedNode.offset(shiftX, 0, shiftZ);
        }

        if (this.path != null && !this.path.isDone()) {
            ((NavigationShifter) (Object) this.path).toroidal$shiftBy(shiftX, shiftZ);
        }
    }

    @Unique
    private @Nullable WorldFold toroidal$wrappedTransformer() {
        return ((TransformerSource) this.mob).toroidal$wrappedTransformer();
    }
}
