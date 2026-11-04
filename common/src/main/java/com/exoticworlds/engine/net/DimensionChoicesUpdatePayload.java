package com.exoticworlds.engine.net;

import java.util.HashMap;
import java.util.Map;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.core.DimensionTable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record DimensionChoicesUpdatePayload(Map<ResourceKey<LevelStem>, DimensionTable.Entry> choices)
        implements CustomPacketPayload {
    private static final String PAYLOAD_ID = "dimension_choices_update";

    public static final Type<DimensionChoicesUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, PAYLOAD_ID));

    public static final StreamCodec<RegistryFriendlyByteBuf, DimensionChoicesUpdatePayload> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, ResourceKey<LevelStem>, DimensionTable.Entry,
                            Map<ResourceKey<LevelStem>, DimensionTable.Entry>>map(
                            HashMap::new,
                            ResourceKey.streamCodec(Registries.LEVEL_STEM),
                            DimensionChoicesPayload.ENTRY_STREAM_CODEC)
                    .map(DimensionChoicesUpdatePayload::new, DimensionChoicesUpdatePayload::choices);

    public DimensionChoicesUpdatePayload {
        choices = Map.copyOf(choices);
    }

    @Override
    public Type<DimensionChoicesUpdatePayload> type() {
        return TYPE;
    }
}
