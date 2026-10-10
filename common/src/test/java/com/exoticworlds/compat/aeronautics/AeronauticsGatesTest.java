package com.exoticworlds.compat.aeronautics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import com.exoticworlds.compat.ModPresence;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class AeronauticsGatesTest {
    private static final String CONFIG = "exotic_worlds.compat.aeronautics.mixins.json";
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";

    private static final Map<String, ModPresence> GATE_BY_TARGET_PACKAGE = Map.of(
            "dev/eriksonn/aeronautics/", AeronauticsMod.GATE,
            "dev/simulated_team/simulated/", SimulatedMod.GATE,
            "dev/ryanhcode/offroad/", OffroadMod.GATE);

    @Test
    void everyMixinIsGatedByTheModItsTargetBelongsTo() throws IOException {
        JsonObject config = read();
        String mixinPackage = config.get("package").getAsString();
        List<String> mixins = new ArrayList<>();
        for (JsonElement mixin : config.getAsJsonArray("mixins")) {
            mixins.add(mixin.getAsString());
        }

        assertFalse(mixins.isEmpty(), "the Aeronautics config lists no mixin");
        for (String mixin : mixins) {
            String mixinClass = mixinPackage + "." + mixin;
            String target = targetOf(mixinClass);
            ModPresence owner = GATE_BY_TARGET_PACKAGE.entrySet().stream()
                    .filter(entry -> target.startsWith(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            assertNotNull(owner, mixin + " targets " + target + ", which no bundled mod owns");
            for (ModPresence gate : GATE_BY_TARGET_PACKAGE.values()) {
                assertEquals(gate == owner, gate.covers(mixinClass),
                        mixin + " targets " + target + " and is not gated by that mod alone");
            }
        }
    }

    private static String targetOf(String mixinClass) throws IOException {
        try (InputStream bytes = AeronauticsGatesTest.class.getClassLoader()
                .getResourceAsStream(mixinClass.replace('.', '/') + ".class")) {
            assertNotNull(bytes, mixinClass + " is not on the test classpath");
            ClassNode node = new ClassNode();
            new ClassReader(bytes.readAllBytes()).accept(node, ClassReader.SKIP_CODE);
            for (AnnotationNode annotation : node.invisibleAnnotations == null ? List.<AnnotationNode>of()
                    : node.invisibleAnnotations) {
                if (annotation.desc.equals(MIXIN)) {
                    List<?> values = (List<?>) annotation.values.get(annotation.values.indexOf("value") + 1);
                    return ((Type) values.get(0)).getInternalName();
                }
            }
        }

        throw new AssertionError(mixinClass + " carries no @Mixin");
    }

    private static JsonObject read() throws IOException {
        try (InputStream stream = AeronauticsGatesTest.class.getClassLoader().getResourceAsStream(CONFIG)) {
            assertNotNull(stream, CONFIG + " is not on the test classpath");
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }
}
