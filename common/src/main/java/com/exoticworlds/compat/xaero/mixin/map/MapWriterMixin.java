package com.exoticworlds.compat.xaero.mixin.map;

import org.jspecify.annotations.Nullable;
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
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.xaero.XaeroInjectionTargets;
import com.exoticworlds.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.Direction;
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
    private @Nullable ToroidalShape exotic_worlds$shape;
    @Unique
    private AxisCopies exotic_worlds$chunkCopiesX = AxisCopies.UNBOUNDED;
    @Unique
    private AxisCopies exotic_worlds$chunkCopiesZ = AxisCopies.UNBOUNDED;
    @Unique
    private @Nullable ChunkPos exotic_worlds$foldedTile;
    @Unique
    private int exotic_worlds$foldedTileChunkX;
    @Unique
    private int exotic_worlds$foldedTileChunkZ;
    @Unique
    private long @Nullable [] exotic_worlds$visitRegions;
    @Unique
    private boolean exotic_worlds$foldedPosition;
    @Unique
    private int exotic_worlds$insideX;
    @Unique
    private int exotic_worlds$insideZ;
    @Unique
    private int exotic_worlds$lastInsideX;
    @Unique
    private int exotic_worlds$lastInsideZ;

    @Inject(method = "onRender", at = @At("HEAD"))
    private void exotic_worlds$resolveRenderShape(CallbackInfo ci) {
        this.exotic_worlds$shape = XaeroWorldMapFold.browsedShape();
        this.exotic_worlds$chunkCopiesX = AxisCopies.ofChunks(this.exotic_worlds$shape, Direction.Axis.X);
        this.exotic_worlds$chunkCopiesZ = AxisCopies.ofChunks(this.exotic_worlds$shape, Direction.Axis.Z);
        this.exotic_worlds$foldedTile = null;
    }

    @ModifyArg(method = "writeMap", at = @At(value = "INVOKE", target = WRITE_CHUNK), index = TILE_CHUNK_X_INDEX)
    private int exotic_worlds$foldTileChunkX(int tileChunkX, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return this.exotic_worlds$shape == null ? tileChunkX : exotic_worlds$foldedTile(chunkX, chunkZ).x();
    }

    @ModifyArg(method = "writeMap", at = @At(value = "INVOKE", target = WRITE_CHUNK), index = TILE_CHUNK_Z_INDEX)
    private int exotic_worlds$foldTileChunkZ(int tileChunkZ, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return this.exotic_worlds$shape == null ? tileChunkZ : exotic_worlds$foldedTile(chunkX, chunkZ).z();
    }

    @ModifyArg(method = "writeMap", at = @At(value = "INVOKE", target = WRITE_CHUNK), index = TILE_CHUNK_LOCAL_X_INDEX)
    private int exotic_worlds$foldTileChunkLocalX(int tileChunkLocalX, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return this.exotic_worlds$shape == null
                ? tileChunkLocalX : XaeroWorldMapFold.tileChunkInRegion(exotic_worlds$foldedTile(chunkX, chunkZ).x());
    }

    @ModifyArg(method = "writeMap", at = @At(value = "INVOKE", target = WRITE_CHUNK), index = TILE_CHUNK_LOCAL_Z_INDEX)
    private int exotic_worlds$foldTileChunkLocalZ(int tileChunkLocalZ, @Local(name = "chunkX") int chunkX,
            @Local(name = "chunkZ") int chunkZ) {
        return this.exotic_worlds$shape == null
                ? tileChunkLocalZ : XaeroWorldMapFold.tileChunkInRegion(exotic_worlds$foldedTile(chunkX, chunkZ).z());
    }

    @Unique
    private ChunkPos exotic_worlds$foldedTile(int chunkX, int chunkZ) {
        ChunkPos folded = this.exotic_worlds$foldedTile;
        if (folded == null || chunkX != this.exotic_worlds$foldedTileChunkX || chunkZ != this.exotic_worlds$foldedTileChunkZ) {
            folded = XaeroWorldMapFold.tileOfChunk(this.exotic_worlds$shape, chunkX, chunkZ);
            this.exotic_worlds$foldedTile = folded;
            this.exotic_worlds$foldedTileChunkX = chunkX;
            this.exotic_worlds$foldedTileChunkZ = chunkZ;
        }

        return folded;
    }

    @Inject(method = "writeChunk", at = @At("HEAD"))
    private void exotic_worlds$foldTilePosition(CallbackInfoReturnable<Boolean> cir,
            @Local(argsOnly = true, ordinal = CHUNK_X_ARG) int chunkX,
            @Local(argsOnly = true, ordinal = CHUNK_Z_ARG) int chunkZ) {
        this.exotic_worlds$foldedPosition = this.exotic_worlds$shape != null;
        if (!this.exotic_worlds$foldedPosition) {
            return;
        }

        ChunkPos canonical = XaeroWorldMapFold.canonicalChunk(this.exotic_worlds$shape, chunkX, chunkZ);
        this.exotic_worlds$insideX = XaeroWorldMapFold.insideTile(canonical.x());
        this.exotic_worlds$insideZ = XaeroWorldMapFold.insideTile(canonical.z());
        this.exotic_worlds$lastInsideX = XaeroWorldMapFold.lastInsideTile(this.exotic_worlds$chunkCopiesX, canonical.x());
        this.exotic_worlds$lastInsideZ = XaeroWorldMapFold.lastInsideTile(this.exotic_worlds$chunkCopiesZ, canonical.z());
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.GETFIELD),
            slice = @Slice(to = @At(value = "INVOKE", target = SET_TILE)))
    private int exotic_worlds$tileInsideX(int raw) {
        return this.exotic_worlds$foldedPosition ? this.exotic_worlds$insideX : raw;
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_Z, opcode = Opcodes.GETFIELD),
            slice = @Slice(to = @At(value = "INVOKE", target = SET_TILE)))
    private int exotic_worlds$tileInsideZ(int raw) {
        return this.exotic_worlds$foldedPosition ? this.exotic_worlds$insideZ : raw;
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.GETFIELD),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = SET_TILE),
                    to = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int exotic_worlds$bufferTriggerX(int raw) {
        return this.exotic_worlds$foldedPosition ? this.exotic_worlds$lastInsideX : raw;
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_Z, opcode = Opcodes.GETFIELD),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = SET_TILE),
                    to = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int exotic_worlds$bufferTriggerZ(int raw) {
        return this.exotic_worlds$foldedPosition ? this.exotic_worlds$lastInsideZ : raw;
    }

    @ModifyExpressionValue(
            method = "updateBottomRightTile",
            at = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.GETFIELD))
    private int exotic_worlds$slopeInsideX(int raw) {
        return this.exotic_worlds$foldedPosition ? this.exotic_worlds$insideX : raw;
    }

    @ModifyExpressionValue(
            method = "updateBottomRightTile",
            at = @At(value = "FIELD", target = INSIDE_Z, opcode = Opcodes.GETFIELD))
    private int exotic_worlds$slopeInsideZ(int raw) {
        return this.exotic_worlds$foldedPosition ? this.exotic_worlds$insideZ : raw;
    }

    @WrapOperation(
            method = "writeChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/pool/MapTilePool;get(Ljava/lang/String;II)Lxaero/map/region/MapTile;"))
    private MapTile exotic_worlds$canonicalTileChunk(MapTilePool pool, String dimension, int chunkX, int chunkZ,
            Operation<MapTile> original) {
        ChunkPos canonical = XaeroWorldMapFold.canonicalChunk(this.exotic_worlds$shape, chunkX, chunkZ);
        return original.call(pool, dimension, canonical.x(), canonical.z());
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "startRegionX")
    private int exotic_worlds$visitFromRegionX(int startRegionX) {
        this.exotic_worlds$visitRegions = this.exotic_worlds$shape != null
                ? XaeroWorldMapFold.canonicalRegions(this.exotic_worlds$shape, this.startTileChunkX, this.startTileChunkZ,
                        this.endTileChunkX, this.endTileChunkZ)
                : null;
        return this.exotic_worlds$visitRegions == null ? startRegionX : 0;
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "startRegionZ")
    private int exotic_worlds$visitFromRegionZ(int startRegionZ) {
        return this.exotic_worlds$visitRegions == null ? startRegionZ : 0;
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "endRegionX")
    private int exotic_worlds$visitToRegionX(int endRegionX) {
        return this.exotic_worlds$visitRegions == null ? endRegionX : 0;
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "endRegionZ")
    private int exotic_worlds$visitToRegionZ(int endRegionZ) {
        return this.exotic_worlds$visitRegions == null ? endRegionZ : this.exotic_worlds$visitRegions.length - 1;
    }

    @WrapOperation(
            method = "onRender",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private MapRegion exotic_worlds$visitCanonicalRegion(MapProcessor processor, int caveLayer, int regionX, int regionZ,
            boolean create, Operation<MapRegion> original) {
        if (this.exotic_worlds$visitRegions == null) {
            return original.call(processor, caveLayer, regionX, regionZ, create);
        }

        long region = this.exotic_worlds$visitRegions[regionZ];
        return original.call(processor, caveLayer, ChunkPos.getX(region), ChunkPos.getZ(region), create);
    }

    @WrapOperation(
            method = "onRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/LeveledRegion;setComparison(IIIII)V"))
    private void exotic_worlds$foldComparison(int x, int z, int level, int leafX, int leafZ, Operation<Void> original) {
        if (this.exotic_worlds$shape == null) {
            original.call(x, z, level, leafX, leafZ);
            return;
        }

        ChunkPos canonical = XaeroWorldMapFold.foldComparison(this.exotic_worlds$shape, x, z);
        original.call(canonical.x(), canonical.z(), level, canonical.x(), canonical.z());
    }
}
