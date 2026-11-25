package com.exoticworlds.compat.vista.mixin;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.exoticworlds.engine.fold.SeamHit;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.mehvahdjukaar.vista.common.ScreenRect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.mehvahdjukaar.vista.common.mob_gaze.GazeRedirect")
public class GazeRedirectSeamMixin {
    @WrapOperation(method = "hasCameraNear",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private static double exotic_worlds$watchedNearestViewFinder(BlockPos viewFinder, Vec3i watched,
            Operation<Double> original, @Local(argsOnly = true) Level level) {
        return original.call(viewFinder, SeamRange.toward(level, viewFinder, watched));
    }

    @WrapOperation(method = "traceGazeTo", at = @At(value = "INVOKE", target = InjectionTargets.AABB_CLIP))
    private static Optional<Vec3> exotic_worlds$watchedNearestRay(AABB watched, Vec3 from, Vec3 to,
            Operation<Optional<Vec3>> original, @Local(argsOnly = true) Level level) {
        return original.call(FoldedBoxQuery.toward(WorldLoopAttachments.wrappedFoldOf(level), from, watched), from,
                to);
    }

    @WrapOperation(method = "tryHitThroughScreens",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_EQUALS))
    private static boolean exotic_worlds$hitOnCanonicalTarget(BlockPos hit, Object target,
            Operation<Boolean> original, @Local(argsOnly = true) Level level) {
        return original.call(WorldLoopAttachments.foldOf(level).fold(hit), target);
    }

    @ModifyReturnValue(method = "tryHitThroughScreens", at = @At("RETURN"))
    private static @Nullable BlockHitResult exotic_worlds$hitOnCanonicalBlock(@Nullable BlockHitResult hit,
            @Local(argsOnly = true) Level level) {
        return hit == null
                ? null
                : SeamHit.reseat(hit, WorldLoopAttachments.foldOf(level).foldOriented(hit.getBlockPos()));
    }

    @WrapOperation(method = "cameraRayThroughScreen", at = @At(value = "INVOKE",
            target = "Lnet/mehvahdjukaar/vista/common/ScreenRect;projectToLocal(Lnet/minecraft/world/phys/Vec3;)"
                    + "Lnet/minecraft/world/phys/Vec2;"))
    private static @Nullable Vec2 exotic_worlds$hitNearestScreen(ScreenRect screen, Vec3 hit,
            Operation<Vec2> original, @Local(argsOnly = true) Level level) {
        return original.call(screen, SeamRange.toward(level, screen.center(), hit));
    }
}
