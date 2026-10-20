package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.BiomeZoomOwner;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedQuart;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;

@Mixin(BiomeManager.class)
public class BiomeManagerMixin implements BiomeZoomOwner {
    private static final int CORNERS = 8;
    private static final int CORNER_X_BIT = 4;
    private static final int CORNER_Y_BIT = 2;
    private static final int CORNER_Z_BIT = 1;
    private static final int ZOOM_CENTER = QuartPos.SIZE / 2;
    private static final double ZOOM = QuartPos.SIZE;

    @Shadow
    @Final
    private BiomeResolver noiseBiomeSource;

    @Shadow
    @Final
    private long biomeZoomSeed;

    @Unique
    private @Nullable LevelReader exotic_worlds$owner;

    @Shadow
    private static double getFiddledDistance(long seed, int xRandom, int yRandom, int zRandom, double distanceX,
            double distanceY, double distanceZ) {
        throw new AssertionError();
    }

    @Override
    public @Nullable LevelReader exotic_worlds$owner() {
        return this.exotic_worlds$owner;
    }

    @Override
    public void exotic_worlds$setOwner(@Nullable LevelReader owner) {
        this.exotic_worlds$owner = owner;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void exotic_worlds$recordOwner(BiomeResolver noiseBiomeSource, long seed, CallbackInfo ci) {
        if (noiseBiomeSource instanceof LevelReader owner) {
            this.exotic_worlds$owner = owner;
        }
    }

    @ModifyReturnValue(method = "withDifferentSource", at = @At("RETURN"))
    private BiomeManager exotic_worlds$carryOwner(BiomeManager copy) {
        ((BiomeZoomOwner) copy).exotic_worlds$setOwner(this.exotic_worlds$owner);
        return copy;
    }

    @WrapMethod(method = "getBiome(III)Lnet/minecraft/core/Holder;")
    private Holder<Biome> exotic_worlds$zoomOnFoldedCorners(int x, int y, int z, Operation<Holder<Biome>> original) {
        WorldFold fold = WorldLoopAttachments.transformerOfReader(this.exotic_worlds$owner);
        if (!fold.isWrapped()) {
            return original.call(x, y, z);
        }

        int absX = x - ZOOM_CENTER;
        int absY = y - ZOOM_CENTER;
        int absZ = z - ZOOM_CENTER;
        int parentX = absX >> QuartPos.BITS;
        int parentY = absY >> QuartPos.BITS;
        int parentZ = absZ >> QuartPos.BITS;
        double fractX = (absX & QuartPos.MASK) / ZOOM;
        double fractY = (absY & QuartPos.MASK) / ZOOM;
        double fractZ = (absZ & QuartPos.MASK) / ZOOM;
        int minI = 0;
        double minFiddledDistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < CORNERS; i++) {
            boolean xEven = (i & CORNER_X_BIT) == 0;
            boolean yEven = (i & CORNER_Y_BIT) == 0;
            boolean zEven = (i & CORNER_Z_BIT) == 0;
            int cornerX = xEven ? parentX : parentX + 1;
            int cornerY = yEven ? parentY : parentY + 1;
            int cornerZ = zEven ? parentZ : parentZ + 1;
            long folded = FoldedQuart.fold(fold, cornerX, cornerY, cornerZ);
            double next = getFiddledDistance(this.biomeZoomSeed, FoldedQuart.x(folded), cornerY, FoldedQuart.z(folded),
                    xEven ? fractX : fractX - 1.0, yEven ? fractY : fractY - 1.0, zEven ? fractZ : fractZ - 1.0);
            if (minFiddledDistance > next) {
                minI = i;
                minFiddledDistance = next;
            }
        }

        return this.noiseBiomeSource.getNoiseBiome(
                (minI & CORNER_X_BIT) == 0 ? parentX : parentX + 1,
                (minI & CORNER_Y_BIT) == 0 ? parentY : parentY + 1,
                (minI & CORNER_Z_BIT) == 0 ? parentZ : parentZ + 1);
    }
}
