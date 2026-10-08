package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.exoticworlds.engine.net.TagPositions.Nesting;
import com.exoticworlds.engine.net.TagPositions.PositionShape;
import com.exoticworlds.engine.net.TagPositions.TagPosition;

import net.minecraft.resources.ResourceLocation;

class PositionRowsTest {
    private static final ResourceLocation HOLDER_ID = ResourceLocation.fromNamespaceAndPath("cject", "holder");
    private static final ResourceLocation MISSING_ID = ResourceLocation.fromNamespaceAndPath("cject", "missing");
    private static final ResourceLocation OFFSET_PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath("cject", "offset_probe");
    private static final ResourceLocation SIZE_PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath("pack", "size_probe");
    private static final ResourceLocation ANCHOR_COMPONENT_ID = ResourceLocation.fromNamespaceAndPath("cject", "anchor");
    private static final ResourceLocation TARGET_COMPONENT_ID = ResourceLocation.fromNamespaceAndPath("pack", "target");

    private static final ResourceLocation CJECT_FILE = ResourceLocation.fromNamespaceAndPath("cject", PositionRows.FILE_NAME);
    private static final ResourceLocation PACK_FILE = ResourceLocation.fromNamespaceAndPath("pack", PositionRows.FILE_NAME);

    private static final String EVERY_FORM = """
            {
              "block_entities": {
                "cject:holder": {
                  "Goal": "packed_long",
                  "Printer": { "Anchor": "block_pos" },
                  "FlyingBlocks": [ { "Target": "vec3_list" } ]
                }
              },
              "entities": {
                "cject:holder": { "block_pos": "block_pos" }
              }
            }
            """;

    private static final Set<TagPosition> EVERY_FORM_POSITIONS = Set.of(
            new TagPosition("Goal", PositionShape.PACKED_LONG),
            new TagPosition(Nesting.COMPOUND, "Printer", "Anchor", PositionShape.BLOCK_POS),
            new TagPosition(Nesting.EACH_OF_LIST, "FlyingBlocks", "Target", PositionShape.VEC3_LIST));

    private static DataResult<PositionRows> parse(String json) {
        return PositionRows.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    @Test
    void everyAddressFormDecodesToItsPosition() {
        PositionRows rows = parse(EVERY_FORM).getOrThrow();

        assertEquals(EVERY_FORM_POSITIONS, Set.copyOf(rows.blockEntities().get(HOLDER_ID)));
        assertEquals(List.of(new TagPosition("block_pos", PositionShape.BLOCK_POS)), rows.entities().get(HOLDER_ID));
    }

    @Test
    void aFileWithNeitherSectionDecodesEmpty() {
        PositionRows rows = parse("{}").getOrThrow();

        assertTrue(rows.blockEntities().isEmpty());
        assertTrue(rows.entities().isEmpty());
        assertTrue(rows.components().isEmpty());
        assertTrue(rows.deny().isEmpty());
    }

    @Test
    void theComponentListDecodesToComponentIds() {
        PositionRows rows = parse("""
                { "components": [ "cject:anchor", "pack:target" ] }
                """).getOrThrow();

        assertEquals(Set.of(ANCHOR_COMPONENT_ID, TARGET_COMPONENT_ID), rows.components());
    }

    @Test
    void aComponentNoModRegistersIsSkippedAndTheRestKept() {
        PositionRows cject = parse("""
                { "components": [ "cject:anchor", "cject:missing" ] }
                """).getOrThrow();
        PositionRows pack = parse("""
                { "components": [ "pack:target" ] }
                """).getOrThrow();

        PositionRows merged = PositionRows.merge(Map.of(CJECT_FILE, cject, PACK_FILE, pack), id -> true, id -> true,
                id -> !id.equals(MISSING_ID));

        assertEquals(Set.of(ANCHOR_COMPONENT_ID, TARGET_COMPONENT_ID), merged.components());
    }

    @Test
    void theDenyListDecodesToPayloadIds() {
        PositionRows rows = parse("""
                { "deny": [ "cject:offset_probe" ] }
                """).getOrThrow();

        assertEquals(Set.of(OFFSET_PAYLOAD_ID), rows.deny());
    }

    @Test
    void theDenyListsOfEveryNamespaceJoin() {
        PositionRows cject = parse("""
                { "deny": [ "cject:offset_probe" ] }
                """).getOrThrow();
        PositionRows pack = parse("""
                { "deny": [ "pack:size_probe" ] }
                """).getOrThrow();

        PositionRows merged =
                PositionRows.merge(Map.of(CJECT_FILE, cject, PACK_FILE, pack), id -> true, id -> true, id -> true);

        assertEquals(Set.of(OFFSET_PAYLOAD_ID, SIZE_PAYLOAD_ID), merged.deny());
    }

    @Test
    void aShapeNobodyNamedFailsTheFile() {
        assertTrue(parse("""
                { "block_entities": { "cject:holder": { "Goal": "packed_int" } } }
                """).isError());
    }

    @Test
    void aListFormNamingTwoCompoundsFailsTheFile() {
        assertTrue(parse("""
                { "block_entities": { "cject:holder": { "FlyingBlocks": [ { "Target": "block_pos" }, {} ] } } }
                """).isError());
    }

    @Test
    void anIdNoModRegistersIsSkippedAndTheRestKept() {
        PositionRows file = parse("""
                {
                  "block_entities": {
                    "cject:holder": { "Goal": "packed_long" },
                    "cject:missing": { "Goal": "packed_long" }
                  }
                }
                """).getOrThrow();

        PositionRows merged = PositionRows.merge(Map.of(CJECT_FILE, file), HOLDER_ID::equals, id -> true, id -> true);

        assertEquals(Set.of(HOLDER_ID), merged.blockEntities().keySet());
        assertTrue(!merged.blockEntities().containsKey(MISSING_ID));
    }

    @Test
    void twoNamespacesDeclaringOneIdJoinTheirRows() {
        PositionRows cject = parse("""
                { "block_entities": { "cject:holder": { "Goal": "packed_long" } } }
                """).getOrThrow();
        PositionRows pack = parse("""
                { "block_entities": { "cject:holder": { "Printer": { "Anchor": "block_pos" } } } }
                """).getOrThrow();

        PositionRows merged =
                PositionRows.merge(Map.of(CJECT_FILE, cject, PACK_FILE, pack), id -> true, id -> true, id -> true);

        assertEquals(Set.of(
                        new TagPosition("Goal", PositionShape.PACKED_LONG),
                        new TagPosition(Nesting.COMPOUND, "Printer", "Anchor", PositionShape.BLOCK_POS)),
                Set.copyOf(merged.blockEntities().get(HOLDER_ID)));
    }
}
