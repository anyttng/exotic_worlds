package com.exoticworlds.compat.reterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.reterraforged.LapCarrier;
import com.exoticworlds.compat.reterraforged.RtfClimateCompression;
import com.exoticworlds.compat.reterraforged.RtfClimateScales;
import com.exoticworlds.compat.reterraforged.RtfLap;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.CellPopulator;
import raccoonman.reterraforged.world.worldgen.cell.climate.Climate;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Heightmap;
import raccoonman.reterraforged.world.worldgen.cell.rivermap.Rivermap;

// Binds the lap itself: its tile workers run off the thread that queued the work.
@Mixin(value = Heightmap.class, remap = false)
public abstract class HeightmapMixin implements LapCarrier {
    @Unique
    private static final int TERRAIN_POPULATOR_ORDINAL = 1;

    @Unique
    private volatile @Nullable WorldFold exotic_worlds$fold;

    @Unique
    private volatile double exotic_worlds$climateCompression = ClimateScaleCompression.NO_COMPRESSION;

    @Shadow
    @Final
    private float terrainFrequency;

    @Shadow
    @Final
    private Climate climate;

    @Override
    public @Nullable WorldFold exotic_worlds$fold() {
        return this.exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$carryFold(WorldFold fold) {
        this.exotic_worlds$climateCompression =
                RtfClimateCompression.factor(fold, (RtfClimateScales) (Object) this.climate.biomeNoise());
        RtfClimateCompression.remember(fold, this.exotic_worlds$climateCompression);
        this.exotic_worlds$fold = fold;
    }

    @Override
    public double exotic_worlds$climateCompression() {
        return this.exotic_worlds$climateCompression;
    }

    @WrapMethod(method = "apply")
    private void exotic_worlds$bindApply(Cell cell, float x, float z, boolean applyClimate, Operation<Void> original) {
        WorldFold fold = this.exotic_worlds$fold;
        if (fold == null) {
            original.call(cell, x, z, applyClimate);
            return;
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope lap = frame.bind(fold)) {
            original.call(cell, frame.shiftX(x, z), frame.shiftZ(z), applyClimate);
        }
    }

    @WrapMethod(method = "applyTerrain")
    private void exotic_worlds$bindTerrain(Cell cell, float x, float z, Operation<Void> original) {
        WorldFold fold = this.exotic_worlds$fold;
        if (fold == null) {
            original.call(cell, x, z);
            return;
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope lap = frame.bind(fold)) {
            original.call(cell, frame.shiftX(x, z), frame.shiftZ(z));
        }
    }

    @WrapMethod(method = "applyRivers")
    private void exotic_worlds$bindRivers(Cell cell, float x, float z, Rivermap rivermap, Operation<Void> original) {
        WorldFold fold = this.exotic_worlds$fold;
        if (fold == null) {
            original.call(cell, x, z, rivermap);
            return;
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope lap = frame.bind(fold)) {
            original.call(cell, frame.shiftX(x, z), frame.shiftZ(z), rivermap);
        }
    }

    @WrapMethod(method = "applyClimate")
    private void exotic_worlds$bindClimate(Cell cell, float x, float z, boolean applyClimate, Operation<Void> original) {
        WorldFold fold = this.exotic_worlds$fold;
        if (fold == null) {
            original.call(cell, x, z, applyClimate);
            return;
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope lap = frame.bind(fold)) {
            original.call(cell, frame.shiftX(x, z), frame.shiftZ(z), applyClimate);
        }
    }

    @WrapOperation(method = "applyClimate", at = @At(value = "INVOKE",
            target = "Lraccoonman/reterraforged/world/worldgen/cell/climate/Climate;apply"
                    + "(Lraccoonman/reterraforged/world/worldgen/cell/Cell;FFZ)V"))
    private void exotic_worlds$compressClimate(Climate climate, Cell cell, float x, float z, boolean applyClimate,
            Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        double factor = this.exotic_worlds$climateCompression;
        if (frame == null || factor == ClimateScaleCompression.NO_COMPRESSION) {
            original.call(climate, cell, x, z, applyClimate);
            return;
        }

        try (RtfLap.Frame.Scope compressed = frame.compress(factor)) {
            original.call(climate, cell, (float) (x * factor), (float) (z * factor), applyClimate);
        }
    }

    // The terrain populators are handed the position times the global horizontal scale.
    @WrapOperation(method = "applyTerrain", at = @At(value = "INVOKE",
            target = "Lraccoonman/reterraforged/world/worldgen/cell/CellPopulator;apply"
                    + "(Lraccoonman/reterraforged/world/worldgen/cell/Cell;FF)V",
            ordinal = TERRAIN_POPULATOR_ORDINAL))
    private void exotic_worlds$scaleTerrainLap(CellPopulator populator, Cell cell, float x, float z,
            Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        if (frame == null) {
            original.call(populator, cell, x, z);
            return;
        }

        try (RtfLap.Frame.Scope scaled = frame.scale(this.terrainFrequency, this.terrainFrequency)) {
            original.call(populator, cell, x, z);
        }
    }
}
