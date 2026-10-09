package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.MapSeamFold;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

@Mixin(MapItemSavedData.class)
public class MapItemSavedDataMixin {
    @Shadow
    @Final
    public int centerX;

    @Shadow
    @Final
    public int centerZ;

    @Shadow
    @Final
    public ResourceKey<Level> dimension;

    @WrapMethod(method = "addDecoration")
    private void toroidal$seatDecoration(Holder<MapDecorationType> type, @Nullable LevelAccessor level, String key,
            double xPos, double zPos, double yRot, @Nullable Component name, Operation<Void> original) {
        Vec3 seated = NearestCopy.toward(MapSeamFold.transformerFor(level, this.dimension), toroidal$centre(),
                new Vec3(xPos, 0.0, zPos));
        original.call(type, level, key, seated.x, seated.z, yRot, name);
    }

    @ModifyVariable(method = "toggleBanner", at = @At("STORE"), ordinal = 0)
    private double toroidal$foldBannerX(double xPos, @Local(argsOnly = true) LevelAccessor level,
            @Local(argsOnly = true) BlockPos pos) {
        return NearestCopy.toward(MapSeamFold.transformerFor(level, this.dimension), Direction.Axis.X,
                toroidal$centre(), Vec3.atBottomCenterOf(pos));
    }

    @ModifyVariable(method = "toggleBanner", at = @At("STORE"), ordinal = 1)
    private double toroidal$foldBannerZ(double zPos, @Local(argsOnly = true) LevelAccessor level,
            @Local(argsOnly = true) BlockPos pos) {
        return NearestCopy.toward(MapSeamFold.transformerFor(level, this.dimension), Direction.Axis.Z,
                toroidal$centre(), Vec3.atBottomCenterOf(pos));
    }

    @Unique
    private Vec3 toroidal$centre() {
        return new Vec3(this.centerX, 0.0, this.centerZ);
    }
}
