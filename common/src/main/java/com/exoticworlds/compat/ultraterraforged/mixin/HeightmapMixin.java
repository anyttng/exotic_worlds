package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.compat.ultraterraforged.LapCarrier;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.CellPopulator;
import com.pandaismyname1.ultraterraforged.world.worldgen.climate.Climate;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.Rivermap;

@Mixin(value = Heightmap.class, remap = false)
public abstract class HeightmapMixin {
    @Unique
    private static final int TERRAIN_POPULATOR_ORDINAL = 1;

    @Shadow
    @Final
    private float terrainFrequency;

    @Shadow
    @Final
    private Levels levels;

    @WrapMethod(method = "applyContinent")
    private void exotic_worlds$bindContinent(Cell cell, float x, float z, Operation<Void> original) {
        WorldFold fold = LapCarrier.foldOf(this.levels);
        if (fold == null) {
            original.call(cell, x, z);
            return;
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope bound = frame.bind(fold)) {
            original.call(cell, frame.shiftX(x, z), frame.shiftZ(z));
        }
    }

    @WrapMethod(method = "applyTerrain")
    private void exotic_worlds$bindTerrain(Cell cell, float x, float z, Rivermap rivermap, Operation<Void> original) {
        WorldFold fold = LapCarrier.foldOf(this.levels);
        if (fold == null) {
            original.call(cell, x, z, rivermap);
            return;
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope bound = frame.bind(fold)) {
            original.call(cell, frame.shiftX(x, z), frame.shiftZ(z), rivermap);
        }
    }

    @WrapMethod(method = "applyClimate")
    private void exotic_worlds$bindClimate(Cell cell, float x, float z, Operation<Void> original) {
        WorldFold fold = LapCarrier.foldOf(this.levels);
        if (fold == null) {
            original.call(cell, x, z);
            return;
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope bound = frame.bind(fold)) {
            original.call(cell, frame.shiftX(x, z), frame.shiftZ(z));
        }
    }

    @WrapMethod(method = "applyPost")
    private void exotic_worlds$bindPost(Cell cell, float x, float z, Operation<Void> original) {
        WorldFold fold = LapCarrier.foldOf(this.levels);
        if (fold == null) {
            original.call(cell, x, z);
            return;
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope bound = frame.bind(fold)) {
            original.call(cell, frame.shiftX(x, z), frame.shiftZ(z));
        }
    }

    @WrapMethod(method = "sampleGround")
    private Cell exotic_worlds$bindGround(float x, float z, Operation<Cell> original) {
        WorldFold fold = LapCarrier.foldOf(this.levels);
        if (fold == null) {
            return original.call(x, z);
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope bound = frame.bind(fold)) {
            return original.call(frame.shiftX(x, z), frame.shiftZ(z));
        }
    }

    @WrapMethod(method = "sampleTerrain")
    private Cell exotic_worlds$bindSampledTerrain(float x, float z, Operation<Cell> original) {
        WorldFold fold = LapCarrier.foldOf(this.levels);
        if (fold == null) {
            return original.call(x, z);
        }

        RtfLap.Frame frame = RtfLap.frame();
        try (RtfLap.Frame.Scope bound = frame.bind(fold)) {
            return original.call(frame.shiftX(x, z), frame.shiftZ(z));
        }
    }

    @WrapOperation(method = "applyClimate", at = @At(value = "INVOKE",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/climate/Climate;apply"
                    + "(Lcom/pandaismyname1/ultraterraforged/world/worldgen/cell/Cell;FF)V"))
    private void exotic_worlds$compressClimate(Climate climate, Cell cell, float x, float z,
            Operation<Void> original) {
        RtfLap.Frame frame = RtfLap.boundFrame();
        double factor = LapCarrier.climateCompressionOf(this.levels);
        if (frame == null || factor == ClimateScaleCompression.NO_COMPRESSION) {
            original.call(climate, cell, x, z);
            return;
        }

        try (RtfLap.Frame.Scope compressed = frame.compress(factor)) {
            original.call(climate, cell, (float) (x * factor), (float) (z * factor));
        }
    }

    @WrapOperation(method = "applyTerrainTypes", at = @At(value = "INVOKE",
            target = "Lcom/pandaismyname1/ultraterraforged/world/worldgen/cell/CellPopulator;apply"
                    + "(Lcom/pandaismyname1/ultraterraforged/world/worldgen/cell/Cell;FF)V",
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
