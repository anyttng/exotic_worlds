package com.exoticworlds.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

final class MixinAnnotations {
    private static final int NAME_VALUE_PAIR = 2;

    private MixinAnnotations() {
    }

    static List<AnnotationNode> of(ClassNode element) {
        return joined(element.visibleAnnotations, element.invisibleAnnotations);
    }

    static List<AnnotationNode> of(FieldNode element) {
        return joined(element.visibleAnnotations, element.invisibleAnnotations);
    }

    static List<AnnotationNode> of(MethodNode element) {
        return joined(element.visibleAnnotations, element.invisibleAnnotations);
    }

    static Optional<AnnotationNode> find(List<AnnotationNode> annotations, String descriptor) {
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(descriptor)) {
                return Optional.of(annotation);
            }
        }

        return Optional.empty();
    }

    static Optional<Object> element(AnnotationNode annotation, String name) {
        if (annotation.values == null) {
            return Optional.empty();
        }

        for (int index = 0; index + 1 < annotation.values.size(); index += NAME_VALUE_PAIR) {
            if (name.equals(annotation.values.get(index))) {
                return Optional.of(annotation.values.get(index + 1));
            }
        }

        return Optional.empty();
    }

    static List<?> values(AnnotationNode annotation, String name) {
        return element(annotation, name)
                .<List<?>>map(value -> value instanceof List<?> array ? array : List.of(value))
                .orElse(List.of());
    }

    static List<String> strings(AnnotationNode annotation, String name) {
        List<String> strings = new ArrayList<>();
        for (Object value : values(annotation, name)) {
            strings.add(asString(value));
        }

        return strings;
    }

    static List<AnnotationNode> nested(AnnotationNode annotation, String name) {
        List<AnnotationNode> nested = new ArrayList<>();
        for (Object value : values(annotation, name)) {
            if (value instanceof AnnotationNode inner) {
                nested.add(inner);
            }
        }

        return nested;
    }

    static String string(AnnotationNode annotation, String name) {
        return element(annotation, name).map(MixinAnnotations::asString).orElse("");
    }

    static int integer(AnnotationNode annotation, String name, int fallback) {
        return element(annotation, name)
                .map(value -> value instanceof Integer number ? number : fallback)
                .orElse(fallback);
    }

    static String asString(Object value) {
        return value instanceof String string ? string : "";
    }

    private static List<AnnotationNode> joined(@Nullable List<AnnotationNode> visible,
            @Nullable List<AnnotationNode> invisible) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (visible != null) {
            annotations.addAll(visible);
        }
        if (invisible != null) {
            annotations.addAll(invisible);
        }
        return annotations;
    }
}
