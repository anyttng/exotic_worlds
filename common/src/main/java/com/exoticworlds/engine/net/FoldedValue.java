package com.exoticworlds.engine.net;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedCopies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FoldedValue {
    record Leaves(UnaryOperator<BlockPos> blockPos, UnaryOperator<Vec3> position, UnaryOperator<ChunkPos> chunkPos,
            Function<Vec3, DeckTransformation> deck) {
    }

    public static Object toward(TranslationContext context, Supplier<Vec3> anchor, Object value) {
        return toward(context, anchor, value, UnaryOperator.identity());
    }

    public static Object toward(TranslationContext context, Supplier<Vec3> anchor, Object value,
            UnaryOperator<Object> fallback) {
        return walk(context.dimension(), towardLeaves(context.transformer(), anchor), value, fallback);
    }

    public static Object toward(WorldFold transformer, ResourceKey<Level> dimension, Vec3 anchor, Object value) {
        return walk(dimension, towardLeaves(transformer, () -> anchor), value, UnaryOperator.identity());
    }

    private static Leaves towardLeaves(WorldFold transformer, Supplier<Vec3> anchor) {
        return new Leaves(
                pos -> transformer.nearestCopy(BlockPos.containing(anchor.get()), pos),
                position -> transformer.nearestCopy(anchor.get(), position),
                chunkPos -> transformer.nearestCopy(new ChunkPos(BlockPos.containing(anchor.get())), chunkPos),
                centre -> transformer.nearestCopyTransformation(anchor.get(), centre));
    }

    static Leaves toClient(TranslationContext context) {
        return new Leaves(context::toClient, context::toClient, context::toClient, context::nearestCopyTransformation);
    }

    static Leaves toServer(TranslationContext context) {
        WorldFold transformer = context.transformer();
        return new Leaves(transformer::fold, transformer::fold, transformer::fold, transformer::foldTransformation);
    }

    static Object walk(TranslationContext context, Leaves leaves, Object value) {
        return walk(context.dimension(), leaves, value, UnaryOperator.identity());
    }

    static BlockPos nearestCopy(TranslationContext context, Vec3 anchor, BlockPos pos) {
        return context.transformer().nearestCopy(BlockPos.containing(anchor), pos);
    }

    private static Object walk(ResourceKey<Level> dimension, Leaves leaves, Object value,
            UnaryOperator<Object> fallback) {
        return switch (value) {
            case null -> null;
            case BlockPos pos -> leaves.blockPos().apply(pos);
            case Vec3 position -> leaves.position().apply(position);
            case ChunkPos chunkPos -> leaves.chunkPos().apply(chunkPos);
            case SectionPos sectionPos -> inside(leaves, sectionPos);
            case GlobalPos globalPos -> inside(dimension, leaves, globalPos);
            case AABB box -> leaves.deck().apply(box.getCenter()).apply(box);
            case Vector3dc vector -> inside(leaves, vector);
            case Optional<?> held -> inside(dimension, leaves, held, fallback);
            case List<?> values -> inside(dimension, leaves, values, fallback);
            default -> other(dimension, leaves, value, fallback);
        };
    }

    private static Object other(ResourceKey<Level> dimension, Leaves leaves, Object value,
            UnaryOperator<Object> fallback) {
        Object fallenBack = fallback.apply(value);
        if (fallenBack != value || !(value instanceof Record record)) {
            return fallenBack;
        }

        return RecordPayloadFold.formOf(record.getClass())
                .rebuilt(record, component -> walk(dimension, leaves, component, fallback));
    }

    private static Vector3dc inside(Leaves leaves, Vector3dc vector) {
        Vec3 position = new Vec3(vector.x(), vector.y(), vector.z());
        Vec3 foldedPosition = leaves.position().apply(position);
        return foldedPosition == position ? vector : new Vector3d(foldedPosition.x, foldedPosition.y, foldedPosition.z);
    }

    private static SectionPos inside(Leaves leaves, SectionPos sectionPos) {
        ChunkPos chunkPos = sectionPos.chunk();
        ChunkPos foldedChunkPos = leaves.chunkPos().apply(chunkPos);
        return foldedChunkPos == chunkPos ? sectionPos : SectionPos.of(foldedChunkPos, sectionPos.y());
    }

    private static GlobalPos inside(ResourceKey<Level> dimension, Leaves leaves, GlobalPos globalPos) {
        if (!globalPos.dimension().equals(dimension)) {
            return globalPos;
        }

        BlockPos foldedPos = leaves.blockPos().apply(globalPos.pos());
        return foldedPos == globalPos.pos() ? globalPos : GlobalPos.of(globalPos.dimension(), foldedPos);
    }

    private static Optional<?> inside(ResourceKey<Level> dimension, Leaves leaves, Optional<?> held,
            UnaryOperator<Object> fallback) {
        Object value = held.orElse(null);
        if (value == null) {
            return held;
        }

        Object foldedValue = walk(dimension, leaves, value, fallback);
        return foldedValue == value ? held : Optional.of(foldedValue);
    }

    @SuppressWarnings("unchecked")
    private static List<?> inside(ResourceKey<Level> dimension, Leaves leaves, List<?> values,
            UnaryOperator<Object> fallback) {
        return FoldedCopies.of((List<Object>) values, value -> walk(dimension, leaves, value, fallback));
    }

    private FoldedValue() {
    }
}
