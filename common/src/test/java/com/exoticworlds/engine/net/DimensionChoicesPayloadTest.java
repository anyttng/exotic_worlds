package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.DimensionTable;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

class DimensionChoicesPayloadTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final ResourceKey<LevelStem> ISLANDS =
            ResourceKey.create(Registries.LEVEL_STEM, Identifier.fromNamespaceAndPath("example_mod", "islands"));

    private static final double COORDINATE_SCALE = 1.0;

    @Test
    void aRowCarriesWhetherItsStemHasEndTerrain() {
        DimensionChoicesPayload sent = new DimensionChoicesPayload(List.of(
                new DimensionChoicesPayload.Row(ISLANDS, Optional.empty(), DimensionTable.Entry.FROM_OVERWORLD,
                        COORDINATE_SCALE, true),
                new DimensionChoicesPayload.Row(LevelStem.NETHER, Optional.empty(),
                        DimensionTable.Entry.FROM_OVERWORLD, COORDINATE_SCALE, false)));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);

        DimensionChoicesPayload.STREAM_CODEC.encode(buffer, sent);

        assertEquals(sent, DimensionChoicesPayload.STREAM_CODEC.decode(buffer));
    }
}
