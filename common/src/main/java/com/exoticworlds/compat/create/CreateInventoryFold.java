package com.exoticworlds.compat.create;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class CreateInventoryFold {
    public static @Nullable InventoryIdentifier fold(Level level, @Nullable InventoryIdentifier identifier) {
        if (identifier == null) {
            return null;
        }

        return fold(WorldLoopAttachments.wrappedFoldOf(level), identifier);
    }

    static @Nullable InventoryIdentifier fold(@Nullable WorldFold fold,
            @Nullable InventoryIdentifier identifier) {
        if (identifier == null || fold == null) {
            return identifier;
        }

        return new SeamIdentifier(canonical(fold, identifier), fold);
    }

    static InventoryIdentifier canonical(WorldFold fold, InventoryIdentifier identifier) {
        return switch (identifier) {
            case InventoryIdentifier.Single single ->
                    new InventoryIdentifier.Single(fold.fold(single.pos()));
            case InventoryIdentifier.MultiFace multiFace ->
                    new InventoryIdentifier.MultiFace(fold.fold(multiFace.pos()), multiFace.sides());
            case InventoryIdentifier.Pair pair -> new InventoryIdentifier.Pair(
                    fold.fold(pair.first()), fold.fold(pair.second()));
            case InventoryIdentifier.Bounds bounds -> seamBounds(fold, bounds);
            default -> identifier;
        };
    }

    private static InventoryIdentifier seamBounds(WorldFold fold, InventoryIdentifier.Bounds bounds) {
        if (!fold.crossesBounds(bounds.bounds())) {
            return bounds;
        }

        return new SeamBounds(fold.split(bounds.bounds()).stream().map(WorldFold.Folded::value).toList());
    }

    private CreateInventoryFold() {
    }

    private static final class SeamIdentifier implements InventoryIdentifier {
        private final InventoryIdentifier canonical;
        private final WorldFold fold;

        private SeamIdentifier(InventoryIdentifier canonical, WorldFold fold) {
            this.canonical = canonical;
            this.fold = fold;
        }

        @Override
        public boolean contains(BlockFace face) {
            BlockPos canonicalPos = fold.fold(face.getPos());
            return canonical.contains(
                    canonicalPos == face.getPos() ? face : new BlockFace(canonicalPos, face.getFace()));
        }

        // The fold stays out of equality, or Create de-duplicates inventories differently in a wrapped world.
        @Override
        public boolean equals(Object other) {
            return other instanceof SeamIdentifier seam && canonical.equals(seam.canonical);
        }

        @Override
        public int hashCode() {
            return canonical.hashCode();
        }

        @Override
        public String toString() {
            return canonical.toString();
        }
    }

    private record SeamBounds(List<BoundingBox> regions) implements InventoryIdentifier {
        @Override
        public boolean contains(BlockFace face) {
            for (BoundingBox region : regions) {
                if (region.isInside(face.getPos())) {
                    return true;
                }
            }

            return false;
        }
    }
}
