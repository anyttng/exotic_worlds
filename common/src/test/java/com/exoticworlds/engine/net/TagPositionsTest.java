package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

class TagPositionsTest {
    private static final int LAP_BLOCKS = 512;

    private static final String SUBJECT = "Test positions";

    private static final Identifier DECLARED_ID = Identifier.fromNamespaceAndPath("cject", "holder");
    private static final Identifier OTHER_ID = Identifier.fromNamespaceAndPath("cject", "relay");

    private static final String PACKED_KEY = "Goal";
    private static final String BLOCK_POS_KEY = "ControllerPos";
    private static final String VEC3_KEY = "CurrentTarget";
    private static final String UNRELATED_KEY = "DesiredLength";
    private static final double UNRELATED_VALUE = 4.0;
    private static final String COMPOUND_KEY = "Printer";
    private static final String LIST_KEY = "FlyingBlocks";

    private static final List<TagPositions.TagPosition> EVERY_SHAPE = List.of(
            new TagPositions.TagPosition(PACKED_KEY, TagPositions.PositionShape.PACKED_LONG),
            new TagPositions.TagPosition(BLOCK_POS_KEY, TagPositions.PositionShape.BLOCK_POS),
            new TagPositions.TagPosition(VEC3_KEY, TagPositions.PositionShape.VEC3_LIST));

    private static final List<TagPositions.TagPosition> EVERY_SHAPE_IN_COMPOUND =
            addressed(TagPositions.Nesting.COMPOUND, COMPOUND_KEY);
    private static final List<TagPositions.TagPosition> EVERY_SHAPE_IN_EACH_OF_LIST =
            addressed(TagPositions.Nesting.EACH_OF_LIST, LIST_KEY);

    private interface Anchored {
    }

    private static final class AnchoredSubject implements Anchored {
    }

    private static class Hung {
    }

    private static final class HungSubject extends Hung {
    }

    private static final class PackedSubject {
    }

    private static final class UnregisteredSubject {
    }

    private static final class HomeLap implements TagPositions.Seat {
        private final List<String> overloads = new ArrayList<>();

        @Override
        public BlockPos seat(BlockPos stored) {
            overloads.add("BlockPos");
            return new BlockPos(homeX(stored.getX()), stored.getY(), stored.getZ());
        }

        @Override
        public Vec3 seat(Vec3 stored) {
            overloads.add("Vec3");
            return new Vec3(homeX((int) Math.floor(stored.x)) + stored.x - Math.floor(stored.x), stored.y, stored.z);
        }

        private static int homeX(int x) {
            return x - Math.floorDiv(x, LAP_BLOCKS) * LAP_BLOCKS;
        }
    }

