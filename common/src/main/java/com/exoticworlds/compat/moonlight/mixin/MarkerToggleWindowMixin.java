package com.exoticworlds.compat.moonlight.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.moonlight.MoonlightMapFold;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

@Mixin(value = MapItemSavedData.class, priority = 1100)
public class MarkerToggleWindowMixin {
    private static final String TOGGLE = "ml$toggleCustomDecoration";

    @ModifyVariable(method = TOGGLE, at = @At("STORE"), ordinal = 0)
    private double toroidal$seatToggleX(double x, @Local(argsOnly = true) LevelAccessor level,
            @Local(argsOnly = true) BlockPos pos) {
        return toroidal$seated(level, pos, Direction.Axis.X);
    }

    @ModifyVariable(method = TOGGLE, at = @At("STORE"), ordinal = 1)
    private double toroidal$seatToggleZ(double z, @Local(argsOnly = true) LevelAccessor level,
            @Local(argsOnly = true) BlockPos pos) {
        return toroidal$seated(level, pos, Direction.Axis.Z);
    }

    @Unique
    private double toroidal$seated(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        MapItemSavedData data = (MapItemSavedData) (Object) this;
        return NearestCopy.toward(MoonlightMapFold.of(level, data), axis, new Vec3(data.centerX, 0.0, data.centerZ),
                Vec3.atBottomCenterOf(pos));
    }
}
