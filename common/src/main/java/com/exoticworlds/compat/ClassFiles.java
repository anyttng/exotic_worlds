package com.exoticworlds.compat;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

final class ClassFiles {
    record Loaded(boolean present, @Nullable ClassNode model) {
    }

    private static final String CLASS_SUFFIX = ".class";

    private final ClassLoader classLoader;
    private final Map<String, Loaded> loaded = new HashMap<>();

    ClassFiles(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    ClassLoader classLoader() {
        return this.classLoader;
    }

    Loaded load(String internalName) {
        return this.loaded.computeIfAbsent(internalName, this::read);
    }

    private Loaded read(String internalName) {
        try (InputStream classFile = this.classLoader.getResourceAsStream(internalName + CLASS_SUFFIX)) {
            if (classFile == null) {
                return new Loaded(false, null);
            }

            ClassNode model = new ClassNode();
            new ClassReader(classFile.readAllBytes()).accept(model, ClassReader.SKIP_FRAMES);
            return new Loaded(true, model);
        } catch (IOException | RuntimeException unreadable) {
            return new Loaded(true, null);
        }
    }
}
