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
        BlockPos a = move.apply(min.get());
        BlockPos b = move.apply(max.get());
        seated.put(MIN_KEY, NbtUtils.writeBlockPos(new BlockPos(Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()))));
        seated.put(MAX_KEY, NbtUtils.writeBlockPos(new BlockPos(Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()))));
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
