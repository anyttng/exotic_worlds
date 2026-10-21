package com.exoticworlds.compat.ultimatemapatlases.mixin;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.compat.ultimatemapatlases.AtlasPointSeat;
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
        Vec3 player = this.player.position();
        return Comparator.comparingDouble(
                bookmark -> this.exotic_worlds$seated(bookmark.getWorldX(), bookmark.getWorldZ(), player)
                        .distanceToSqr(player));
    }

    @WrapOperation(
            method = "updateVisibleDecoration",
            at = @At(value = "INVOKE",
                    target = "Lnet/nanaky/ultimate_map_atlases/client/screen/DecorationBookmarkButton;getWorldX()D"))
    private double exotic_worlds$bookmarkXNearView(DecorationBookmarkButton bookmark, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) int centerX, @Local(argsOnly = true, ordinal = 1) int centerZ) {
        return this.exotic_worlds$seated(original.call(bookmark), bookmark.getWorldZ(),
                new Vec3(centerX, 0.0, centerZ)).x;
    }

    @WrapOperation(
            method = "updateVisibleDecoration",
            at = @At(value = "INVOKE",
                    target = "Lnet/nanaky/ultimate_map_atlases/client/screen/DecorationBookmarkButton;getWorldZ()D"))
    private double exotic_worlds$bookmarkZNearView(DecorationBookmarkButton bookmark, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) int centerX, @Local(argsOnly = true, ordinal = 1) int centerZ) {
        return this.exotic_worlds$seated(bookmark.getWorldX(), original.call(bookmark),
                new Vec3(centerX, 0.0, centerZ)).z;
    }

    @Unique
    private Vec3 exotic_worlds$seated(double x, double z, Vec3 anchor) {
        return AtlasPointSeat.toward(this.selectedSlice.dimension(), anchor, x, z);
    }
}
