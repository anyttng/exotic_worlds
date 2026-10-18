package com.exoticworlds.compat.xaero.mixin.map;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.exoticworlds.compat.xaero.XaeroExportAssembly;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;

import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;

import xaero.map.MapProcessor;
import xaero.map.file.RegionDetection;
import xaero.map.file.export.PNGExporter;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.region.ExportMapRegion;
import xaero.map.region.ExportMapTileChunk;
import xaero.map.region.MapLayer;
import xaero.map.region.MapRegion;
import xaero.map.world.MapDimension;

@Mixin(value = PNGExporter.class, remap = false)
public abstract class PNGExporterMixin {
    @Unique
    private final XaeroExportAssembly exotic_worlds$assembly = new XaeroExportAssembly();

    @ModifyVariable(method = "export", at = @At("STORE"), name = "minRegionX")
    private int exotic_worlds$walkFromRegionX(int minRegionX, @Local(name = "minTileChunkX") int minTileChunkX,
            @Local(name = "minTileChunkZ") int minTileChunkZ, @Local(name = "maxTileChunkX") int maxTileChunkX,
            @Local(name = "maxTileChunkZ") int maxTileChunkZ) {
        this.exotic_worlds$assembly.beginFrame(minTileChunkX, minTileChunkZ, maxTileChunkX, maxTileChunkZ);
        return this.exotic_worlds$assembly.walking() ? 0 : minRegionX;
    }

    @ModifyVariable(method = "export", at = @At("STORE"), name = "minRegionZ")
    private int exotic_worlds$walkFromRegionZ(int minRegionZ) {
        return this.exotic_worlds$assembly.walking() ? 0 : minRegionZ;
    }

    @ModifyVariable(method = "export", at = @At("STORE"), name = "maxRegionX")
    private int exotic_worlds$walkToRegionX(int maxRegionX) {
        return this.exotic_worlds$assembly.walking() ? 0 : maxRegionX;
    }

    @ModifyVariable(method = "export", at = @At("STORE"), name = "maxRegionZ")
    private int exotic_worlds$walkToRegionZ(int maxRegionZ) {
        return this.exotic_worlds$assembly.walking() ? this.exotic_worlds$assembly.lastWalkedRegion() : maxRegionZ;
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private MapRegion exotic_worlds$fetchSourceRegion(MapProcessor processor, int caveLayer, int regionX, int regionZ,
            boolean create, Operation<MapRegion> original) {
        this.exotic_worlds$assembly.enterRegion(regionX, regionZ);
        return original.call(processor, caveLayer, this.exotic_worlds$assembly.regionX(), this.exotic_worlds$assembly.regionZ(),
                create);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapLayer;regionDetectionExists(II)Z"))
    private boolean exotic_worlds$detectSourceRegion(MapLayer layer, int regionX, int regionZ,
            Operation<Boolean> original) {
        return original.call(layer, this.exotic_worlds$assembly.regionX(), this.exotic_worlds$assembly.regionZ());
    }

    @WrapOperation(
            method = "export",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/MapLayer;getRegionDetection(II)Lxaero/map/file/RegionDetection;"))
    private RegionDetection exotic_worlds$fetchSourceDetection(MapLayer layer, int regionX, int regionZ,
            Operation<RegionDetection> original) {
        return original.call(layer, this.exotic_worlds$assembly.regionX(), this.exotic_worlds$assembly.regionZ());
    }

    @WrapOperation(
            method = "export",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/highlight/DimensionHighlighterHandler;shouldApplyRegionHighlights(IIZ)Z"))
    private boolean exotic_worlds$sourceRegionHighlights(DimensionHighlighterHandler handler, int regionX, int regionZ,
            boolean discovered, Operation<Boolean> original) {
        return original.call(handler, this.exotic_worlds$assembly.regionX(), this.exotic_worlds$assembly.regionZ(), discovered);
    }

    @WrapOperation(method = "export", at = @At(value = "NEW", target = "xaero/map/region/ExportMapRegion"))
    private ExportMapRegion exotic_worlds$buildSourceRegion(MapDimension dimension, int regionX, int regionZ,
            int caveLayer, Registry<Biome> biomes, Operation<ExportMapRegion> original) {
        return original.call(dimension, this.exotic_worlds$assembly.regionX(), this.exotic_worlds$assembly.regionZ(), caveLayer,
                biomes);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/ExportMapTileChunk;getX()I"))
    private int exotic_worlds$slotX(ExportMapTileChunk chunk, Operation<Integer> original) {
        return this.exotic_worlds$assembly.walking() ? this.exotic_worlds$assembly.tileChunkX(chunk) : original.call(chunk);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/ExportMapTileChunk;getZ()I"))
    private int exotic_worlds$slotZ(ExportMapTileChunk chunk, Operation<Integer> original) {
        return this.exotic_worlds$assembly.walking() ? this.exotic_worlds$assembly.tileChunkZ(chunk) : original.call(chunk);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.GUI_MAP_RENDER_TEXTURED_MODAL_RECT_WITH_LIGHTING_3))
    private void exotic_worlds$drawTileCopies(Matrix4f matrix, float x, float y, float width, float height,
            GpuTextureView texture, boolean hasLight, MultiTextureRenderTypeRenderer renderer, Operation<Void> original) {
        if (this.exotic_worlds$assembly.walking()) {
            this.exotic_worlds$assembly.draw(matrix, x, y, texture, hasLight, renderer);
        } else {
            original.call(matrix, x, y, width, height, texture, hasLight, renderer);
        }
    }
}
