package com.exoticworlds.compat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

final class ClassFiles {
    record Loaded(boolean present, @Nullable ClassModel model) {
    }

    private static final String CLASS_SUFFIX = ".class";

    private final ClassLoader classLoader;
    private final Map<String, Loaded> loaded = new HashMap<>();

    ClassFiles(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    Loaded load(String internalName) {
        return this.loaded.computeIfAbsent(internalName, this::read);
    }

    private Loaded read(String internalName) {
        try (InputStream classFile = this.classLoader.getResourceAsStream(internalName + CLASS_SUFFIX)) {
            if (classFile == null) {
                return new Loaded(false, null);
            }

            return new Loaded(true, ClassFile.of().parse(classFile.readAllBytes()));
        } catch (IOException | RuntimeException unreadable) {
            return new Loaded(true, null);
        }
    }
}
