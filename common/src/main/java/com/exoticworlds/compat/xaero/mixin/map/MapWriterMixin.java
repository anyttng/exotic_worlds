package com.exoticworlds.compat.xaero.mixin.map;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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
    private static final int CHUNK_X_ARG = 8;
    @Unique
    private static final int CHUNK_Z_ARG = 9;

    @Unique
    private long @Nullable [] toroidal$visitRegions;
    @Unique
    private boolean toroidal$foldedPosition;
    @Unique
    private int toroidal$insideX;
    @Unique
    private int toroidal$insideZ;
    @Unique
    private int toroidal$lastInsideX;
    @Unique
    private int toroidal$lastInsideZ;

    @ModifyArgs(
            method = "writeMap",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapWriter;writeChunk(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Registry;IZLnet/minecraft/core/Registry;Lxaero/map/region/OverlayManager;ZZZZZLnet/minecraft/core/BlockPos$MutableBlockPos;Lxaero/map/biome/BlockTintProvider;IIIIIIIIILxaero/map/region/MapUpdateFastConfig;)Z"))
    private void toroidal$foldWriteKeys(Args args) {
        int tileChunkX = args.get(16);
        int tileChunkZ = args.get(17);
        ChunkPos folded = XaeroWorldMapFold.tileOfChunk(args.<Integer>get(20), args.<Integer>get(21));
        if (folded.x() == tileChunkX && folded.z() == tileChunkZ) {
            return;
        }

        args.set(16, folded.x());
        args.set(17, folded.z());
        args.set(18, XaeroWorldMapFold.tileChunkInRegion(folded.x()));
        args.set(19, XaeroWorldMapFold.tileChunkInRegion(folded.z()));
    }

    @Inject(method = "writeChunk", at = @At("HEAD"))
    private void toroidal$foldTilePosition(CallbackInfoReturnable<Boolean> cir,
            @Local(argsOnly = true, ordinal = CHUNK_X_ARG) int chunkX,
            @Local(argsOnly = true, ordinal = CHUNK_Z_ARG) int chunkZ) {
        this.toroidal$foldedPosition = XaeroWorldMapFold.active();
        if (!this.toroidal$foldedPosition) {
            return;
        }

        ChunkPos canonical = XaeroWorldMapFold.canonicalChunk(chunkX, chunkZ);
        this.toroidal$insideX = XaeroWorldMapFold.insideTile(canonical.x());
        this.toroidal$insideZ = XaeroWorldMapFold.insideTile(canonical.z());
        this.toroidal$lastInsideX = XaeroWorldMapFold.lastInsideTile(
                XaeroWorldMapFold.chunkCopies(Direction.Axis.X), canonical.x());
        this.toroidal$lastInsideZ = XaeroWorldMapFold.lastInsideTile(
                XaeroWorldMapFold.chunkCopies(Direction.Axis.Z), canonical.z());
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.GETFIELD),
            slice = @Slice(to = @At(value = "INVOKE", target = SET_TILE)))
    private int toroidal$tileInsideX(int raw) {
        return this.toroidal$foldedPosition ? this.toroidal$insideX : raw;
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_Z, opcode = Opcodes.GETFIELD),
            slice = @Slice(to = @At(value = "INVOKE", target = SET_TILE)))
    private int toroidal$tileInsideZ(int raw) {
        return this.toroidal$foldedPosition ? this.toroidal$insideZ : raw;
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.GETFIELD),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = SET_TILE),
                    to = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int toroidal$bufferTriggerX(int raw) {
        return this.toroidal$foldedPosition ? this.toroidal$lastInsideX : raw;
    }

    @ModifyExpressionValue(
            method = "writeChunk",
            at = @At(value = "FIELD", target = INSIDE_Z, opcode = Opcodes.GETFIELD),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = SET_TILE),
                    to = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.PUTFIELD, ordinal = 0)))
    private int toroidal$bufferTriggerZ(int raw) {
        return this.toroidal$foldedPosition ? this.toroidal$lastInsideZ : raw;
    }

    @ModifyExpressionValue(
            method = "updateBottomRightTile",
            at = @At(value = "FIELD", target = INSIDE_X, opcode = Opcodes.GETFIELD))
    private int toroidal$slopeInsideX(int raw) {
        return this.toroidal$foldedPosition ? this.toroidal$insideX : raw;
    }

    @ModifyExpressionValue(
            method = "updateBottomRightTile",
            at = @At(value = "FIELD", target = INSIDE_Z, opcode = Opcodes.GETFIELD))
    private int toroidal$slopeInsideZ(int raw) {
        return this.toroidal$foldedPosition ? this.toroidal$insideZ : raw;
    }

    @WrapOperation(
            method = "writeChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/pool/MapTilePool;get(Ljava/lang/String;II)Lxaero/map/region/MapTile;"))
    private MapTile toroidal$canonicalTileChunk(MapTilePool pool, String dimension, int chunkX, int chunkZ,
            Operation<MapTile> original) {
        ChunkPos canonical = XaeroWorldMapFold.canonicalChunk(chunkX, chunkZ);
        return original.call(pool, dimension, canonical.x(), canonical.z());
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "startRegionX")
    private int toroidal$visitFromRegionX(int startRegionX) {
        this.toroidal$visitRegions = XaeroWorldMapFold.active()
                ? XaeroWorldMapFold.canonicalRegions(this.startTileChunkX, this.startTileChunkZ,
                        this.endTileChunkX, this.endTileChunkZ)
                : null;
        return this.toroidal$visitRegions == null ? startRegionX : 0;
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "startRegionZ")
    private int toroidal$visitFromRegionZ(int startRegionZ) {
        return this.toroidal$visitRegions == null ? startRegionZ : 0;
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "endRegionX")
    private int toroidal$visitToRegionX(int endRegionX) {
        return this.toroidal$visitRegions == null ? endRegionX : 0;
    }

    @ModifyVariable(method = "onRender", at = @At("STORE"), name = "endRegionZ")
    private int toroidal$visitToRegionZ(int endRegionZ) {
        return this.toroidal$visitRegions == null ? endRegionZ : this.toroidal$visitRegions.length - 1;
    }

    @WrapOperation(
            method = "onRender",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private MapRegion toroidal$visitCanonicalRegion(MapProcessor processor, int caveLayer, int regionX, int regionZ,
            boolean create, Operation<MapRegion> original) {
        if (this.toroidal$visitRegions == null) {
            return original.call(processor, caveLayer, regionX, regionZ, create);
        }

        long region = this.toroidal$visitRegions[regionZ];
        return original.call(processor, caveLayer, ChunkPos.getX(region), ChunkPos.getZ(region), create);
    }

    @WrapOperation(
            method = "onRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/LeveledRegion;setComparison(IIIII)V"))
    private void toroidal$foldComparison(int x, int z, int level, int leafX, int leafZ, Operation<Void> original) {
        if (!XaeroWorldMapFold.active()) {
            original.call(x, z, level, leafX, leafZ);
            return;
        }

        ChunkPos canonical = XaeroWorldMapFold.foldComparison(x, z);
        original.call(canonical.x(), canonical.z(), level, canonical.x(), canonical.z());
    }
}
