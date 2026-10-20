package com.exoticworlds.compat.xaero.mixin.map;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroWriterFold;

import net.minecraft.world.level.ChunkPos;

import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.pool.MapTilePool;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;

@Mixin(value = MapWriter.class, remap = false)
public abstract class MapWriterMixin {
    @Shadow
    private int startTileChunkX;
    @Shadow
    private int startTileChunkZ;
    @Shadow
    private int endTileChunkX;
    @Shadow
    private int endTileChunkZ;

    @Unique
    private static final String INSIDE_X = "Lxaero/map/MapWriter;insideX:I";
    @Unique
    private static final String INSIDE_Z = "Lxaero/map/MapWriter;insideZ:I";
    @Unique
    private static final String SET_TILE = "Lxaero/map/region/MapTileChunk;setTile(IILxaero/map/region/MapTile;"
            + "Lxaero/map/cache/BlockStateShortShapeCache;Lxaero/map/MapProcessor;)V";
    @Unique
    private static final String WRITE_CHUNK = "Lxaero/map/MapWriter;writeChunk(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Registry;IZLnet/minecraft/core/Registry;Lxaero/map/region/OverlayManager;ZZZZZLnet/minecraft/core/BlockPos$MutableBlockPos;Lxaero/map/biome/BlockTintProvider;IIIIIIIIILxaero/map/region/MapUpdateFastConfig;)Z";
    @Unique
    private static final int CHUNK_X_ARG = 8;
    @Unique
    private static final int CHUNK_Z_ARG = 9;
    @Unique
    private static final int TILE_CHUNK_X_INDEX = 16;
    @Unique
    private static final int TILE_CHUNK_Z_INDEX = 17;
    @Unique
    private static final int TILE_CHUNK_LOCAL_X_INDEX = 18;
    @Unique
    private static final int TILE_CHUNK_LOCAL_Z_INDEX = 19;

    @Unique
    private final XaeroWriterFold exotic_worlds$fold = new XaeroWriterFold();

    @Inject(method = "onRender", at = @At("HEAD"))
    private void exotic_worlds$resolveRenderShape(CallbackInfo ci) {
        this.exotic_worlds$fold.beginRender();
    }

    @ModifyArg(method = "writeMap", at = @At(value = "INVOKE", target = WRITE_CHUNK), index = TILE_CHUNK_X_INDEX)
    private int exotic_worlds$foldTileChunkX(int tileChunkX, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return this.exotic_worlds$fold.tileX(tileChunkX, chunkX, chunkZ);
    }

    @ModifyArg(method = "writeMap", at = @At(value = "INVOKE", target = WRITE_CHUNK), index = TILE_CHUNK_Z_INDEX)
    private int exotic_worlds$foldTileChunkZ(int tileChunkZ, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return this.exotic_worlds$fold.tileZ(tileChunkZ, chunkX, chunkZ);
    }

    @ModifyArg(method = "writeMap", at = @At(value = "INVOKE", target = WRITE_CHUNK), index = TILE_CHUNK_LOCAL_X_INDEX)
    private int exotic_worlds$foldTileChunkLocalX(int tileChunkLocalX, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return this.exotic_worlds$fold.tileInRegionX(tileChunkLocalX, chunkX, chunkZ);
    }

    @ModifyArg(method = "writeMap", at = @At(value = "INVOKE", target = WRITE_CHUNK), index = TILE_CHUNK_LOCAL_Z_INDEX)
    private int exotic_worlds$foldTileChunkLocalZ(int tileChunkLocalZ, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return this.exotic_worlds$fold.tileInRegionZ(tileChunkLocalZ, chunkX, chunkZ);
    }

