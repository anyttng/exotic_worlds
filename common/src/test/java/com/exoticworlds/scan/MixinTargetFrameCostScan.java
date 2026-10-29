package com.exoticworlds.scan;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class MixinTargetFrameCostScan {
    private static final String REPO_ROOT_PROPERTY = "toroidal.repoRoot";
    private static final String RESOURCES = "common/src/main/resources";
    private static final String CONFIGS = "exotic_worlds*.mixins.json";
    private static final String PACKAGE_KEY = "package";
    private static final List<String> MIXIN_LIST_KEYS = List.of("mixins", "client", "server");
    private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String VALUE_KEY = "value";
    private static final String TARGETS_KEY = "targets";
    private static final String SET_COMPUTE_LIMITS = "setComputeLimits";
    private static final String LIMIT_EXCEPTION = "LimitExceededException";
    private static final String OBJECT = "java/lang/Object";
    private static final long LIMIT = 100_000_000L;
    private static final long DETAIL_FLOOR = 50_000_000L;
    private static final long CEILING = 2_000_000_000L;
    private static final long RESOLUTION = 100_000L;
    private static final Set<String> STRIPPED_BEFORE_MIXIN = Set.of("xaero/map/gui/GuiMap");
    private static final Path REPORT = Path.of(System.getProperty("toroidal.reports", "build/reports"))
            .resolve("scan").resolve("mixin-target-frame-cost.txt");

    private record Measured(String target, long ops, int methods, String heaviest, long heaviestOps,
            Set<String> mixins) {
        String line() {
            return String.format(Locale.ROOT, "%,14d ops  %s  methods=%d%s  mixins=%s", ops, target, methods,
                    heaviest.isEmpty() ? "" : String.format(Locale.ROOT, "  heaviest=%s (%,d ops)", heaviest,
                            heaviestOps), String.join(",", mixins));
        }
    }

    @Test
    void everyMixinTargetClassFitsTheFrameBudget() throws IOException {
        Method limits = computeLimits();
        assumeTrue(limits != null, "the test runtime's ASM has no frame budget to measure against");

        ClassLoader loader = MixinTargetFrameCostScan.class.getClassLoader();
        Map<String, Set<String>> targets = targets(loader);
        assertTrue(!targets.isEmpty(), "no mixin of the configs under " + RESOURCES + " is on this runtime");
        List<Measured> measured = new ArrayList<>();
        List<String> absent = new ArrayList<>();
        List<String> unmeasured = new ArrayList<>();
        for (Map.Entry<String, Set<String>> target : targets.entrySet()) {
            byte[] bytes = read(loader, target.getKey() + ".class");
            if (bytes == null) {
                absent.add(target.getKey() + "  mixins=" + String.join(",", target.getValue()));
                continue;
            }

            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            try {
                long ops = operations(limits, node);
                String heaviest = "";
                long heaviestOps = 0;
                if (ops > DETAIL_FLOOR) {
                    for (MethodNode method : node.methods) {
                        long methodOps = operations(limits, alone(node, method));
                        if (methodOps > heaviestOps) {
                            heaviestOps = methodOps;
                            heaviest = method.name + method.desc;
                        }
                    }
                }

                measured.add(new Measured(target.getKey(), ops, node.methods.size(), heaviest, heaviestOps,
                        target.getValue()));
            } catch (RuntimeException refused) {
                unmeasured.add(target.getKey() + "  " + refused);
            }
        }

        measured.sort((left, right) -> Long.compare(right.ops(), left.ops()));
        List<String> lines = new ArrayList<>();
        lines.add("limit: " + String.format(Locale.ROOT, "%,d", LIMIT) + " ops per method (ASM 9.11); a class's figure"
                + " is its heaviest method, resolved to " + String.format(Locale.ROOT, "%,d", RESOLUTION) + " ops;"
                + " methods are listed above " + String.format(Locale.ROOT, "%,d", DETAIL_FLOOR));
        lines.add("target classes: " + targets.size() + ", measured: " + measured.size());
        lines.add("");
        for (Measured entry : measured) {
            lines.add(entry.line());
        }

        lines.add("");
        lines.add("absent on this runtime: " + absent.size());
        lines.addAll(absent);
        lines.add("");
        lines.add("unmeasured: " + unmeasured.size());
        lines.addAll(unmeasured);
        Files.createDirectories(REPORT.getParent());
        Files.write(REPORT, lines);

        List<String> over = new ArrayList<>();
        for (Measured entry : measured) {
            if (entry.ops() > LIMIT && !STRIPPED_BEFORE_MIXIN.contains(entry.target())) {
                over.add(entry.line());
            }
        }

        assertTrue(over.isEmpty(), "mixin target classes over the frame budget with no strip before Mixin: " + over);
    }

    private static Map<String, Set<String>> targets(ClassLoader loader) throws IOException {
        Map<String, Set<String>> targets = new TreeMap<>();
        for (Path config : configs()) {
            JsonObject json = JsonParser.parseString(Files.readString(config)).getAsJsonObject();
            String mixinPackage = json.get(PACKAGE_KEY).getAsString();
            for (String mixin : mixinsOf(json)) {
                String mixinClass = mixinPackage + "." + mixin;
                byte[] bytes = read(loader, mixinClass.replace('.', '/') + ".class");
                if (bytes == null) {
                    continue;
                }

                ClassNode node = new ClassNode();
                new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
                for (String target : targetsOf(node)) {
                    targets.computeIfAbsent(target, key -> new TreeSet<>()).add(mixin);
                }
            }
        }

        return targets;
    }

    private static List<Path> configs() throws IOException {
        List<Path> configs = new ArrayList<>();
        try (DirectoryStream<Path> found = Files.newDirectoryStream(
                Path.of(System.getProperty(REPO_ROOT_PROPERTY)).resolve(RESOURCES), CONFIGS)) {
            found.forEach(configs::add);
        }

        assertTrue(!configs.isEmpty(), "no mixin config found under " + RESOURCES);
        return configs;
    }

    private static List<String> mixinsOf(JsonObject config) {
        List<String> mixins = new ArrayList<>();
        for (String key : MIXIN_LIST_KEYS) {
            if (config.get(key) instanceof JsonArray list) {
                for (JsonElement mixin : list) {
                    mixins.add(mixin.getAsString());
                }
            }
        }

        return mixins;
    }

    private static List<String> targetsOf(ClassNode mixin) {
        List<String> targets = new ArrayList<>();
        for (AnnotationNode annotation : annotationsOf(mixin)) {
            if (MIXIN_ANNOTATION.equals(annotation.desc) && annotation.values != null) {
                targets.addAll(targetsIn(annotation));
            }
        }

        return targets;
    }

    private static List<AnnotationNode> annotationsOf(ClassNode mixin) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (mixin.visibleAnnotations != null) {
            annotations.addAll(mixin.visibleAnnotations);
        }

        if (mixin.invisibleAnnotations != null) {
            annotations.addAll(mixin.invisibleAnnotations);
        }

        return annotations;
    }

    private static List<String> targetsIn(AnnotationNode mixinAnnotation) {
        List<String> targets = new ArrayList<>();
        for (int index = 0; index < mixinAnnotation.values.size(); index += 2) {
            Object values = mixinAnnotation.values.get(index + 1);
            if (VALUE_KEY.equals(mixinAnnotation.values.get(index)) && values instanceof List<?> classes) {
                for (Object clazz : classes) {
                    targets.add(((Type) clazz).getInternalName());
                }
            } else if (TARGETS_KEY.equals(mixinAnnotation.values.get(index)) && values instanceof List<?> names) {
                for (Object name : names) {
                    targets.add(((String) name).replace('.', '/'));
                }
            }
        }

        return targets;
    }

    private static long operations(Method limits, ClassNode node) {
        long low = 0;
        long high = CEILING;
        while (high - low > RESOLUTION) {
            long middle = (low + high) / 2;
            if (exceeds(limits, node, middle)) {
                low = middle;
            } else {
                high = middle;
            }
        }

        return high;
    }

    private static boolean exceeds(Method limits, ClassNode node, long limit) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String first, String second) {
                return OBJECT;
            }
        };
        try {
            limits.invoke(writer, Integer.MAX_VALUE, limit);
            node.accept(writer);
            return false;
        } catch (IllegalAccessException | InvocationTargetException unreachable) {
            throw new IllegalStateException(SET_COMPUTE_LIMITS, unreachable);
        } catch (RuntimeException refused) {
            if (LIMIT_EXCEPTION.equals(refused.getClass().getSimpleName())) {
                return true;
            }

            throw refused;
        }
    }

    private static ClassNode alone(ClassNode node, MethodNode method) {
        ClassNode one = new ClassNode();
        node.accept(one);
        one.methods.removeIf(candidate -> !(candidate.name.equals(method.name) && candidate.desc.equals(method.desc)));
        return one;
    }

    private static Method computeLimits() {
        try {
            return ClassWriter.class.getMethod(SET_COMPUTE_LIMITS, int.class, long.class);
        } catch (NoSuchMethodException absent) {
            return null;
        }
    }

    private static byte[] read(ClassLoader loader, String resource) {
        try (InputStream stream = loader.getResourceAsStream(resource)) {
            return stream == null ? null : stream.readAllBytes();
        } catch (IOException unreadable) {
            return null;
        }
    }
}
