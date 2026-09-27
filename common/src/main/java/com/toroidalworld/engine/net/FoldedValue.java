package com.toroidalworld.engine.net;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.fold.FoldedCopies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
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
        WorldFold transformer = context.transformer();
        Leaves leaves = new Leaves(
                pos -> nearestCopy(context, anchor.get(), pos),
                position -> transformer.nearestCopy(anchor.get(), position),
                chunkPos -> transformer.nearestCopy(ChunkPos.containing(BlockPos.containing(anchor.get())), chunkPos),
                centre -> transformer.nearestCopyTransformation(anchor.get(), centre));
        return walk(context, leaves, value, fallback);
    }

    static Leaves toClient(TranslationContext context) {
        return new Leaves(context::toClient, context::toClient, context::toClient, context::nearestCopyTransformation);
    }

    static Leaves toServer(TranslationContext context) {
        WorldFold transformer = context.transformer();
        return new Leaves(transformer::fold, transformer::fold, transformer::fold, transformer::foldTransformation);
    }

    static Object walk(TranslationContext context, Leaves leaves, Object value) {
        return walk(context, leaves, value, UnaryOperator.identity());
    }

    static BlockPos nearestCopy(TranslationContext context, Vec3 anchor, BlockPos pos) {
        return context.transformer().nearestCopy(BlockPos.containing(anchor), pos);
    }

    private static Object walk(TranslationContext context, Leaves leaves, Object value,
            UnaryOperator<Object> fallback) {
        return switch (value) {
            case null -> null;
            case BlockPos pos -> leaves.blockPos().apply(pos);
            case Vec3 position -> leaves.position().apply(position);
            case ChunkPos chunkPos -> leaves.chunkPos().apply(chunkPos);
            case SectionPos sectionPos -> inside(leaves, sectionPos);
            case GlobalPos globalPos -> inside(context, leaves, globalPos);
            case AABB box -> leaves.deck().apply(box.getCenter()).apply(box);
            case Vector3dc vector -> inside(leaves, vector);
            case Optional<?> held -> inside(context, leaves, held, fallback);
            case List<?> values -> inside(context, leaves, values, fallback);
            default -> other(context, leaves, value, fallback);
        };
    }

    private static Object other(TranslationContext context, Leaves leaves, Object value,
            UnaryOperator<Object> fallback) {
        Object fallenBack = fallback.apply(value);
        if (fallenBack != value || !(value instanceof Record record)) {
            return fallenBack;
        }

        return RecordPayloadFold.formOf(record.getClass())
                .rebuilt(record, component -> walk(context, leaves, component, fallback));
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

    private static GlobalPos inside(TranslationContext context, Leaves leaves, GlobalPos globalPos) {
        if (!globalPos.dimension().equals(context.dimension())) {
            return globalPos;
        }

        BlockPos foldedPos = leaves.blockPos().apply(globalPos.pos());
        return foldedPos == globalPos.pos() ? globalPos : GlobalPos.of(globalPos.dimension(), foldedPos);
    }

    private static Optional<?> inside(TranslationContext context, Leaves leaves, Optional<?> held,
            UnaryOperator<Object> fallback) {
        Object value = held.orElse(null);
        if (value == null) {
            return held;
        }

        Object foldedValue = walk(context, leaves, value, fallback);
        return foldedValue == value ? held : Optional.of(foldedValue);
    }

    @SuppressWarnings("unchecked")
    private static List<?> inside(TranslationContext context, Leaves leaves, List<?> values,
            UnaryOperator<Object> fallback) {
        return FoldedCopies.of((List<Object>) values, value -> walk(context, leaves, value, fallback));
    }

    private FoldedValue() {
    }
}
