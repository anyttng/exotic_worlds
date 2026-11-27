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
import com.exoticworlds.engine.net.RecordPayloadFold.ComponentRole;
import com.exoticworlds.engine.net.RecordPayloadFold.Role;
import com.exoticworlds.engine.net.TagPositions.Nesting;
import com.exoticworlds.engine.net.TagPositions.PositionShape;
import com.exoticworlds.engine.net.TagPositions.TagPosition;

import net.minecraft.resources.Identifier;

class PositionRowsTest {
    private static final Identifier HOLDER_ID = Identifier.fromNamespaceAndPath("cject", "holder");
    private static final Identifier MISSING_ID = Identifier.fromNamespaceAndPath("cject", "missing");
    private static final Identifier OFFSET_PAYLOAD_ID = Identifier.fromNamespaceAndPath("cject", "offset_probe");
    private static final Identifier SIZE_PAYLOAD_ID = Identifier.fromNamespaceAndPath("pack", "size_probe");
    private static final Identifier CURVE_PAYLOAD_ID = Identifier.fromNamespaceAndPath("pack", "curve_probe");
    private static final Identifier ANCHOR_COMPONENT_ID = Identifier.fromNamespaceAndPath("cject", "anchor");
    private static final Identifier TARGET_COMPONENT_ID = Identifier.fromNamespaceAndPath("pack", "target");

    private static final Identifier CJECT_FILE = Identifier.fromNamespaceAndPath("cject", PositionRows.FILE_NAME);
    private static final Identifier PACK_FILE = Identifier.fromNamespaceAndPath("pack", PositionRows.FILE_NAME);

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

    private static ComponentRole direct(Role role) {
        return new ComponentRole.Direct(role);
    }

    private static ComponentRole nested(Map<String, ComponentRole> components) {
        return new ComponentRole.Nested(components);
    }

    private static PositionRows mergedPayloads(String cject, String pack) {
        return PositionRows.merge(Map.of(CJECT_FILE, parse(cject).getOrThrow(), PACK_FILE, parse(pack).getOrThrow()),
                id -> true, id -> true, id -> true);
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
        assertTrue(rows.payloads().isEmpty());
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

    @Test
    void thePayloadRowsDecodeToRoles() {
        PositionRows rows = parse("""
                {
                  "payloads": {
                    "cject:offset_probe": { "from": "group", "to": "group" },
                    "pack:size_probe": { "origin": "world", "size": "untouched" }
                  }
                }
                """).getOrThrow();

        assertEquals(Map.of(
                        OFFSET_PAYLOAD_ID, Map.of("from", direct(Role.GROUP), "to", direct(Role.GROUP)),
                        SIZE_PAYLOAD_ID, Map.of("origin", direct(Role.WORLD), "size", direct(Role.UNTOUCHED))),
                rows.payloads());
    }

    @Test
    void aNestedObjectDecodesToTheRolesOfTheRecordItHolds() {
        PositionRows rows = parse("""
                {
                  "payloads": {
                    "pack:curve_probe": {
                      "curve": { "start": "world", "tangent": "untouched" },
                      "arm": { "motion": { "side": "untouched" } }
                    }
                  }
                }
                """).getOrThrow();

        assertEquals(Map.of(
                        "curve", nested(Map.of("start", direct(Role.WORLD), "tangent", direct(Role.UNTOUCHED))),
                        "arm", nested(Map.of("motion", nested(Map.of("side", direct(Role.UNTOUCHED)))))),
                rows.payloads().get(CURVE_PAYLOAD_ID));
    }

    @Test
    void aNestedRowEncodesBackToTheSameRow() {
        PositionRows rows = parse("""
                { "payloads": { "pack:curve_probe": { "curve": { "tangent": "untouched" } } } }
                """).getOrThrow();

        assertEquals(rows, parse(PositionRows.CODEC.encodeStart(JsonOps.INSTANCE, rows).getOrThrow().toString())
                .getOrThrow());
    }

    @Test
    void aRoleNobodyNamedFailsTheFile() {
        assertTrue(parse("""
                { "payloads": { "cject:offset_probe": { "offset": "local" } } }
                """).isError());
    }

    @Test
    void twoNamespacesNamingOnePayloadJoinTheirComponents() {
        PositionRows cject = parse("""
                { "payloads": { "pack:size_probe": { "origin": "world" } } }
                """).getOrThrow();
        PositionRows pack = parse("""
                { "payloads": { "pack:size_probe": { "size": "untouched" } } }
                """).getOrThrow();

        PositionRows merged =
                PositionRows.merge(Map.of(CJECT_FILE, cject, PACK_FILE, pack), id -> true, id -> true, id -> true);

        assertEquals(Map.of("origin", direct(Role.WORLD), "size", direct(Role.UNTOUCHED)),
                merged.payloads().get(SIZE_PAYLOAD_ID));
    }

    @Test
    void twoNamespacesNestingIntoOneComponentJoinTheirNestedRoles() {
        PositionRows merged = mergedPayloads("""
                { "payloads": { "pack:curve_probe": { "curve": { "start": "world" } } } }
                """, """
                { "payloads": { "pack:curve_probe": { "curve": { "tangent": "untouched" } } } }
                """);

        assertEquals(Map.of("curve", nested(Map.of("start", direct(Role.WORLD), "tangent", direct(Role.UNTOUCHED)))),
                merged.payloads().get(CURVE_PAYLOAD_ID));
    }

    @Test
    void aRoleAndANestedObjectOnOneComponentAreDroppedAndTheRestKept() {
        PositionRows merged = mergedPayloads("""
                { "payloads": { "pack:curve_probe": { "curve": "untouched", "origin": "world" } } }
                """, """
                { "payloads": { "pack:curve_probe": { "curve": { "tangent": "untouched" } } } }
                """);

        assertEquals(Map.of("origin", direct(Role.WORLD)), merged.payloads().get(CURVE_PAYLOAD_ID));
    }

    @Test
    void aNestedComponentTwoNamespacesNameDifferentlyIsDroppedAndItsSiblingsKept() {
        PositionRows merged = mergedPayloads("""
                { "payloads": { "pack:curve_probe": { "curve": { "start": "world", "tangent": "untouched" } } } }
                """, """
                { "payloads": { "pack:curve_probe": { "curve": { "tangent": "group" } } } }
                """);

        assertEquals(Map.of("curve", nested(Map.of("start", direct(Role.WORLD)))),
                merged.payloads().get(CURVE_PAYLOAD_ID));
    }

    @Test
    void aComponentTwoNamespacesNameDifferentlyIsDroppedAndTheRestKept() {
        PositionRows cject = parse("""
                { "payloads": { "pack:size_probe": { "origin": "world", "size": "untouched" } } }
                """).getOrThrow();
        PositionRows pack = parse("""
                { "payloads": { "pack:size_probe": { "size": "group" } } }
                """).getOrThrow();

        PositionRows merged =
                PositionRows.merge(Map.of(CJECT_FILE, cject, PACK_FILE, pack), id -> true, id -> true, id -> true);

        assertEquals(Map.of("origin", direct(Role.WORLD)), merged.payloads().get(SIZE_PAYLOAD_ID));
    }
}
