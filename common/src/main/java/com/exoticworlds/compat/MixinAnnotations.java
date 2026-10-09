package com.exoticworlds.compat;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class MixinAnnotations {
    private MixinAnnotations() {
    }

    static List<Annotation> of(AttributedElement element) {
        List<Annotation> annotations = new ArrayList<>();
        element.findAttribute(Attributes.runtimeVisibleAnnotations())
                .ifPresent(attribute -> annotations.addAll(attribute.annotations()));
        element.findAttribute(Attributes.runtimeInvisibleAnnotations())
                .ifPresent(attribute -> annotations.addAll(attribute.annotations()));
        return annotations;
    }

    static Optional<Annotation> find(List<Annotation> annotations, String descriptor) {
        for (Annotation annotation : annotations) {
            if (annotation.className().equalsString(descriptor)) {
                return Optional.of(annotation);
            }
        }

        return Optional.empty();
    }

    static Optional<AnnotationValue> element(Annotation annotation, String name) {
        for (AnnotationElement element : annotation.elements()) {
            if (element.name().equalsString(name)) {
                return Optional.of(element.value());
            }
        }

        return Optional.empty();
    }

    static List<AnnotationValue> values(Annotation annotation, String name) {
        return element(annotation, name)
                .map(value -> value instanceof AnnotationValue.OfArray array ? array.values() : List.of(value))
                .orElse(List.of());
    }

    static List<String> strings(Annotation annotation, String name) {
        List<String> strings = new ArrayList<>();
        for (AnnotationValue value : values(annotation, name)) {
            strings.add(asString(value));
        }

        return strings;
    }

    static List<Annotation> nested(Annotation annotation, String name) {
        List<Annotation> nested = new ArrayList<>();
        for (AnnotationValue value : values(annotation, name)) {
            if (value instanceof AnnotationValue.OfAnnotation inner) {
                nested.add(inner.annotation());
            }
        }

        return nested;
    }

    static String string(Annotation annotation, String name) {
        return element(annotation, name).map(MixinAnnotations::asString).orElse("");
    }

    static int integer(Annotation annotation, String name, int fallback) {
        return element(annotation, name)
                .map(value -> value instanceof AnnotationValue.OfInt number ? number.intValue() : fallback)
                .orElse(fallback);
    }

    static String asString(AnnotationValue value) {
        return value instanceof AnnotationValue.OfString string ? string.stringValue() : "";
    }
}
