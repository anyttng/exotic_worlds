package com.exoticworlds.compat.create.mixin;

import java.lang.ref.WeakReference;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CarriageEntityFrame;
import com.exoticworlds.compat.create.CreateInjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.engine.seam.SeamSnap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.trains.entity.Carriage$DimensionalCarriageEntity", remap = false)
public abstract class CarriageAnchorMixin implements CarriageEntityFrame {
    @Unique
    private static final String COUPLE_GET_SECOND = "Lnet/createmod/catnip/data/Couple;getSecond()Ljava/lang/Object;";

    @Shadow
    public Vec3 positionAnchor;

    @Shadow
    public TrackNodeLocation pivot;

    @Shadow
    public WeakReference<?> entity;

    @Unique
    private @Nullable ResourceKey<Level> exotic_worlds$dimension;

    @WrapOperation(method = "read",
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                    target = CreateInjectionTargets.CARRIAGE_POSITION_ANCHOR))
    private void exotic_worlds$storeLoadedAnchorInWorldFrame(Carriage.DimensionalCarriageEntity dce, Vec3 anchor,
            Operation<Void> original) {
        original.call(dce,
                anchor == null ? null : CreateSeamFold.canonicalOnServer(this.exotic_worlds$dimension, anchor));
    }

    // Ordinal 2 only: the two earlier reads build the chunk lookahead, this one moves the carriage and its riders.
    @ModifyExpressionValue(method = "alignEntity",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = CreateInjectionTargets.CARRIAGE_POSITION_ANCHOR,
                    ordinal = 2))
    private Vec3 exotic_worlds$anchorForWrite(Vec3 anchor) {
        Vec3 written = exotic_worlds$anchorInClientFrame(anchor);
        Entity carriageEntity = exotic_worlds$entity();
        if (carriageEntity == null || carriageEntity.level().isClientSide()) {
            return written;
        }

        exotic_worlds$carryAboard(carriageEntity, written);
        return written;
    }

    @ModifyExpressionValue(method = "alignEntity", at = @At(value = "INVOKE", target = COUPLE_GET_SECOND))
    private Object exotic_worlds$coupledAnchorInLeadingFrame(Object coupled, @Local(ordinal = 0) Vec3 leading) {
        Entity carriageEntity = exotic_worlds$entity();
        if (carriageEntity == null || !(coupled instanceof Vec3 trailing)) {
            return coupled;
        }

        return CreateSeamFold.nearestCopy(carriageEntity.level(), leading, trailing);
    }

    @ModifyExpressionValue(method = "alignEntity",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/entity/CarriageContraptionEntity;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$entityPositionInAnchorFrame(Vec3 position) {
        Entity carriageEntity = exotic_worlds$entity();
        Vec3 anchor = this.positionAnchor;
        if (carriageEntity == null || anchor == null) {
            return position;
        }

        return CreateSeamFold.nearestCopy(carriageEntity.level(), anchor, position);
    }

    @ModifyReturnValue(method = "leadingAnchor", at = @At("RETURN"))
    private Vec3 exotic_worlds$leadingAnchorInClientFrame(Vec3 anchor) {
        return exotic_worlds$anchorInClientFrame(anchor);
    }

    @ModifyReturnValue(method = "trailingAnchor", at = @At("RETURN"))
    private Vec3 exotic_worlds$trailingAnchorInClientFrame(Vec3 anchor) {
        return exotic_worlds$anchorInClientFrame(anchor);
    }

    @ModifyExpressionValue(method = "updateCutoff", at = @At(value = "INVOKE", target = COUPLE_GET_SECOND))
    private Object exotic_worlds$trailingAnchorInLeadingFrame(Object trailing, @Local(ordinal = 0) Vec3 leading) {
        if (!(trailing instanceof Vec3 trailingAnchor)) {
            return trailing;
        }

        return exotic_worlds$inCutoffFrame(leading, trailingAnchor);
    }

    @ModifyExpressionValue(method = "updateCutoff",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.VEC3_ADD_SCALARS))
    private Vec3 exotic_worlds$pivotInLeadingAnchorFrame(Vec3 pivotLoc, @Local(ordinal = 0) Vec3 leading) {
        return exotic_worlds$inCutoffFrame(leading, pivotLoc);
    }

    @Override
    public @Nullable Level exotic_worlds$carriageLevel() {
        Entity carriageEntity = exotic_worlds$entity();
        return carriageEntity == null ? null : carriageEntity.level();
    }

    @Override
    public @Nullable ResourceKey<Level> exotic_worlds$carriageDimension() {
        return this.exotic_worlds$dimension;
    }

    @Override
    public void exotic_worlds$bindCarriageDimension(ResourceKey<Level> dimension) {
        this.exotic_worlds$dimension = dimension;
    }

    @Unique
    private Vec3 exotic_worlds$inCutoffFrame(Vec3 anchor, Vec3 target) {
        return CreateSeamFold.nearestCopy(exotic_worlds$carriageLevel(),
                this.pivot == null ? null : this.pivot.getDimension(), anchor, target);
    }

    @Unique
    private void exotic_worlds$carryAboard(Entity carriage, Vec3 written) {
        DeckTransformation lap = CreateSeamFold.nearestCopyTransformation(carriage.level(), written,
                carriage.position());
        if (lap.isIdentity()) {
            return;
        }

        for (Entity passenger : carriage.getPassengers()) {
            SeamSnap.withPassengers(passenger, lap);
        }

        if (carriage instanceof ContraptionColliderAccessor colliders) {
            for (Entity aboard : colliders.exotic_worlds$collidingEntities().keySet()) {
                if (aboard.isPassenger() || aboard instanceof Player) {
                    continue;
                }

                SeamSnap.withPassengers(aboard, lap);
            }
        }
    }

    @Unique
    private @Nullable Vec3 exotic_worlds$anchorInClientFrame(@Nullable Vec3 anchor) {
        Entity carriageEntity = exotic_worlds$entity();
        if (anchor == null || carriageEntity == null || !carriageEntity.level().isClientSide()) {
            return anchor;
        }

        return CreateSeamFold.nearestCopy(carriageEntity.level(), carriageEntity.position(), anchor);
    }

    @Unique
    private @Nullable Entity exotic_worlds$entity() {
        return this.entity.get() instanceof Entity carriageEntity ? carriageEntity : null;
    }
}
