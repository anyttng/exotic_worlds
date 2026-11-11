package com.exoticworlds.engine.noise;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.platform.Platforms;

public final class AppliedMixins {
    private static final String MIXIN_MERGED = "org.spongepowered.asm.mixin.transformer.meta.MixinMerged";
    private static final String MIXIN_MEMBER = "mixin";
    private static final String CLASS_SUFFIX = ".class";

    public record Applied(String mixin, @Nullable String modId) {
        String owner() {
            return this.modId != null ? this.modId : this.mixin;
        }
    }

    public static List<Applied> on(String className) {
        Class<?> target;
        try {
            target = Class.forName(className, false, AppliedMixins.class.getClassLoader());
        } catch (ClassNotFoundException absent) {
            return List.of();
        }

        return Arrays.stream(target.getDeclaredMethods())
                .flatMap(AppliedMixins::mergedFrom)
                .distinct()
                .map(mixin -> new Applied(mixin, Platforms.get().modIdOfResource(resourceOf(mixin)).orElse(null)))
                .sorted(Comparator.comparing(Applied::mixin))
                .toList();
    }

    static List<String> foreignOn(Collection<String> classNames) {
        return foreign(classNames.stream().flatMap(className -> on(className).stream()).toList());
    }

    static List<String> foreign(Collection<Applied> applied) {
        return applied.stream()
                .filter(mixin -> !Objects.equals(mixin.modId(), ExoticWorlds.MODID))
                .map(Applied::owner)
                .distinct()
                .sorted()
                .toList();
    }

    private static Stream<String> mergedFrom(Method method) {
        return Arrays.stream(method.getDeclaredAnnotations())
                .filter(annotation -> annotation.annotationType().getName().equals(MIXIN_MERGED))
                .map(AppliedMixins::mixinOf);
    }

    private static String mixinOf(Annotation merged) {
        try {
            return (String) merged.annotationType().getMethod(MIXIN_MEMBER).invoke(merged);
        } catch (ReflectiveOperationException unreachable) {
            throw new IllegalStateException("MixinMerged carries no " + MIXIN_MEMBER + ": " + unreachable,
                    unreachable);
        }
    }

    private static String resourceOf(String className) {
        return className.replace('.', '/') + CLASS_SUFFIX;
    }

    private AppliedMixins() {
    }
}
