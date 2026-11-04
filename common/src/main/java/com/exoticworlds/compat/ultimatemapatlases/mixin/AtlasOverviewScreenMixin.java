package com.exoticworlds.compat.ultimatemapatlases.mixin;

import java.util.Comparator;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.compat.ultimatemapatlases.AtlasPointSeat;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.seam.MapSeamFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.nanaky.ultimate_map_atlases.client.screen.AtlasOverviewScreen;
import net.nanaky.ultimate_map_atlases.client.screen.DecorationBookmarkButton;
import net.nanaky.ultimate_map_atlases.utils.Slice;

@Mixin(AtlasOverviewScreen.class)
public abstract class AtlasOverviewScreenMixin {
    @Shadow
    @Final
    private Player player;

    @Shadow
    private Slice selectedSlice;

    @ModifyArg(
            method = "recalculateDecorationWidgets",
            at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V", ordinal = 0))
    private Comparator<DecorationBookmarkButton> exotic_worlds$byDistanceAcrossSeam(
            Comparator<DecorationBookmarkButton> byRawDistance) {
        WorldFold fold = this.exotic_worlds$fold();
        if (fold == null) {
            return byRawDistance;
        }

        Vec3 player = this.player.position();
        return Comparator.comparingDouble(
                bookmark -> AtlasPointSeat.toward(fold, player, bookmark.getWorldX(), bookmark.getWorldZ())
                        .distanceToSqr(player));
    }

    @WrapOperation(
            method = "updateVisibleDecoration",
            at = @At(value = "INVOKE",
                    target = "Lnet/nanaky/ultimate_map_atlases/client/screen/DecorationBookmarkButton;getWorldX()D"))
    private double exotic_worlds$bookmarkXNearView(DecorationBookmarkButton bookmark, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) int centerX, @Local(argsOnly = true, ordinal = 1) int centerZ) {
        double x = original.call(bookmark);
        WorldFold fold = this.exotic_worlds$fold();
        return fold == null
                ? x
                : AtlasPointSeat.toward(fold, new Vec3(centerX, 0.0, centerZ), x, bookmark.getWorldZ()).x;
    }

    @WrapOperation(
            method = "updateVisibleDecoration",
            at = @At(value = "INVOKE",
                    target = "Lnet/nanaky/ultimate_map_atlases/client/screen/DecorationBookmarkButton;getWorldZ()D"))
    private double exotic_worlds$bookmarkZNearView(DecorationBookmarkButton bookmark, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) int centerX, @Local(argsOnly = true, ordinal = 1) int centerZ) {
        double z = original.call(bookmark);
        WorldFold fold = this.exotic_worlds$fold();
        return fold == null
                ? z
                : AtlasPointSeat.toward(fold, new Vec3(centerX, 0.0, centerZ), bookmark.getWorldX(), z).z;
    }

    @Unique
    private @Nullable WorldFold exotic_worlds$fold() {
        return MapSeamFold.foldFor(null, this.selectedSlice.dimension());
    }
}
