package com.exoticworlds.compat.create.mixin;

import java.util.Optional;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.CarriageEntityFrame;
import com.exoticworlds.compat.create.CreateInjectionTargets;
import com.exoticworlds.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Carriage.class, remap = false)
public abstract class CarriagePairMixin {
    @Shadow
    public abstract TravellingPoint getLeadingPoint();

    @Inject(method = "getDimensional(Lnet/minecraft/resources/ResourceKey;)"
            + "Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;",
            at = @At("RETURN"))
    private void exotic_worlds$bindDimensionToCarriageEntity(ResourceKey<Level> dimension,
            CallbackInfoReturnable<Carriage.DimensionalCarriageEntity> cir) {
        ((CarriageEntityFrame) cir.getReturnValue()).exotic_worlds$bindCarriageDimension(dimension);
    }

    @WrapOperation(method = "updateContraptionAnchors",
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                    target = CreateInjectionTargets.CARRIAGE_POSITION_ANCHOR))
    private void exotic_worlds$storeAnchorInWorldFrame(Carriage.DimensionalCarriageEntity dce, Vec3 anchor,
            Operation<Void> original) {
        original.call(dce, anchor == null
                ? null
                : CreateSeamFold.canonicalOnServer(((CarriageEntityFrame) dce).exotic_worlds$carriageDimension(), anchor));
    }

    @WrapOperation(method = "getAnchorDiff",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.VEC3_DISTANCE_TO))
    private double exotic_worlds$foldAnchorSpan(Vec3 leading, Vec3 trailing, Operation<Double> original) {
        return original.call(leading,
                CreateSeamFold.nearestCopy(getLeadingPoint().node1.getLocation().dimension, leading, trailing));
    }

    @ModifyReturnValue(method = "getPositionInDimension", at = @At("RETURN"))
    private Optional<BlockPos> exotic_worlds$publishCanonicalPosition(Optional<BlockPos> position,
            @Local(argsOnly = true) ResourceKey<Level> dimension) {
        return position.map(pos -> CreateSeamFold.canonical(dimension, pos));
    }

    @ModifyExpressionValue(method = "pivoted",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.VEC3_ADD_SCALARS))
    private Vec3 exotic_worlds$pivotInPointFrame(Vec3 portalVec,
            @Local(argsOnly = true) Carriage.DimensionalCarriageEntity dce,
            @Local(argsOnly = true) ResourceKey<Level> dimension,
            @Local(ordinal = 0) Vec3 startVec) {
        return CreateSeamFold.nearestCopy(((CarriageEntityFrame) dce).exotic_worlds$carriageLevel(), dimension,
                startVec, portalVec);
    }
}
