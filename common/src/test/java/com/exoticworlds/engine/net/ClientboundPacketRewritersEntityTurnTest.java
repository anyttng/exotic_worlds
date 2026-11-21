package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.seam.ClientPosition;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.VecDelta;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

class ClientboundPacketRewritersEntityTurnTest {
    private static final int FACE_WIDTH = 64;
    private static final int VIEW_DISTANCE = 12;
    private static final int TORUS_WIDTH_IN_CHUNKS = 32;

    private static final SeamTransform CLOCKWISE_TO_THE_SEAT = new SeamTransform(-1, 1, FACE_WIDTH, -FACE_WIDTH, true);

    private static final WorldFold TURNED = (WorldFold) Proxy.newProxyInstance(
            WorldFold.class.getClassLoader(),
            new Class<?>[] {WorldFold.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "preservesLocalIndices" -> false;
                case "nearestCopy" -> CLOCKWISE_TO_THE_SEAT.apply((Vec3) arguments[1]);
                case "foldTransformation" -> new DeckTransformation(CLOCKWISE_TO_THE_SEAT.inverse());
                default -> throw new UnsupportedOperationException(method.getName());
            });

    private static final WorldFold TORUS =
            WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_IN_CHUNKS)));

    private static final double ANCHOR_X = 60.0;
    private static final int ENTITY_ID = 11;
    private static final UUID ENTITY_UUID = new UUID(1L, 2L);

    private static final BlockPos SERVER_BLOCK = new BlockPos(58, 70, 2);
    private static final BlockPos SEAT_BLOCK = new BlockPos(61, 70, -6);
    private static final Vec3 SERVER_POSITION = new Vec3(58.5, 70.0, 2.5);
    private static final Vec3 SEAT_POSITION = new Vec3(61.5, 70.0, -5.5);

    private static final Vec3 MOVEMENT = new Vec3(1.0, 0.5, 2.0);
    private static final Vec3 TURNED_MOVEMENT = new Vec3(-2.0, 0.5, 1.0);
    private static final float YAW = 45.0F;
    private static final float HEAD_YAW = 22.5F;
    private static final float PITCH = 11.25F;
    private static final float QUARTER_TURN = 90.0F;

    private static final int OWNER_ID = 77;
    private static final int DIRECTION_SLOT = 8;
    private static final float STEP_WEIGHT = 1.0F;
    private static final float EXPLOSION_RADIUS = 4.0F;

    private static final VecDelta STEP = new VecDelta.Linear((short) 100, (short) 7, (short) 300);
    private static final VecDelta TURNED_STEP = new VecDelta.Linear((short) -300, (short) 7, (short) 100);

    private static TranslationContext context(WorldFold fold, Class<?> entityClass) {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(ANCHOR_X, 0.0, Level.OVERWORLD, fold);
        return new TranslationContext(
                fold,
                clientPosition,
                size -> new RegistryFriendlyByteBuf(Unpooled.buffer(size), RegistryAccess.EMPTY),
                Level.OVERWORLD,
                VIEW_DISTANCE,
                VIEW_DISTANCE,
                entityId -> SERVER_POSITION,
                entityId -> new TagPositions.Subject(entityClass, null),
                () -> {
                });
    }

    private static TranslationContext turned(Class<?> entityClass) {
        return context(TURNED, entityClass);
    }

    private static ClientboundAddEntityPacket spawn(Vec3 position, int data) {
        return new ClientboundAddEntityPacket(ENTITY_ID, ENTITY_UUID, position.x, position.y, position.z,
                PITCH, YAW, EntityTypes.PIG, data, MOVEMENT, HEAD_YAW);
    }

    private static BlockPos blockOf(ClientboundAddEntityPacket packet) {
        return BlockPos.containing(packet.getX(), packet.getY(), packet.getZ());
    }

    private static int stateId(Direction facing) {
        BlockState state = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        return Block.getId(state);
    }

    private static ClientboundRotateHeadPacket headRotation(float yaw) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(ENTITY_ID);
        buffer.writeByte(Mth.packDegrees(yaw));
        return new ClientboundRotateHeadPacket(buffer);
    }

    @Test
    void anItemFrameAddedAtAQuarterTurnSeatCarriesTheDirectionTurnedAndSitsInTheSeatedBlock() {
        ClientboundAddEntityPacket seated = ClientboundPacketRewriters.addEntity(
                spawn(Vec3.atLowerCornerOf(SERVER_BLOCK), Direction.NORTH.get3DDataValue()), turned(ItemFrame.class));

        assertEquals(Direction.EAST.get3DDataValue(), seated.getData());
        assertEquals(SEAT_BLOCK, blockOf(seated));
    }

    @Test
    void aPaintingAddedAtAQuarterTurnSeatCarriesTheDirectionTurnedAndSitsInTheSeatedBlock() {
        ClientboundAddEntityPacket seated = ClientboundPacketRewriters.addEntity(
                spawn(Vec3.atLowerCornerOf(SERVER_BLOCK), Direction.WEST.get3DDataValue()), turned(Painting.class));

        assertEquals(Direction.NORTH.get3DDataValue(), seated.getData());
        assertEquals(SEAT_BLOCK, blockOf(seated));
    }

    @Test
    void aFallingBlockAddedAtAQuarterTurnSeatCarriesItsStateTurned() {
        ClientboundAddEntityPacket seated = ClientboundPacketRewriters.addEntity(
                spawn(SERVER_POSITION, stateId(Direction.NORTH)), turned(FallingBlockEntity.class));

        assertEquals(stateId(Direction.EAST), seated.getData());
        assertEquals(SEAT_POSITION, new Vec3(seated.getX(), seated.getY(), seated.getZ()));
    }

    @Test
    void spawnDataThatIsNotADirectionIsUnchanged() {
        ClientboundAddEntityPacket seated = ClientboundPacketRewriters.addEntity(
                spawn(SERVER_POSITION, OWNER_ID), turned(Pig.class));

        assertEquals(OWNER_ID, seated.getData());
    }

    @Test
    void anEntityAddedAtAQuarterTurnSeatCarriesItsYawsAndMovementTurned() {
        ClientboundAddEntityPacket seated = ClientboundPacketRewriters.addEntity(
                spawn(SERVER_POSITION, 0), turned(Pig.class));

        assertEquals(YAW + QUARTER_TURN, seated.getYRot());
        assertEquals(HEAD_YAW + QUARTER_TURN, seated.getYHeadRot());
        assertEquals(PITCH, seated.getXRot());
        assertEquals(TURNED_MOVEMENT, seated.getMovement());
    }

    @Test
    void anEntityTeleportedToAQuarterTurnSeatCarriesItsYawAndMovementTurned() {
        ClientboundTeleportEntityPacket seated = ClientboundPacketRewriters.teleportEntity(
                new ClientboundTeleportEntityPacket(ENTITY_ID,
                        new PositionMoveRotation(SERVER_POSITION, MOVEMENT, YAW, PITCH), Set.of(), true),
                turned(Pig.class));

        assertEquals(SEAT_POSITION, seated.change().position());
        assertEquals(YAW + QUARTER_TURN, seated.change().yRot());
        assertEquals(PITCH, seated.change().xRot());
        assertEquals(TURNED_MOVEMENT, seated.change().deltaMovement());
    }

    @Test
    void aTeleportKeepingTheMovementOnOneAxisKeepsItOnTheAxisItTurnsOnto() {
        ClientboundTeleportEntityPacket seated = ClientboundPacketRewriters.teleportEntity(
                new ClientboundTeleportEntityPacket(ENTITY_ID,
                        new PositionMoveRotation(SERVER_POSITION, MOVEMENT, YAW, PITCH),
                        Set.of(Relative.DELTA_X, Relative.Y_ROT), true),
                turned(Pig.class));

        assertEquals(Set.of(Relative.DELTA_Z, Relative.Y_ROT), seated.relatives());
        assertEquals(YAW, seated.change().yRot());
    }

    @Test
    void anEntitySyncedAtAQuarterTurnSeatCarriesItsYawTurned() {
        ClientboundEntityPositionSyncPacket seated = ClientboundPacketRewriters.entityPositionSync(
                new ClientboundEntityPositionSyncPacket(ENTITY_ID, PositionPath.of(SERVER_POSITION), YAW, PITCH, true),
                turned(Pig.class));

        assertEquals(SEAT_POSITION, seated.position().endPosition());
        assertEquals(YAW + QUARTER_TURN, seated.yRot());
        assertEquals(PITCH, seated.xRot());
    }

    @Test
    void aMinecartStepSentToAQuarterTurnSeatCarriesItsMovementAndYawTurned() {
        ClientboundMoveMinecartPacket seated = ClientboundPacketRewriters.moveMinecart(
                new ClientboundMoveMinecartPacket(ENTITY_ID, List.of(
                        new NewMinecartBehavior.MinecartStep(SERVER_POSITION, MOVEMENT, YAW, PITCH, STEP_WEIGHT))),
                turned(Pig.class));

        assertEquals(List.of(new NewMinecartBehavior.MinecartStep(
                SEAT_POSITION, TURNED_MOVEMENT, YAW + QUARTER_TURN, PITCH, STEP_WEIGHT)), seated.lerpSteps());
    }

    @Test
    void aRelativeMoveSentToAQuarterTurnSeatIsTurned() {
        ClientboundMoveEntityPacket seated = (ClientboundMoveEntityPacket) EntityPacketTurns.moveEntity(
                new ClientboundMoveEntityPacket.PosRot(
                        ENTITY_ID, STEP, Mth.packDegrees(YAW), Mth.packDegrees(PITCH), true),
                turned(Pig.class));

        assertEquals(ClientboundMoveEntityPacket.PosRot.class, seated.getClass());
        assertEquals(ENTITY_ID, seated.entityId);
        assertEquals(TURNED_STEP, seated.getPositionDelta());
        assertEquals(YAW + QUARTER_TURN, seated.getYRot());
        assertEquals(PITCH, seated.getXRot());
        assertEquals(true, seated.isOnGround());
    }

    @Test
    void aSteppedRelativeMoveSentToAQuarterTurnSeatIsTurnedStepByStep() {
        VecDelta stepped = new VecDelta.Stepped(List.of(
                new VecDelta.Stepped.DeltaStep((short) 100, (short) 7, (short) 300, 1),
                new VecDelta.Stepped.DeltaStep((short) -5, (short) 0, (short) 9, 2)));

        ClientboundMoveEntityPacket seated = (ClientboundMoveEntityPacket) EntityPacketTurns.moveEntity(
                new ClientboundMoveEntityPacket.Pos(ENTITY_ID, stepped, false), turned(Pig.class));

        assertEquals(new VecDelta.Stepped(List.of(
                new VecDelta.Stepped.DeltaStep((short) -300, (short) 7, (short) 100, 1),
                new VecDelta.Stepped.DeltaStep((short) -9, (short) 0, (short) -5, 2))), seated.getPositionDelta());
    }

    @Test
    void aRotationSentToAQuarterTurnSeatIsTurned() {
        ClientboundMoveEntityPacket seated = (ClientboundMoveEntityPacket) EntityPacketTurns.moveEntity(
                new ClientboundMoveEntityPacket.Rot(ENTITY_ID, Mth.packDegrees(YAW), Mth.packDegrees(PITCH), true),
                turned(Pig.class));

        assertEquals(ClientboundMoveEntityPacket.Rot.class, seated.getClass());
        assertEquals(YAW + QUARTER_TURN, seated.getYRot());
        assertEquals(PITCH, seated.getXRot());
    }

    @Test
    void aHeadRotationSentToAQuarterTurnSeatIsTurned() {
        ClientboundRotateHeadPacket seated = (ClientboundRotateHeadPacket) EntityPacketTurns.rotateHead(
                headRotation(HEAD_YAW), turned(Pig.class));

        assertEquals(ENTITY_ID, seated.entityId);
        assertEquals(HEAD_YAW + QUARTER_TURN, seated.getYHeadRot());
    }

    @Test
    void aVelocitySentToAQuarterTurnSeatIsTurned() {
        ClientboundSetEntityMotionPacket seated = (ClientboundSetEntityMotionPacket) EntityPacketTurns.entityMotion(
                new ClientboundSetEntityMotionPacket(ENTITY_ID, MOVEMENT), turned(Pig.class));

        assertEquals(new ClientboundSetEntityMotionPacket(ENTITY_ID, TURNED_MOVEMENT), seated);
    }

    @Test
    void aSyncedDirectionSentToAQuarterTurnSeatIsTurned() {
        ClientboundSetEntityDataPacket seated = ClientboundPacketRewriters.setEntityData(
                new ClientboundSetEntityDataPacket(ENTITY_ID, List.of(
                        new SynchedEntityData.DataValue<>(DIRECTION_SLOT, EntityDataSerializers.DIRECTION, Direction.NORTH))),
                turned(ItemFrame.class));

        assertEquals(Direction.EAST, seated.packedItems().getFirst().value());
    }

    @Test
    void anExplosionsKnockbackIsTurnedByTheCopyThePlayerStandsOn() {
        ClientboundExplodePacket seated = ClientboundPacketRewriters.explode(
                new ClientboundExplodePacket(SERVER_POSITION, EXPLOSION_RADIUS, 0, Optional.of(MOVEMENT),
                        ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE, WeightedList.of(), true),
                turned(Pig.class));

        assertEquals(Optional.of(TURNED_MOVEMENT), seated.playerKnockback());
    }

    @Test
    void aTranslationSeamLeavesAnAddedEntityAsSent() {
        ClientboundAddEntityPacket seated = ClientboundPacketRewriters.addEntity(
                spawn(Vec3.atLowerCornerOf(SERVER_BLOCK), Direction.NORTH.get3DDataValue()), context(TORUS, ItemFrame.class));

        assertEquals(Direction.NORTH.get3DDataValue(), seated.getData());
        assertEquals(Vec3.atLowerCornerOf(SERVER_BLOCK), new Vec3(seated.getX(), seated.getY(), seated.getZ()));
        assertEquals(YAW, seated.getYRot());
        assertEquals(HEAD_YAW, seated.getYHeadRot());
        assertSame(MOVEMENT, seated.getMovement());
    }

    @Test
    void aTranslationSeamHandsTheFrequentEntityPacketsOnUntouched() {
        TranslationContext context = context(TORUS, Pig.class);
        List<Packet<?>> sent = List.of(
                new ClientboundMoveEntityPacket.PosRot(ENTITY_ID, STEP, Mth.packDegrees(YAW), Mth.packDegrees(PITCH), true),
                new ClientboundMoveEntityPacket.Pos(ENTITY_ID, STEP, true),
                new ClientboundMoveEntityPacket.Rot(ENTITY_ID, Mth.packDegrees(YAW), Mth.packDegrees(PITCH), true),
                headRotation(HEAD_YAW),
                new ClientboundSetEntityMotionPacket(ENTITY_ID, MOVEMENT));

        for (Packet<?> packet : sent) {
            assertSame(packet, PacketTranslator.toClient(packet, context));
        }
    }

    @Test
    void aTurnedFoldRoutesTheFrequentEntityPacketsThroughTheirRewriters() {
        Packet<?> seated = PacketTranslator.toClient(
                new ClientboundSetEntityMotionPacket(ENTITY_ID, MOVEMENT), turned(Pig.class));

        assertEquals(new ClientboundSetEntityMotionPacket(ENTITY_ID, TURNED_MOVEMENT), seated);
    }
}
