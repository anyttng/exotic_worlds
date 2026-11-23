package com.exoticworlds;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ImportRulesTest {
    private static final String REPO_ROOT_PROPERTY = "toroidal.repoRoot";
    private static final List<String> SOURCE_ROOTS =
            List.of("common/src/main/java", "neoforge/src/main/java", "fabric/src/main/java");
    private static final String PACKAGE_PATH = "com/exoticworlds";
    private static final String PACKAGE_PREFIX = "com.exoticworlds.";
    private static final String IMPORT_KEYWORD = "import ";
    private static final String STATIC_KEYWORD = "static ";
    private static final String JAVA_SUFFIX = ".java";
    private static final String MIXIN_SEGMENT = "mixin";
    private static final String ROOT_GROUP = "";
    private static final String API_V2 = "api.v2";
    private static final String BRIDGE_GROUP = "bridge";
    private static final Set<String> VERSIONED_ROOTS = Set.of("api", BRIDGE_GROUP);
    private static final Set<String> VERSIONS = Set.of("v1", "v2");
    private static final List<String> MODEL_READERS = List.of("shape", "client.shape");
    private static final Set<String> MODEL_KINDS = Set.of("interface", "record", "enum", "abstract class");
    private static final Set<String> MODEL_CLASSES = Set.of("api.v2.option.DimensionOption", "api.v2.option.ShapeOptions");
    private static final String ABSTRACT_CLASS = "abstract class";
    private static final Pattern DECLARATION = Pattern.compile(
            "^public\\s+((?:(?:final|abstract|sealed|non-sealed)\\s+)*)(class|interface|record|enum)\\s",
            Pattern.MULTILINE);

    private static final Set<String> ENGINE_READERS =
            Set.of("engine", "mixin", "accessors", "compat", "platform", "client.engine", BRIDGE_GROUP);
    private static final String MIGRATION_GROUP = "migration";
    private static final Set<String> MIGRATION_READERS = Set.of(MIGRATION_GROUP, "engine", MIXIN_SEGMENT);
    private static final Set<String> LOADER_ENTRIES =
            Set.of("WorldLoop", "WorldLoopNetwork", "ExoticWorldsNeoForge", "ExoticWorldsFabric",
                    "ExoticWorldsFabricClient", "VanillaBootstrapListener");
    private static final Set<String> EXCEPTIONS = Set.of(
            "shape.climate.ClimateCompression -> engine.noise.ClimateScaleCompression",
            "shape.torus.CoastFieldLift -> engine.noise.GenerationFoldContext",
            "api.v1.shape.ShapeDimensions -> engine.gen.ShapedDimensions",
            "api.v1.ExoticWorldsApi -> engine.seam.circumnavigation.SeamTravel",
            "api.v1.ExoticWorldsClientApi -> engine.level.PublishedShapes",
            "shape.WorldShapes -> engine.gen.ShapedDimensions",
            "shape.layout.LayoutDimensions -> engine.gen.ShapedDimensions",
            "shape.layout.OverworldDerivation -> engine.gen.ShapedDimensions");

    @Test
    void everyImportStaysInsideItsGroup() throws IOException {
        List<Path> sources = sourceFiles();
        Map<String, String> kinds = kindsOf(sources);
        List<String> violations = new ArrayList<>();
        for (Path source : sources) {
            String type = typeOf(source);
            for (String imported : ownImports(source)) {
                String edge = type + " -> " + imported;
                String broken = brokenRule(type, imported, kinds);
                if (broken != null && !EXCEPTIONS.contains(edge)) {
                    violations.add(edge + " (" + broken + ")");
                }
            }
        }

        assertTrue(violations.isEmpty(), "imports crossing a group boundary: " + violations.stream().sorted().toList());
    }

    private static String brokenRule(String reader, String imported, Map<String, String> kinds) {
        String readerGroup = groupOf(packageOf(reader));
        String importedGroup = groupOf(packageOf(imported));
        String readerVersion = versionOf(reader);
        String importedVersion = versionOf(imported);
        if (readerVersion != null && importedVersion != null && !readerVersion.equals(importedVersion)) {
            return "versions never read each other: an api and its bridge stand alone";
        }
        if (packageOf(reader).startsWith(API_V2) && importedVersion == null) {
            return "api.v2 holds declarations alone: it reads itself and its bridge";
        }
        if (readerVersion == null && importedGroup.equals(BRIDGE_GROUP)) {
            return "the mod never reads a bridge: only its api calls it";
        }
        if (readerVersion == null && importedVersion != null && !isModelRead(reader, imported, kinds)) {
            return "the mod reads the api only as v2 models in shape and client/shape";
        }
        if (importedGroup.equals("engine")
                && !ENGINE_READERS.contains(readerGroup)
                && !LOADER_ENTRIES.contains(simpleNameOf(reader))) {
            return "engine is read by mixins, accessors, compat, platform, client/engine and the loader entries alone";
        }
        if (readerGroup.equals("engine") && (importedGroup.equals("shape") || importedGroup.equals("client"))) {
            return "engine never reads a shape or the client";
        }
        if (readerGroup.equals("core") && !importedGroup.equals("core") && !isVocabulary(importedGroup)) {
            return "core is the vocabulary: it reads no work";
        }
        if (importedGroup.equals(MIGRATION_GROUP) && !MIGRATION_READERS.contains(readerGroup)) {
            return "migration is read by the engine and the mixins alone, so deleting it touches nothing else";
        }
        if (readerGroup.equals(MIGRATION_GROUP) && !importedGroup.equals(MIGRATION_GROUP)
                && !importedGroup.equals(ROOT_GROUP)) {
            return "migration reads only the package root";
        }
        return null;
    }

    private static boolean isVocabulary(String group) {
        return group.equals("accessors") || group.equals("api") || group.equals("platform") || group.equals(ROOT_GROUP);
    }

    private static String versionOf(String type) {
        String packageName = packageOf(type);
        for (String root : VERSIONED_ROOTS) {
            for (String version : VERSIONS) {
                String versioned = root + "." + version;
                if (packageName.equals(versioned) || packageName.startsWith(versioned + ".")) {
                    return version;
                }
            }
        }
        return null;
    }

    private static boolean isModelRead(String reader, String imported, Map<String, String> kinds) {
        String readerPackage = packageOf(reader);
        boolean readsModels = MODEL_READERS.stream()
                .anyMatch(group -> readerPackage.equals(group) || readerPackage.startsWith(group + "."));
        boolean isModel = MODEL_KINDS.contains(kinds.getOrDefault(imported, ""))
                || MODEL_CLASSES.contains(imported);
        return readsModels && packageOf(imported).startsWith(API_V2) && isModel;
    }

    private static Map<String, String> kindsOf(List<Path> sources) throws IOException {
        Map<String, String> kinds = new HashMap<>();
        for (Path source : sources) {
            Matcher declaration = DECLARATION.matcher(Files.readString(source, StandardCharsets.UTF_8));
            if (declaration.find()) {
                boolean abstractClass = declaration.group(1).contains("abstract") && declaration.group(2).equals("class");
                kinds.put(typeOf(source), abstractClass ? ABSTRACT_CLASS : declaration.group(2));
            }
        }
        return kinds;
    }

    private static String groupOf(String packageName) {
        List<String> segments = List.of(packageName.split("\\."));
        if (segments.contains(MIXIN_SEGMENT)) {
            return MIXIN_SEGMENT;
        }
        if (packageName.startsWith("client.engine")) {
            return "client.engine";
        }
        return segments.isEmpty() ? ROOT_GROUP : segments.get(0);
    }

    private static String packageOf(String type) {
        int lastDot = type.lastIndexOf('.');
        return lastDot < 0 ? ROOT_GROUP : type.substring(0, lastDot);
    }

    private static String simpleNameOf(String type) {
        return type.substring(type.lastIndexOf('.') + 1);
    }

    private static List<String> ownImports(Path source) throws IOException {
        List<String> imported = new ArrayList<>();
        for (String line : Files.readAllLines(source, StandardCharsets.UTF_8)) {
            String statement = line.strip();
            if (!statement.startsWith(IMPORT_KEYWORD)) {
                continue;
            }

            statement = statement.substring(IMPORT_KEYWORD.length()).strip();
            if (statement.startsWith(STATIC_KEYWORD)) {
                statement = statement.substring(STATIC_KEYWORD.length()).strip();
            }
            if (!statement.startsWith(PACKAGE_PREFIX)) {
                continue;
            }

            imported.add(topLevelType(statement.substring(PACKAGE_PREFIX.length()).replace(";", "").strip()));
        }
        return imported;
    }

    private static String topLevelType(String reference) {
        List<String> segments = List.of(reference.split("\\."));
        List<String> upTo = new ArrayList<>();
        for (String segment : segments) {
            upTo.add(segment);
            if (Character.isUpperCase(segment.charAt(0))) {
                break;
            }
        }
        return String.join(".", upTo);
    }

    private static String typeOf(Path source) {
        String path = source.toString().replace('\\', '/');
        String relative = path.substring(path.lastIndexOf(PACKAGE_PATH) + PACKAGE_PATH.length() + 1);
        return relative.substring(0, relative.length() - JAVA_SUFFIX.length()).replace('/', '.');
    }

    private static List<Path> sourceFiles() throws IOException {
        String repositoryRoot = System.getProperty(REPO_ROOT_PROPERTY);
        assertNotNull(repositoryRoot, "the " + REPO_ROOT_PROPERTY + " system property names no repository root");

        List<Path> sources = new ArrayList<>();
        for (String sourceRoot : SOURCE_ROOTS) {
            Path root = Path.of(repositoryRoot, sourceRoot, PACKAGE_PATH);
            assertTrue(Files.isDirectory(root), "missing source root " + root);
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(file -> file.getFileName().toString().endsWith(JAVA_SUFFIX)).forEach(sources::add);
            }
        }
        return sources;
    }
}
