package com.exoticworlds.compat.mekanism;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class MultiblockSyncedTag {
    static final String MIN_KEY = "min";
    static final String MAX_KEY = "max";
    static final String RENDER_LOCATION_KEY = "render_location";
    static final String RENDER_Y_KEY = "render_y";
    static final String VALVE_KEY = "valve";
    static final String COILS_KEY = "coils";
    static final String COMPLEX_KEY = "complex";
    static final String ASSEMBLIES_KEY = "assemblies";
    static final String POSITION_KEY = "position";

    private static final List<String> POSITION_KEYS = List.of(RENDER_LOCATION_KEY, RENDER_Y_KEY, COMPLEX_KEY);
    private static final Map<String, String> LIST_POSITION_KEYS = Map.of(
            VALVE_KEY, POSITION_KEY,
            COILS_KEY, POSITION_KEY,
            ASSEMBLIES_KEY, POSITION_KEY);

    public static CompoundTag seat(WorldFold fold, BlockPos anchor, CompoundTag tag) {
        Optional<BlockPos> min = NbtUtils.readBlockPos(tag, MIN_KEY);
        Optional<BlockPos> max = NbtUtils.readBlockPos(tag, MAX_KEY);
        if (min.isEmpty() || max.isEmpty()) {
            return tag;
        }

        DeckTransformation move = fold.nearestCopyTransformation(anchor, min.get());
        if (move.isIdentity()) {
            return tag;
        }

        CompoundTag seated = tag.copy();
        BoundingBox moved = move.apply(BoundingBox.fromCorners(min.get(), max.get()));
        seated.put(MIN_KEY, NbtUtils.writeBlockPos(new BlockPos(moved.minX(), moved.minY(), moved.minZ())));
        seated.put(MAX_KEY, NbtUtils.writeBlockPos(new BlockPos(moved.maxX(), moved.maxY(), moved.maxZ())));
        for (String key : POSITION_KEYS) {
            move(seated, key, move);
        }

        for (Map.Entry<String, String> entry : LIST_POSITION_KEYS.entrySet()) {
            ListTag list = seated.getList(entry.getKey(), Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                move(list.getCompound(i), entry.getValue(), move);
            }
        }

        return seated;
    }

    private static void move(CompoundTag tag, String key, DeckTransformation move) {
        NbtUtils.readBlockPos(tag, key).ifPresent(pos -> tag.put(key, NbtUtils.writeBlockPos(move.apply(pos))));
    }

    private MultiblockSyncedTag() {
    }
}
