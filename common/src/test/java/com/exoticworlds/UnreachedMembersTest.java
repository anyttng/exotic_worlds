package com.exoticworlds;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class UnreachedMembersTest {
    private static final String REPO_ROOT_PROPERTY = "toroidal.repoRoot";
    private static final List<String> MAIN_CLASS_ROOTS = List.of("common/build/classes/java/main",
            "frames/build/classes/java/main", "neoforge/build/classes/java/main", "fabric/build/classes/java/main");
    private static final List<String> TEST_CLASS_ROOTS = List.of("common/build/classes/java/test",
            "neoforge/build/classes/java/test", "fabric/build/classes/java/test");
    private static final List<String> SOURCE_ROOTS = List.of("common/src/main/java", "frames/src/main/java",
            "neoforge/src/main/java", "fabric/src/main/java");
    private static final String OWN_PREFIX = "com/exoticworlds/";
    private static final String API_PREFIX = OWN_PREFIX + "api/v1/";
    private static final String MIXIN_SEGMENT = "/mixin/";
    private static final String CLASS_SUFFIX = ".class";
    private static final String JAVA_SUFFIX = ".java";
    private static final Set<String> CLASS_FILES_WITHOUT_TYPE = Set.of("package-info.class", "module-info.class");
    private static final String CONSTRUCTOR = "<init>";
    private static final String STATIC_INITIALIZER = "<clinit>";
    private static final Set<String> ENUM_METHODS = Set.of("values", "valueOf");
    private static final String NO_ARGUMENTS = "()";
    private static final char NESTED_SEPARATOR = '$';
    private static final String LINE_MARKER = "// mc/";
    private static final String COMMENT_START = "//";
    private static final String ANNOTATION_START = "@";
    private static final Pattern TYPE_DECLARATION = Pattern.compile("\\b(?:class|record|interface|enum)\\s+(\\w+)");
    private static final Pattern MEMBER_NAME = Pattern.compile("(\\w+)\\s*\\(");

    private static final Set<String> READ_BY_TESTS = Set.of(
            "compat.distanthorizons.DhProbes.foldedKeyLines",
            "compat.distanthorizons.DhProbes.unchangedKeys",
            "compat.distanthorizons.DhProbes.resetKeyGates",
            "engine.fold.ChunkViewSweep.positions",
            "compat.wover.LapAxis.latticeBlocks",
            "shape.WorldLoopPresets.blockWidth",
            "engine.net.PacketTranslator.toClient",
            "engine.net.PacketTranslator.toServer",
            "accessors.FoldedClimateSampler.exotic_worlds$fold");
    private static final Set<String> READ_BY_HARNESS = Set.of(
            "compat.distanthorizons.DhFold.periodSections",
            "compat.ModPresenceGatePlugin.gates",
            "core.TranslationLattice.nearestZLaps");
    private static final Set<String> CALLED_BY_EMITTED_CODE = Set.of(
            "engine.noise.ContextScaledNoise.sampleWrapped",
            "engine.noise.ContextScaledNoise.sampleWrappedWarped");

    @Test
    void noMainMemberIsCalledByTestsAlone() throws IOException {
        Path root = repositoryRoot();
        Map<String, ClassNode> mainClasses = readClasses(root, MAIN_CLASS_ROOTS);
        Map<String, ClassNode> testClasses = readClasses(root, TEST_CLASS_ROOTS);
        Map<String, ClassNode> allClasses = new HashMap<>(testClasses);
        allClasses.putAll(mainClasses);
        Hierarchy hierarchy = new Hierarchy(allClasses, mainClasses);
        Markers markers = markedForLines(root);

        Set<MethodRef> reached = invoked(mainClasses.values(), hierarchy);
        List<String> unreached = new ArrayList<>();
        for (MethodRef called : invoked(testClasses.values(), hierarchy)) {
            if (!reached.contains(called) && !isExempt(called, mainClasses.get(called.owner()), hierarchy, markers)) {
                unreached.add(describe(called));
            }
        }

        assertTrue(unreached.isEmpty(), "main members only tests call: " + unreached.stream().sorted().toList());
    }

    private static boolean isExempt(MethodRef member, ClassNode owner, Hierarchy hierarchy, Markers markers) {
        MethodNode method = owner.methods.stream()
                .filter(declared -> declared.name.equals(member.name()) && declared.desc.equals(member.desc()))
                .findFirst()
                .orElseThrow();
        String listed = listedName(member);
        return !isChecked(owner, method)
                || member.owner().startsWith(API_PREFIX)
                || member.owner().contains(MIXIN_SEGMENT)
                || isRecordComponent(owner, method)
                || hierarchy.overridesOutsideContract(owner, method)
                || markers.covers(member, hierarchy.supertypes(member.owner()))
                || READ_BY_TESTS.contains(listed)
                || READ_BY_HARNESS.contains(listed)
                || CALLED_BY_EMITTED_CODE.contains(listed);
    }

    private static boolean isChecked(ClassNode owner, MethodNode method) {
        if ((method.access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) != 0
                || method.name.equals(STATIC_INITIALIZER)) {
            return false;
        }
        return (owner.access & Opcodes.ACC_ENUM) == 0 || !ENUM_METHODS.contains(method.name);
    }

    private static boolean isRecordComponent(ClassNode owner, MethodNode method) {
        return owner.recordComponents != null
                && method.desc.startsWith(NO_ARGUMENTS)
                && owner.recordComponents.stream().anyMatch(component -> component.name.equals(method.name));
    }

    private static Set<MethodRef> invoked(Collection<ClassNode> callers, Hierarchy hierarchy) {
        Set<MethodRef> invoked = new HashSet<>();
        for (ClassNode caller : callers) {
            for (MethodNode method : caller.methods) {
                MethodRef self = new MethodRef(caller.name, method.name, method.desc);
                for (AbstractInsnNode instruction : method.instructions) {
                    for (MethodRef target : targetsOf(instruction)) {
                        for (MethodRef declared : hierarchy.declarationsOf(target)) {
                            if (!declared.equals(self)) {
                                invoked.add(declared);
                            }
                        }
                    }
                }
            }
        }
        return invoked;
    }

    private static List<MethodRef> targetsOf(AbstractInsnNode instruction) {
        if (instruction instanceof MethodInsnNode call) {
            return List.of(new MethodRef(call.owner, call.name, call.desc));
        }
        if (instruction instanceof InvokeDynamicInsnNode dynamic) {
            return Arrays.stream(dynamic.bsmArgs)
                    .filter(Handle.class::isInstance)
                    .map(argument -> MethodRef.of((Handle) argument))
                    .toList();
        }
        if (instruction instanceof LdcInsnNode constant && constant.cst instanceof Handle handle) {
            return List.of(MethodRef.of(handle));
        }
        return List.of();
    }

    private static Markers markedForLines(Path root) throws IOException {
        Set<String> types = new HashSet<>();
        Set<String> members = new HashSet<>();
        for (String sourceRoot : SOURCE_ROOTS) {
            Path sources = root.resolve(sourceRoot);
            assertTrue(Files.isDirectory(sources), "missing source root " + sources);
            for (Path source : filesEndingIn(sources, JAVA_SUFFIX)) {
                String relative = sources.relativize(source).toString().replace('\\', '/');
                String fileType = relative.substring(0, relative.length() - JAVA_SUFFIX.length());
                List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).strip().startsWith(LINE_MARKER)) {
                        markDeclaration(declarationAfter(lines, i + 1), fileType, types, members);
                    }
                }
            }
        }
        return new Markers(types, members);
    }

    private static void markDeclaration(String declaration, String fileType, Set<String> types, Set<String> members) {
        int parenthesis = declaration.indexOf('(');
        Matcher type = TYPE_DECLARATION.matcher(declaration);
        if (type.find() && (parenthesis < 0 || type.start() < parenthesis)) {
            String name = type.group(1);
            types.add(fileType.endsWith("/" + name) ? fileType : fileType + NESTED_SEPARATOR + name);
            return;
        }
        Matcher member = MEMBER_NAME.matcher(declaration);
        if (member.find()) {
            members.add(fileType + "." + member.group(1));
        }
    }

    private static String declarationAfter(List<String> lines, int from) {
        for (int i = from; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (!line.isEmpty() && !line.startsWith(COMMENT_START) && !line.startsWith(ANNOTATION_START)) {
                return line;
            }
        }
        return "";
    }

    private static Map<String, ClassNode> readClasses(Path root, List<String> classRoots) throws IOException {
        Map<String, ClassNode> classes = new HashMap<>();
        for (String classRoot : classRoots) {
            Path directory = root.resolve(classRoot);
            assertTrue(Files.isDirectory(directory), "missing class root " + directory);
            for (Path file : filesEndingIn(directory, CLASS_SUFFIX)) {
                if (CLASS_FILES_WITHOUT_TYPE.contains(file.getFileName().toString())) {
                    continue;
                }
                ClassNode node = new ClassNode();
                new ClassReader(Files.readAllBytes(file))
                        .accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classes.put(node.name, node);
            }
        }
        return classes;
    }

    private static List<Path> filesEndingIn(Path directory, String suffix) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(suffix)).toList();
        }
    }

    private static Path repositoryRoot() {
        String repositoryRoot = System.getProperty(REPO_ROOT_PROPERTY);
        assertNotNull(repositoryRoot, "the " + REPO_ROOT_PROPERTY + " system property names no repository root");
        return Path.of(repositoryRoot);
    }

    private static String topLevelOf(String owner) {
        int nested = owner.indexOf(NESTED_SEPARATOR, owner.lastIndexOf('/') + 1);
        return nested < 0 ? owner : owner.substring(0, nested);
    }

    private static String listedName(MethodRef member) {
        return topLevelOf(member.owner()).substring(OWN_PREFIX.length()).replace('/', '.') + "." + member.name();
    }

    private static String describe(MethodRef member) {
        return member.owner().substring(OWN_PREFIX.length()).replace('/', '.') + "." + member.name() + member.desc();
    }

    private static List<String> parentsOf(ClassNode node) {
        List<String> parents = new ArrayList<>(node.interfaces);
        if (node.superName != null) {
            parents.add(node.superName);
        }
        return parents;
    }

    private record MethodRef(String owner, String name, String desc) {
        static MethodRef of(Handle handle) {
            return new MethodRef(handle.getOwner(), handle.getName(), handle.getDesc());
        }

        String signature() {
            return this.name + this.desc;
        }
    }

    private record Markers(Set<String> types, Set<String> members) {
        boolean covers(MethodRef member, Set<String> ownerAndSupertypes) {
            String owner = member.owner();
            return this.types.stream().anyMatch(type -> owner.equals(type) || owner.startsWith(type + NESTED_SEPARATOR))
                    || ownerAndSupertypes.stream()
                            .anyMatch(type -> this.members.contains(topLevelOf(type) + "." + member.name()));
        }
    }

    private static final class Hierarchy {
        private final Map<String, ClassNode> classes;
        private final Map<String, Set<String>> mainSignatures = new HashMap<>();
        private final Map<String, Set<String>> children = new HashMap<>();
        private final Map<String, Set<String>> related = new HashMap<>();
        private final Map<String, Optional<Set<String>>> foreignSignatures = new HashMap<>();

        Hierarchy(Map<String, ClassNode> classes, Map<String, ClassNode> mainClasses) {
            this.classes = classes;
            for (ClassNode node : mainClasses.values()) {
                Set<String> signatures = new HashSet<>();
                node.methods.forEach(method -> signatures.add(method.name + method.desc));
                this.mainSignatures.put(node.name, signatures);
            }
            for (ClassNode node : classes.values()) {
                for (String parent : parentsOf(node)) {
                    this.children.computeIfAbsent(parent, key -> new HashSet<>()).add(node.name);
                }
            }
        }

        List<MethodRef> declarationsOf(MethodRef target) {
            if (!target.owner().startsWith(OWN_PREFIX)) {
                return List.of();
            }
            Set<String> types = this.related.computeIfAbsent(target.owner(), this::relatedTypes);
            String signature = target.signature();
            return types.stream()
                    .filter(type -> this.mainSignatures.getOrDefault(type, Set.of()).contains(signature))
                    .map(type -> new MethodRef(type, target.name(), target.desc()))
                    .toList();
        }

        boolean overridesOutsideContract(ClassNode owner, MethodNode method) {
            if ((method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0 || method.name.equals(CONSTRUCTOR)) {
                return false;
            }
            String signature = method.name + method.desc;
            for (String type : supertypes(owner.name)) {
                if (type.startsWith(API_PREFIX)) {
                    if (this.mainSignatures.getOrDefault(type, Set.of()).contains(signature)) {
                        return true;
                    }
                } else if (!this.classes.containsKey(type)) {
                    Optional<Set<String>> signatures = foreignSignaturesOf(type);
                    if (signatures.isEmpty() || signatures.get().contains(signature)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private Set<String> relatedTypes(String type) {
            Set<String> types = supertypes(type);
            types.addAll(subtypes(type));
            return types;
        }

        Set<String> supertypes(String type) {
            Set<String> found = new HashSet<>();
            Deque<String> pending = new ArrayDeque<>(List.of(type));
            while (!pending.isEmpty()) {
                String next = pending.pop();
                ClassNode node = this.classes.get(next);
                if (found.add(next) && node != null) {
                    pending.addAll(parentsOf(node));
                }
            }
            return found;
        }

        private Set<String> subtypes(String type) {
            Set<String> found = new HashSet<>();
            Deque<String> pending = new ArrayDeque<>(List.of(type));
            while (!pending.isEmpty()) {
                String next = pending.pop();
                if (found.add(next)) {
                    pending.addAll(this.children.getOrDefault(next, Set.of()));
                }
            }
            return found;
        }

        private Optional<Set<String>> foreignSignaturesOf(String type) {
            Optional<Set<String>> known = this.foreignSignatures.get(type);
            if (known == null) {
                known = readForeignSignatures(type);
                this.foreignSignatures.put(type, known);
            }
            return known;
        }

        private Optional<Set<String>> readForeignSignatures(String type) {
            ClassNode node = new ClassNode();
            try (InputStream bytes = ClassLoader.getSystemResourceAsStream(type + CLASS_SUFFIX)) {
                if (bytes == null) {
                    return Optional.empty();
                }
                new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
            } catch (IOException unreadable) {
                return Optional.empty();
            }
            Set<String> signatures = new HashSet<>();
            node.methods.forEach(method -> signatures.add(method.name + method.desc));
            for (String parent : parentsOf(node)) {
                Optional<Set<String>> inherited = foreignSignaturesOf(parent);
                if (inherited.isEmpty()) {
                    return Optional.empty();
                }
                signatures.addAll(inherited.get());
            }
            return Optional.of(signatures);
        }
    }
}
