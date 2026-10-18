package com.exoticworlds.compat.electroenergetics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.compat.electroenergetics.BoxCopies;
import com.exoticworlds.compat.electroenergetics.WireBox;
import com.exoticworlds.compat.electroenergetics.WireChunkKeys;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.world.level.ChunkPos;

@Mixin(targets = "com.george_vi.electroenergetics.content.wire.WireSync$ChunkCuboidEntry", remap = false)
public abstract class ChunkCuboidEntryMixin implements TransformerHolder, WireBox {
    @Shadow
    public int minX;

    @Shadow
    public int maxX;

    @Shadow
    public int minZ;

    @Shadow
    public int maxZ;

    @Unique
    private WorldFold exotic_worlds$transformer = WorldFolds.NOOP;

    @Override
    public WorldFold exotic_worlds$transformer() {
        return this.exotic_worlds$transformer;
    }

    @Override
    public void exotic_worlds$setTransformer(WorldFold transformer) {
        this.exotic_worlds$transformer = transformer;
    }

    @Override
    public ChunkPos exotic_worlds$centre() {
        return new ChunkPos(Math.floorDiv(this.minX + this.maxX - 1, 2), Math.floorDiv(this.minZ + this.maxZ - 1, 2));
    }

    @Override
    public int exotic_worlds$radius() {
        return (this.maxX - this.minX - 1) / 2;
    }

    @WrapMethod(method = "includes(II)Z")
    private boolean exotic_worlds$includesNearestCopy(int x, int z, Operation<Boolean> original) {
        ChunkPos seated = BoxCopies.seat(this.exotic_worlds$transformer, this.exotic_worlds$centre(), this.exotic_worlds$radius(),
                new ChunkPos(x, z));
        return original.call(seated.x, seated.z);
    }

    @ModifyReturnValue(method = "iterator()Lit/unimi/dsi/fastutil/longs/LongIterator;", at = @At("RETURN"))
    private LongIterator exotic_worlds$foldedChunks(LongIterator raw) {
        return WireChunkKeys.of(raw, this.exotic_worlds$transformer);
    }
}
