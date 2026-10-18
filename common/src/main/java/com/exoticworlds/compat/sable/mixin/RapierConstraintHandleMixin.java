package com.exoticworlds.compat.sable.mixin;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.sable.SableConstraintEdge;
import com.exoticworlds.compat.sable.SableConstraintEdgeHolder;
import com.exoticworlds.compat.sable.SableMotorGoal;
import com.exoticworlds.compat.sable.SableMotorGoalHolder;

@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.constraint.RapierConstraintHandle", remap = false)
public class RapierConstraintHandleMixin implements SableMotorGoalHolder, SableConstraintEdgeHolder {
    @Unique
    private @Nullable SableMotorGoal exotic_worlds$motorGoal;

    @Unique
    private @Nullable SableConstraintEdge exotic_worlds$constraintEdge;

    @Override
    public @Nullable SableMotorGoal exotic_worlds$motorGoal() {
        return this.exotic_worlds$motorGoal;
    }

    @Override
    public void exotic_worlds$motorGoal(@Nullable SableMotorGoal goal) {
        this.exotic_worlds$motorGoal = goal;
    }

    @Override
    public void exotic_worlds$constraintEdge(@Nullable SableConstraintEdge edge) {
        this.exotic_worlds$constraintEdge = edge;
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void exotic_worlds$forgetConstraint(CallbackInfo callback) {
        SableConstraintEdge edge = this.exotic_worlds$constraintEdge;
        if (edge != null) {
            edge.drop();
            this.exotic_worlds$constraintEdge = null;
        }

        this.exotic_worlds$motorGoal = null;
    }

    @WrapOperation(method = "setMotor",
            at = @At(value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/physics/impl/rapier/Rapier3D;setConstraintMotor(JJIDDDZD)V"))
    private void exotic_worlds$seatMotorGoal(long sceneHandle, long handle, int axis, double target, double stiffness,
            double damping, boolean hasForceLimit, double maxForce, Operation<Void> original) {
        SableMotorGoal goal = this.exotic_worlds$motorGoal;
        if (goal == null || !goal.record(axis, target, stiffness, damping, hasForceLimit, maxForce)) {
            original.call(sceneHandle, handle, axis, target, stiffness, damping, hasForceLimit, maxForce);
            return;
        }

        if (!goal.completed()) {
            return;
        }

        Vector3d correction = goal.seatCorrection();
        for (int linear = 0; linear < SableMotorGoal.LINEAR_AXES; linear++) {
            double shift = correction == null ? 0.0 : correction.get(linear);
            original.call(sceneHandle, handle, linear, goal.target(linear) + shift, goal.stiffness(linear),
                    goal.damping(linear), goal.forceLimited(linear), goal.maxForce(linear));
        }
    }
}