    private static ListTag doubleList(double x, double y, double z) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(x));
        list.add(DoubleTag.valueOf(y));
        list.add(DoubleTag.valueOf(z));
        return list;
    }

    private static Vec3 vec3In(CompoundTag tag, String key) {
        ListTag list = tag.getListOrEmpty(key);
        return new Vec3(list.getDoubleOr(0, Double.NaN), list.getDoubleOr(1, Double.NaN),
                list.getDoubleOr(2, Double.NaN));
    }

    private static BlockPos blockPosIn(CompoundTag tag, String key) {
        return tag.read(key, BlockPos.CODEC).orElseThrow();
    }

    private static BlockPos packedIn(CompoundTag tag) {
        return BlockPos.of(tag.getLong(PACKED_KEY).orElseThrow());
    }

    private static List<TagPositions.TagPosition> addressed(TagPositions.Nesting nesting, String container) {
        return EVERY_SHAPE.stream()
                .map(position -> new TagPositions.TagPosition(nesting, container, position.key(), position.shape()))
                .toList();
    }

    private static CompoundTag everyShapeALapOut() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
        tag.store(BLOCK_POS_KEY, BlockPos.CODEC, new BlockPos(7 + LAP_BLOCKS, 102, 0));
        tag.put(VEC3_KEY, doubleList(0.5 + LAP_BLOCKS, 0.5, 0.5));
        return tag;
    }

    private static void assertEveryShapeHome(CompoundTag tag, String where) {
        assertEquals(new BlockPos(3, 102, 0), packedIn(tag), where);
        assertEquals(new BlockPos(7, 102, 0), blockPosIn(tag, BLOCK_POS_KEY), where);
        assertEquals(new Vec3(0.5, 0.5, 0.5), vec3In(tag, VEC3_KEY), where);
    }

    private static CompoundTag blockPosAt(BlockPos position) {
        CompoundTag tag = new CompoundTag();
        tag.store(BLOCK_POS_KEY, BlockPos.CODEC, position);
        return tag;
    }

    private static TagPositions.Subject subject(Class<?> type) {
        return new TagPositions.Subject(type, null);
    }

    private static Map<Identifier, List<TagPositions.TagPosition>> blockPosDeclaredFor(Identifier id) {
        return Map.of(id, List.of(new TagPositions.TagPosition(BLOCK_POS_KEY, TagPositions.PositionShape.BLOCK_POS)));
    }

    @Test
    void everyShapeComesBackOnTheSeatedCopy() {
        CompoundTag tag = everyShapeALapOut();

        assertEveryShapeHome(TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag), "top level");
    }

    @Test
    void everyShapeInsideANestedCompoundComesBackOnTheSeatedCopy() {
        CompoundTag printer = everyShapeALapOut();
        printer.putDouble(UNRELATED_KEY, UNRELATED_VALUE);
        CompoundTag tag = new CompoundTag();
        tag.put(COMPOUND_KEY, printer);
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_COMPOUND, tag);

        assertEveryShapeHome(seated.getCompoundOrEmpty(COMPOUND_KEY), COMPOUND_KEY);
        assertEquals(UNRELATED_VALUE, seated.getCompoundOrEmpty(COMPOUND_KEY).getDoubleOr(UNRELATED_KEY, Double.NaN));
        assertEquals(UNRELATED_VALUE, seated.getDoubleOr(UNRELATED_KEY, Double.NaN));
    }

    @Test
    void everyShapeInEachCompoundOfAListComesBackOnTheSeatedCopy() {
        CompoundTag second = everyShapeALapOut();
        second.putDouble(UNRELATED_KEY, UNRELATED_VALUE);
        ListTag launched = new ListTag();
        launched.add(everyShapeALapOut());
        launched.add(second);
        CompoundTag tag = new CompoundTag();
        tag.put(LIST_KEY, launched);

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_EACH_OF_LIST, tag);

        ListTag seatedList = seated.getListOrEmpty(LIST_KEY);
        assertEveryShapeHome(seatedList.getCompoundOrEmpty(0), LIST_KEY + "[0]");
        assertEveryShapeHome(seatedList.getCompoundOrEmpty(1), LIST_KEY + "[1]");
        assertEquals(UNRELATED_VALUE, seatedList.getCompoundOrEmpty(1).getDoubleOr(UNRELATED_KEY, Double.NaN));
    }

    @Test
    void theElementsBesideAFoldedOneSurviveTheList() {
        ListTag launched = new ListTag();
        launched.add(blockPosAt(new BlockPos(7, 102, 0)));
        launched.add(blockPosAt(new BlockPos(9 + LAP_BLOCKS, 102, 0)));
        CompoundTag tag = new CompoundTag();
        tag.put(LIST_KEY, launched);

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_EACH_OF_LIST, tag);

        ListTag seatedList = seated.getListOrEmpty(LIST_KEY);
        assertEquals(new BlockPos(7, 102, 0), blockPosIn(seatedList.getCompoundOrEmpty(0), BLOCK_POS_KEY));
        assertEquals(new BlockPos(9, 102, 0), blockPosIn(seatedList.getCompoundOrEmpty(1), BLOCK_POS_KEY));
    }

    @Test
    void aNestedPositionAlreadyHomeIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.put(COMPOUND_KEY, blockPosAt(new BlockPos(7, 102, 0)));

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_COMPOUND, tag));
    }

    @Test
    void aListedPositionAlreadyHomeIsTheArgumentBack() {
        ListTag launched = new ListTag();
        launched.add(blockPosAt(new BlockPos(7, 102, 0)));
        CompoundTag tag = new CompoundTag();
        tag.put(LIST_KEY, launched);

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_EACH_OF_LIST, tag));
    }

    @Test
    void aTagWithoutTheNamedContainerIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_COMPOUND, tag));
        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_EACH_OF_LIST, tag));
    }

    @Test
    void anAddressWithoutItsContainerIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TagPositions.TagPosition(
                TagPositions.Nesting.COMPOUND, null, BLOCK_POS_KEY, TagPositions.PositionShape.BLOCK_POS));
        assertThrows(IllegalArgumentException.class, () -> new TagPositions.TagPosition(
                TagPositions.Nesting.TOP, COMPOUND_KEY, BLOCK_POS_KEY, TagPositions.PositionShape.BLOCK_POS));
    }

    @Test
    void aVec3KeyTakesTheVec3Overload() {
        CompoundTag tag = new CompoundTag();
        tag.put(VEC3_KEY, doubleList(0.5 + LAP_BLOCKS, 0.5, 0.5));
        HomeLap seat = new HomeLap();

        TagPositions.seatedIn(seat, EVERY_SHAPE, tag);

        assertEquals(List.of("Vec3"), seat.overloads);
    }

    @Test
    void aBlockKeyTakesTheBlockPosOverload() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
        HomeLap seat = new HomeLap();

        TagPositions.seatedIn(seat, EVERY_SHAPE, tag);

        assertEquals(List.of("BlockPos"), seat.overloads);
    }

    @Test
    void theKeysBesideASeatedOneSurviveTheCopy() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag);

        assertEquals(UNRELATED_VALUE, seated.getDoubleOr(UNRELATED_KEY, Double.NaN));
    }

    @Test
    void theTagHandedInIsLeftAsItWas() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag);

        assertTrue(seated != tag);
        assertEquals(new BlockPos(3 + LAP_BLOCKS, 102, 0), packedIn(tag));
    }

    @Test
    void aPositionAlreadyHomeIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3, 102, 0).asLong());

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag));
    }

    @Test
    void aTagWithoutAnyRegisteredKeyIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag));
    }

    @Test
    void aBlockPosUnderAVec3KeyIsLeftAlone() {
        CompoundTag tag = new CompoundTag();
        tag.store(VEC3_KEY, BlockPos.CODEC, new BlockPos(LAP_BLOCKS, 0, 0));

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag));
    }

    @Test
    void aVec3UnderABlockPosKeyIsLeftAlone() {
        CompoundTag tag = new CompoundTag();
        tag.put(BLOCK_POS_KEY, doubleList(LAP_BLOCKS, 0.0, 0.0));

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag));
    }

    @Test
    void anEmptyPositionListIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), List.of(), tag));
    }

    @Nested
    class Tables {
        @Test
        void aTypeCarriesTheKeysRegisteredOnItAndOnEverySupertype() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.register(Anchored.class, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);
            table.register(AnchoredSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);

            CompoundTag tag = new CompoundTag();
            tag.store(BLOCK_POS_KEY, BlockPos.CODEC, new BlockPos(7 + LAP_BLOCKS, 102, 0));
            tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());

            CompoundTag seated = table.seatedIn(new HomeLap(), subject(AnchoredSubject.class), tag);

            assertEquals(new BlockPos(7, 102, 0), blockPosIn(seated, BLOCK_POS_KEY));
            assertEquals(new BlockPos(3, 102, 0), packedIn(seated));
        }

        @Test
        void aTypeCarriesTheKeysRegisteredOnItsSuperclass() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.register(Hung.class, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);

            CompoundTag tag = new CompoundTag();
            tag.store(BLOCK_POS_KEY, BlockPos.CODEC, new BlockPos(7 + LAP_BLOCKS, 102, 0));

            CompoundTag seated = table.seatedIn(new HomeLap(), subject(HungSubject.class), tag);

            assertTrue(table.carriesPositions(subject(HungSubject.class)));
            assertEquals(new BlockPos(7, 102, 0), blockPosIn(seated, BLOCK_POS_KEY));
        }

        @Test
        void registeringATypeAgainKeepsTheKeysItAlreadyCarried() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);
            table.register(PackedSubject.class, TagPositions.PositionShape.VEC3_LIST, VEC3_KEY);

            CompoundTag tag = new CompoundTag();
            tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
            tag.put(VEC3_KEY, doubleList(0.5 + LAP_BLOCKS, 0.5, 0.5));

            CompoundTag seated = table.seatedIn(new HomeLap(), subject(PackedSubject.class), tag);

            assertEquals(new BlockPos(3, 102, 0), packedIn(seated));
            assertEquals(new Vec3(0.5, 0.5, 0.5), vec3In(seated, VEC3_KEY));
        }

        @Test
        void aRegistrationMadeAfterAFirstReadReachesTheNextOne() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);

            CompoundTag read = new CompoundTag();
            read.put(VEC3_KEY, doubleList(0.5 + LAP_BLOCKS, 0.5, 0.5));
            assertSame(read, table.seatedIn(new HomeLap(), subject(PackedSubject.class), read));

            table.register(PackedSubject.class, TagPositions.PositionShape.VEC3_LIST, VEC3_KEY);
            CompoundTag seated = table.seatedIn(new HomeLap(), subject(PackedSubject.class), read);

            assertEquals(new Vec3(0.5, 0.5, 0.5), vec3In(seated, VEC3_KEY));
        }

        @Test
        void aTypeNobodyRegisteredCarriesNothingAndGetsItsTagBack() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);

            CompoundTag tag = new CompoundTag();
            tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());

            assertTrue(table.carriesPositions(subject(PackedSubject.class)));
            assertFalse(table.carriesPositions(subject(UnregisteredSubject.class)));
            assertSame(tag, table.seatedIn(new HomeLap(), subject(UnregisteredSubject.class), tag));
        }

        @Test
        void aRegistrationWithoutAKeyIsRefused() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);

            assertThrows(IllegalArgumentException.class, () -> table.register(
                    PackedSubject.class, TagPositions.PositionShape.PACKED_LONG));
        }

        @Test
        void anAddressedRegistrationSeatsInsideTheContainerItNames() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.registerIn(PackedSubject.class, COMPOUND_KEY, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);
            table.registerInEach(PackedSubject.class, LIST_KEY, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);

            ListTag launched = new ListTag();
            launched.add(blockPosAt(new BlockPos(9 + LAP_BLOCKS, 102, 0)));
            CompoundTag tag = new CompoundTag();
            tag.put(COMPOUND_KEY, blockPosAt(new BlockPos(7 + LAP_BLOCKS, 102, 0)));
            tag.put(LIST_KEY, launched);

            CompoundTag seated = table.seatedIn(new HomeLap(), subject(PackedSubject.class), tag);

            assertEquals(new BlockPos(7, 102, 0), blockPosIn(seated.getCompoundOrEmpty(COMPOUND_KEY), BLOCK_POS_KEY));
            assertEquals(new BlockPos(9, 102, 0),
                    blockPosIn(seated.getListOrEmpty(LIST_KEY).getCompoundOrEmpty(0), BLOCK_POS_KEY));
        }

        @Test
        void oneTableKnowsNothingOfWhatAnotherWasGiven() {
            TagPositions.Table registeredInto = new TagPositions.Table(SUBJECT);
            TagPositions.Table untouched = new TagPositions.Table(SUBJECT);

            registeredInto.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);

            assertTrue(registeredInto.carriesPositions(subject(PackedSubject.class)));
            assertFalse(untouched.carriesPositions(subject(PackedSubject.class)));
        }

        @Test
        void aRowDeclaredForATypeIdSeatsTheSubjectCarryingThatId() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.declare(blockPosDeclaredFor(DECLARED_ID));
            TagPositions.Subject declared = new TagPositions.Subject(UnregisteredSubject.class, DECLARED_ID);

            CompoundTag seated = table.seatedIn(new HomeLap(), declared, blockPosAt(new BlockPos(7 + LAP_BLOCKS, 102, 0)));

            assertTrue(table.carriesPositions(declared));
            assertEquals(new BlockPos(7, 102, 0), blockPosIn(seated, BLOCK_POS_KEY));
        }

        @Test
        void aRowDeclaredForOneTypeIdLeavesAnotherIdAndAnIdlessSubjectAlone() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.declare(blockPosDeclaredFor(DECLARED_ID));

            assertFalse(table.carriesPositions(new TagPositions.Subject(UnregisteredSubject.class, OTHER_ID)));
            assertFalse(table.carriesPositions(subject(UnregisteredSubject.class)));
        }

        @Test
        void declaringAgainReplacesTheRowsAFirstReadResolved() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.declare(blockPosDeclaredFor(DECLARED_ID));
            TagPositions.Subject declared = new TagPositions.Subject(UnregisteredSubject.class, DECLARED_ID);
            CompoundTag tag = blockPosAt(new BlockPos(7 + LAP_BLOCKS, 102, 0));
            assertTrue(table.carriesPositions(declared));

            table.declare(Map.of());

            assertFalse(table.carriesPositions(declared));
            assertSame(tag, table.seatedIn(new HomeLap(), declared, tag));
        }

        @Test
        void aDeclaredRowAtAnAddressARegistrationHoldsIsDroppedForTheRegistration() {
            TagPositions.Table table = new TagPositions.Table(SUBJECT);
            table.register(PackedSubject.class, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);
            table.declare(Map.of(DECLARED_ID, List.of(
                    new TagPositions.TagPosition(BLOCK_POS_KEY, TagPositions.PositionShape.BLOCK_POS),
                    new TagPositions.TagPosition(PACKED_KEY, TagPositions.PositionShape.PACKED_LONG))));

            CompoundTag tag = blockPosAt(new BlockPos(7 + LAP_BLOCKS, 102, 0));
            tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
            HomeLap seat = new HomeLap();

            CompoundTag seated = table.seatedIn(seat, new TagPositions.Subject(PackedSubject.class, DECLARED_ID), tag);

            assertEquals(new BlockPos(7, 102, 0), blockPosIn(seated, BLOCK_POS_KEY));
            assertEquals(new BlockPos(3, 102, 0), packedIn(seated));
            assertEquals(List.of("BlockPos", "BlockPos"), seat.overloads);
        }
    }
}
