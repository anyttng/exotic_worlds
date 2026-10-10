package com.exoticworlds.engine.net;

import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.simibubi.create.content.contraptions.glue.GlueEffectPacket;
import com.simibubi.create.content.equipment.bell.SoulPulseEffectPacket;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryEffectPacket;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmPlacementPacket;
import com.simibubi.create.content.logistics.box.PackageDestroyPacket;
import com.simibubi.create.content.logistics.depot.EjectorPlacementPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelEffectPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.packagePort.PackagePortPlacementPacket;
import com.simibubi.create.content.logistics.packagerLink.WiFiEffectPacket;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterEffectPacket;
import com.simibubi.create.content.logistics.stockTicker.LogisticalStockResponsePacket;
import com.exoticworlds.compat.create.CreateTranslation;
import com.exoticworlds.engine.seam.ClientPosition;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class CreatePayloadTranslationTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final int VIEW_DISTANCE = 16;

    private static final double NEAR_THE_EAST_EDGE = 200.0;
    private static final double AT_THE_CENTRE = 8.0;
    private static final double PLAYER_Z = 8.0;

    private static final BlockPos SERVER_BLOCK = new BlockPos(-232, 64, 8);
    private static final BlockPos CLIENT_BLOCK = new BlockPos(280, 64, 8);
    private static final BlockPos INLAND_BLOCK = new BlockPos(200, 64, 8);
    private static final Vec3 SERVER_POINT = Vec3.atCenterOf(SERVER_BLOCK);
    private static final Vec3 CLIENT_POINT = Vec3.atCenterOf(CLIENT_BLOCK);

    private static final BlockPos SERVER_MIRROR = new BlockPos(232, 64, 8);
    private static final BlockPos SERVER_PLACED = new BlockPos(-232, 64, 8);
    private static final BlockPos PLACED_BESIDE_THE_MIRROR = new BlockPos(280, 64, 8);

    @BeforeAll
    static void registerTheCreateRewriters() {
        CreateTranslation.register();
    }

    private static TranslationContext contextAt(double playerX) {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(playerX, PLAYER_Z, Level.OVERWORLD, null, PER_AXIS);
        return new TranslationContext(PER_AXIS, mirror, CreatePayloadTranslationTest::buffer, Level.OVERWORLD,
                VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityId -> null, entityId -> null, () -> {},
                PacketTranslator.production());
    }

    private static CustomPacketPayload seated(CustomPacketPayload payload, double playerX) {
        ClientboundCustomPayloadPacket sent = new ClientboundCustomPayloadPacket(payload);
        return ((ClientboundCustomPayloadPacket) PacketTranslator.toClient(sent, contextAt(playerX))).payload();
    }

    private static CustomPacketPayload seated(CustomPacketPayload payload) {
        return seated(payload, NEAR_THE_EAST_EDGE);
    }

    // The record fold's deny check reads payload.type(), which starts Create's own registration outside a loader.
    private static CustomPacketPayload folded(CustomPacketPayload payload) {
        TranslationContext context = contextAt(NEAR_THE_EAST_EDGE);
        return RecordPayloadFold.formOf(payload.getClass())
                .rebuilt(payload, value -> FoldedValue.walk(context, FoldedValue.toClient(context), value));
    }

    @Test
    void everyPayloadTheRecordFoldCoversHasNoRewriterOfItsOwn() {
        List<CustomPacketPayload> covered = List.of(
                new ArmPlacementPacket.ClientBoundRequest(SERVER_BLOCK),
                new EjectorPlacementPacket.ClientBoundRequest(SERVER_BLOCK),
                new PackagePortPlacementPacket.ClientBoundRequest(SERVER_BLOCK),
                new SoulPulseEffectPacket(SERVER_BLOCK, 5, true),
                new WiFiEffectPacket(SERVER_BLOCK),
                new GlueEffectPacket(SERVER_BLOCK, Direction.UP, true),
                new RedstoneRequesterEffectPacket(SERVER_BLOCK, true),
                new LogisticalStockResponsePacket(true, SERVER_BLOCK, List.of()),
                new PackageDestroyPacket(SERVER_POINT, ItemStack.EMPTY));
        for (CustomPacketPayload payload : covered) {
            assertNull(PacketTranslator.production().clientboundPayloadFor(payload), payload.getClass().getName());
        }
    }

    @Test
    void everyPlacementEchoLandsOnTheCopyTheClientHolds() {
        assertEquals(new ArmPlacementPacket.ClientBoundRequest(CLIENT_BLOCK),
                folded(new ArmPlacementPacket.ClientBoundRequest(SERVER_BLOCK)));
        assertEquals(new EjectorPlacementPacket.ClientBoundRequest(CLIENT_BLOCK),
                folded(new EjectorPlacementPacket.ClientBoundRequest(SERVER_BLOCK)));
        assertEquals(new PackagePortPlacementPacket.ClientBoundRequest(CLIENT_BLOCK),
                folded(new PackagePortPlacementPacket.ClientBoundRequest(SERVER_BLOCK)));
    }

    @Test
    void everySingleBlockEffectLandsOnTheCopyTheClientHolds() {
        assertEquals(new SoulPulseEffectPacket(CLIENT_BLOCK, 5, true),
                folded(new SoulPulseEffectPacket(SERVER_BLOCK, 5, true)));
        assertEquals(new WiFiEffectPacket(CLIENT_BLOCK), folded(new WiFiEffectPacket(SERVER_BLOCK)));
        assertEquals(new GlueEffectPacket(CLIENT_BLOCK, Direction.UP, true),
                folded(new GlueEffectPacket(SERVER_BLOCK, Direction.UP, true)));
        assertEquals(new RedstoneRequesterEffectPacket(CLIENT_BLOCK, true),
                folded(new RedstoneRequesterEffectPacket(SERVER_BLOCK, true)));
        assertEquals(new LogisticalStockResponsePacket(true, CLIENT_BLOCK, List.of()),
                folded(new LogisticalStockResponsePacket(true, SERVER_BLOCK, List.of())));
    }

    @Test
    void theDestroyedPackagesBurstLandsOnTheCopyTheClientHolds() {
        ItemStack box = ItemStack.EMPTY;
        assertEquals(new PackageDestroyPacket(CLIENT_POINT, box), folded(new PackageDestroyPacket(SERVER_POINT, box)));
    }

    @Test
    void theSymmetryMirrorAndItsPlacementsLandInOneFrame() {
        assertEquals(new SymmetryEffectPacket(CLIENT_BLOCK, List.of(CLIENT_BLOCK)),
                seated(new SymmetryEffectPacket(SERVER_BLOCK, List.of(SERVER_BLOCK))));
    }

    @Test
    void aSymmetryPlacementPastTheMirrorSeatsOnTheMirrorRatherThanOnThePlayer() {
        assertEquals(new SymmetryEffectPacket(SERVER_MIRROR, List.of(PLACED_BESIDE_THE_MIRROR)),
                seated(new SymmetryEffectPacket(SERVER_MIRROR, List.of(SERVER_PLACED)), AT_THE_CENTRE));
    }

    @Test
    void theFactoryPanelFeedbackCrossesTheBoundaryCanonical() {
        FactoryPanelPosition acrossTheSeam = new FactoryPanelPosition(SERVER_BLOCK, FactoryPanelBlock.PanelSlot.TOP_LEFT);
        FactoryPanelEffectPacket feedback = new FactoryPanelEffectPacket(acrossTheSeam, acrossTheSeam, true);
        assertNotNull(PacketTranslator.production().clientboundPayloadFor(feedback));
        assertSame(feedback, seated(feedback));
    }

    @Test
    void aPayloadInThePlayersOwnFrameTravelsUntouched() {
        WiFiEffectPacket inland = new WiFiEffectPacket(INLAND_BLOCK);
        assertSame(inland, folded(inland));

        SymmetryEffectPacket inlandSymmetry = new SymmetryEffectPacket(INLAND_BLOCK, List.of(INLAND_BLOCK));
        assertSame(inlandSymmetry, seated(inlandSymmetry));
    }

    private static RegistryFriendlyByteBuf buffer(int capacity) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(capacity), REGISTRIES);
    }
}
