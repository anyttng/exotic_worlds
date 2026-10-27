package com.exoticworlds.compat.create.client;

import java.util.List;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.exoticworlds.compat.create.CreateTrackFold;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.seam.MapSurfaceCopies;
import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

public final class TrainMapViewFold {
    // Create's TrainMapManager.renderAndPick grows the rect it is handed by this margin before it draws anything.
    private static final int OFF_SCREEN_MARGIN = 32;

    public record NearestNodeKey(Vec3i anchor, Vec3i raw, Vec3i nearest) {
    }

    public static WorldFold fold() {
        return TrainMapFrame.current();
    }

    public static NearestNodeKey nearestNodeKey(Vec3i anchor, Vec3i key, LocalRef<NearestNodeKey> memo) {
        return nearestNodeKey(fold(), anchor, key, memo);
    }

    public static NearestNodeKey nearestNodeKey(WorldFold fold, Vec3i anchor, Vec3i key,
            LocalRef<NearestNodeKey> memo) {
        NearestNodeKey known = memo.get();
        if (known != null && known.anchor() == anchor && known.raw() == key) {
            return known;
        }

        NearestNodeKey folded =
                new NearestNodeKey(anchor, key, CreateTrackFold.nearestNodeKey(fold, anchor, key));
        memo.set(folded);
        return folded;
    }

    public static long foldPixelNode(int x, int z) {
        return foldPixelNode(fold(), x, z);
    }

    public static long foldPixelNode(WorldFold fold, int x, int z) {
        return fold.foldBlockNode(BlockPos.asLong(x, 0, z));
    }

    public static Vec3 canonical(Vec3 position) {
        return fold().fold(position);
    }

    public static List<DeckTransformation> copiesDrawnFor(Rect2i bounds) {
        WorldFold fold = fold();
        if (!fold.isWrapped()) {
            return List.of(DeckTransformation.IDENTITY);
        }

        return copiesDrawnFor(fold, MapSurfaceCopies.current(), bounds);
    }

    public static List<DeckTransformation> copiesDrawnFor(WorldFold fold, Copies surface, Rect2i bounds) {
        Rect2i drawn = new Rect2i(bounds.getX() - OFF_SCREEN_MARGIN, bounds.getY() - OFF_SCREEN_MARGIN,
                bounds.getWidth() + 2 * OFF_SCREEN_MARGIN, bounds.getHeight() + 2 * OFF_SCREEN_MARGIN);
        return copies(fold, surface, drawn);
    }

    public static List<DeckTransformation> copies(WorldFold fold, Copies surface, Rect2i view) {
        BoundingBox painted = surface.painted();
        int minX = Math.max(view.getX(), painted.minX());
        int maxX = Math.min(view.getX() + view.getWidth() - 1, painted.maxX());
        int minZ = Math.max(view.getY(), painted.minZ());
        int maxZ = Math.min(view.getY() + view.getHeight() - 1, painted.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return List.of();
        }

        return fold.copiesTouching(new BoundingBox(minX, 0, minZ, maxX, 0, maxZ), surface.reach());
    }

    public static Rect2i canonicalView(DeckTransformation copy, Rect2i view) {
        BoundingBox canonical = copy.inverse().apply(new BoundingBox(view.getX(), 0, view.getY(),
                view.getX() + view.getWidth() - 1, 0, view.getY() + view.getHeight() - 1));
        return new Rect2i(canonical.minX(), canonical.minZ(), canonical.getXSpan(), canonical.getZSpan());
    }

    public static BlockPos canonicalPixel(DeckTransformation copy, int x, int z) {
        return copy.inverse().apply(new BlockPos(x, 0, z));
    }

    private TrainMapViewFold() {
    }
}