    @Inject(method = "writeChunk", at = @At("HEAD"))
    private void exotic_worlds$foldTilePosition(CallbackInfoReturnable<Boolean> cir,
            @Local(argsOnly = true, ordinal = CHUNK_X_ARG) int chunkX,
            @Local(argsOnly = true, ordinal = CHUNK_Z_ARG) int chunkZ) {
        this.exotic_worlds$fold.enterChunk(chunkX, chunkZ);
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.GETFIELD),
            slice = @Slice(to = @At(value = "INVOKE", target = SET_TILE)))
    private int exotic_worlds$tileInsideX(int raw) {
        return this.exotic_worlds$fold.insideX(raw);
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_Z, opcode = Opcodes.GETFIELD),
            slice = @Slice(to = @At(value = "INVOKE", target = SET_TILE)))
    private int exotic_worlds$tileInsideZ(int raw) {
        return this.exotic_worlds$fold.insideZ(raw);
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.GETFIELD),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = SET_TILE),
                    to = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int exotic_worlds$bufferTriggerX(int raw) {
        return this.exotic_worlds$fold.lastInsideX(raw);
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_Z, opcode = Opcodes.GETFIELD),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = SET_TILE),
                    to = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int exotic_worlds$bufferTriggerZ(int raw) {
        return this.exotic_worlds$fold.lastInsideZ(raw);
    }

    @ModifyExpressionValue(
            method = "updateBottomRightTile",
            at = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.GETFIELD))
    private int exotic_worlds$slopeInsideX(int raw) {
        return this.exotic_worlds$fold.insideX(raw);
    }

    @ModifyExpressionValue(
            method = "updateBottomRightTile",
            at = @At(value = "FIELD", target = INSIDE_Z, opcode = Opcodes.GETFIELD))
    private int exotic_worlds$slopeInsideZ(int raw) {
        return this.exotic_worlds$fold.insideZ(raw);
    }

    @WrapOperation(
            method = "writeChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/pool/MapTilePool;get(Ljava/lang/String;II)Lxaero/map/region/MapTile;"))
    private MapTile exotic_worlds$canonicalTileChunk(MapTilePool pool, String dimension, int chunkX, int chunkZ,
            Operation<MapTile> original) {
        ChunkPos canonical = this.exotic_worlds$fold.canonicalChunk(chunkX, chunkZ);
        return original.call(pool, dimension, canonical.x(), canonical.z());
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "startRegionX")
    private int exotic_worlds$visitFromRegionX(int startRegionX) {
        return this.exotic_worlds$fold.visitFromRegionX(startRegionX, this.startTileChunkX, this.startTileChunkZ,
                this.endTileChunkX, this.endTileChunkZ);
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "startRegionZ")
    private int exotic_worlds$visitFromRegionZ(int startRegionZ) {
        return this.exotic_worlds$fold.visitFromRegionZ(startRegionZ);
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "endRegionX")
    private int exotic_worlds$visitToRegionX(int endRegionX) {
        return this.exotic_worlds$fold.visitToRegionX(endRegionX);
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "endRegionZ")
    private int exotic_worlds$visitToRegionZ(int endRegionZ) {
        return this.exotic_worlds$fold.visitToRegionZ(endRegionZ);
    }

    @WrapOperation(
            method = "onRender",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private MapRegion exotic_worlds$visitCanonicalRegion(MapProcessor processor, int caveLayer, int regionX, int regionZ,
            boolean create, Operation<MapRegion> original) {
        ChunkPos region = this.exotic_worlds$fold.visitedRegion(regionX, regionZ);
        return original.call(processor, caveLayer, region.x(), region.z(), create);
    }

    @WrapOperation(
            method = "onRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/LeveledRegion;setComparison(IIIII)V"))
    private void exotic_worlds$foldComparison(int x, int z, int level, int leafX, int leafZ, Operation<Void> original) {
        ChunkPos at = this.exotic_worlds$fold.comparison(x, z);
        ChunkPos leaf = this.exotic_worlds$fold.comparisonLeaf(x, z, leafX, leafZ);
        original.call(at.x(), at.z(), level, leaf.x(), leaf.z());
    }
}
