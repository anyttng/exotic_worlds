package com.exoticworlds.compat.xaero.mixin.map;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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
    private final XaeroExportAssembly toroidal$assembly = new XaeroExportAssembly();

    @ModifyVariable(method = "export", at = @At("STORE"), name = "minRegionX")
    private int toroidal$walkFromRegionX(int minRegionX, @Local(name = "minTileChunkX") int minTileChunkX,
            @Local(name = "minTileChunkZ") int minTileChunkZ, @Local(name = "maxTileChunkX") int maxTileChunkX,
            @Local(name = "maxTileChunkZ") int maxTileChunkZ) {
        this.toroidal$assembly.beginFrame(minTileChunkX, minTileChunkZ, maxTileChunkX, maxTileChunkZ);
        return this.toroidal$assembly.walking() ? 0 : minRegionX;
    }

    @ModifyVariable(method = "export", at = @At("STORE"), name = "minRegionZ")
    private int toroidal$walkFromRegionZ(int minRegionZ) {
        return this.toroidal$assembly.walking() ? 0 : minRegionZ;
    }

    @ModifyVariable(method = "export", at = @At("STORE"), name = "maxRegionX")
    private int toroidal$walkToRegionX(int maxRegionX) {
        return this.toroidal$assembly.walking() ? 0 : maxRegionX;
    }

    @ModifyVariable(method = "export", at = @At("STORE"), name = "maxRegionZ")
    private int toroidal$walkToRegionZ(int maxRegionZ) {
        return this.toroidal$assembly.walking() ? this.toroidal$assembly.lastWalkedRegion() : maxRegionZ;
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private MapRegion toroidal$fetchSourceRegion(MapProcessor processor, int caveLayer, int regionX, int regionZ,
            boolean create, Operation<MapRegion> original) {
        this.toroidal$assembly.enterRegion(regionX, regionZ);
        return original.call(processor, caveLayer, this.toroidal$assembly.regionX(), this.toroidal$assembly.regionZ(),
                create);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapLayer;regionDetectionExists(II)Z"))
    private boolean toroidal$detectSourceRegion(MapLayer layer, int regionX, int regionZ,
            Operation<Boolean> original) {
        return original.call(layer, this.toroidal$assembly.regionX(), this.toroidal$assembly.regionZ());
    }

    @WrapOperation(
            method = "export",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/MapLayer;getRegionDetection(II)Lxaero/map/file/RegionDetection;"))
    private RegionDetection toroidal$fetchSourceDetection(MapLayer layer, int regionX, int regionZ,
            Operation<RegionDetection> original) {
        return original.call(layer, this.toroidal$assembly.regionX(), this.toroidal$assembly.regionZ());
    }

    @WrapOperation(
            method = "export",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/highlight/DimensionHighlighterHandler;shouldApplyRegionHighlights(IIZ)Z"))
    private boolean toroidal$sourceRegionHighlights(DimensionHighlighterHandler handler, int regionX, int regionZ,
            boolean discovered, Operation<Boolean> original) {
        return original.call(handler, this.toroidal$assembly.regionX(), this.toroidal$assembly.regionZ(), discovered);
    }

    @WrapOperation(method = "export", at = @At(value = "NEW", target = "xaero/map/region/ExportMapRegion"))
    private ExportMapRegion toroidal$buildSourceRegion(MapDimension dimension, int regionX, int regionZ,
            int caveLayer, Registry<Biome> biomes, Operation<ExportMapRegion> original) {
        return original.call(dimension, this.toroidal$assembly.regionX(), this.toroidal$assembly.regionZ(), caveLayer,
                biomes);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/ExportMapTileChunk;getX()I"))
    private int toroidal$slotX(ExportMapTileChunk chunk, Operation<Integer> original) {
        return this.toroidal$assembly.walking() ? this.toroidal$assembly.tileChunkX(chunk) : original.call(chunk);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/ExportMapTileChunk;getZ()I"))
    private int toroidal$slotZ(ExportMapTileChunk chunk, Operation<Integer> original) {
        return this.toroidal$assembly.walking() ? this.toroidal$assembly.tileChunkZ(chunk) : original.call(chunk);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.GUI_MAP_RENDER_TEXTURED_MODAL_RECT_WITH_LIGHTING_3))
    private void toroidal$drawTileCopies(Matrix4f matrix, float x, float y, float width, float height,
            int texture, boolean hasLight, MultiTextureRenderTypeRenderer renderer, Operation<Void> original) {
        if (this.toroidal$assembly.walking()) {
            this.toroidal$assembly.draw(matrix, x, y, texture, hasLight, renderer);
        } else {
            original.call(matrix, x, y, width, height, texture, hasLight, renderer);
        }
    }
}
