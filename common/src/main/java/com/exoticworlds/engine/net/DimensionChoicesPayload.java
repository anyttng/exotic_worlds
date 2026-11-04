package com.exoticworlds.engine.net;

import java.util.List;
import java.util.Optional;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.DimensionTable;
import com.exoticworlds.engine.gen.DimensionChoices;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record DimensionChoicesPayload(List<Row> rows) implements CustomPacketPayload {
    private static final String PAYLOAD_ID = "dimension_choices";

    public static final Type<DimensionChoicesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, PAYLOAD_ID));

    static final StreamCodec<RegistryFriendlyByteBuf, DimensionTable.Entry> ENTRY_STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(DimensionTable.Entry.CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionChoicesPayload> STREAM_CODEC =
            Row.STREAM_CODEC.apply(ByteBufCodecs.list()).map(DimensionChoicesPayload::new, DimensionChoicesPayload::rows);

    public DimensionChoicesPayload {
        rows = List.copyOf(rows);
    }

    @Override
    public Type<DimensionChoicesPayload> type() {
        return TYPE;
    }

    public record Row(ResourceKey<LevelStem> stem, Optional<DimensionChoices.Refusal> lock,
            DimensionTable.Entry entry) {
        private static final StreamCodec<ByteBuf, DimensionChoices.Refusal> REFUSAL_STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.map(DimensionChoices.Refusal::valueOf, DimensionChoices.Refusal::name);

        static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(Registries.LEVEL_STEM), Row::stem,
                ByteBufCodecs.optional(REFUSAL_STREAM_CODEC), Row::lock,
                ENTRY_STREAM_CODEC, Row::entry,
                Row::new);
    }
}
