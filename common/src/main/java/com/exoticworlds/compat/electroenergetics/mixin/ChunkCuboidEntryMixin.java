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
    private WorldFold toroidal$transformer = WorldFolds.NOOP;

    @Override
    public WorldFold toroidal$transformer() {
        return this.toroidal$transformer;
    }

    @Override
    public void toroidal$setTransformer(WorldFold transformer) {
        this.toroidal$transformer = transformer;
    }

    @Override
    public ChunkPos toroidal$centre() {
        return new ChunkPos(Math.floorDiv(this.minX + this.maxX - 1, 2), Math.floorDiv(this.minZ + this.maxZ - 1, 2));
    }

    @Override
    public int toroidal$radius() {
        return (this.maxX - this.minX - 1) / 2;
    }

    @WrapMethod(method = "includes(II)Z")
    private boolean toroidal$includesNearestCopy(int x, int z, Operation<Boolean> original) {
        ChunkPos seated = BoxCopies.seat(this.toroidal$transformer, this.toroidal$centre(), this.toroidal$radius(),
                new ChunkPos(x, z));
        return original.call(seated.x, seated.z);
    }

    @ModifyReturnValue(method = "iterator()Lit/unimi/dsi/fastutil/longs/LongIterator;", at = @At("RETURN"))
    private LongIterator toroidal$foldedChunks(LongIterator raw) {
        return WireChunkKeys.of(raw, this.toroidal$transformer);
    }
}
