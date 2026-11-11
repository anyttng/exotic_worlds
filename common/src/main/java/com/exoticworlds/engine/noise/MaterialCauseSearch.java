package com.exoticworlds.engine.noise;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;

final class MaterialCauseSearch {
    private static final String VANILLA_PACKAGE = "net.minecraft.";
    private static final Object NO_STATE = new Object();

    private static final List<String> MATERIAL_CLASSES = Stream.of(SurfaceSystem.class, SurfaceRules.Context.class)
            .map(Class::getName)
            .toList();

    static final class Recorder implements SurfaceRules.RuleSource {
        private final SurfaceRules.RuleSource root;
        private final SeamCauseSearch.Point target;
        private final IdentityHashMap<Object, Object> answers = new IdentityHashMap<>();

        Recorder(SurfaceRules.RuleSource root, SeamCauseSearch.Point target) {
            this.root = root;
            this.target = target;
        }

        @Override
        public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
            SurfaceRules.SurfaceRule rootRule = this.root.apply(context);
            IdentityHashMap<Object, Object> evaluators = new IdentityHashMap<>();
            compileTree(this.root, context, evaluators);
            return (blockX, blockY, blockZ) -> {
                if (blockX == this.target.x() && blockY == this.target.y() && blockZ == this.target.z()) {
                    evaluators.forEach((node, evaluator) -> this.answers.put(node, answerOf(evaluator, blockX,
                            blockY, blockZ)));
                }

                return rootRule.tryApply(blockX, blockY, blockZ);
            };
        }

        @Override
        public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
            throw new UnsupportedOperationException("A recorder is never serialized");
        }

        IdentityHashMap<Object, Object> answers() {
            return this.answers;
        }
    }

    static SeamCause causeOf(SurfaceRules.RuleSource root, IdentityHashMap<Object, Object> here,
            IdentityHashMap<Object, Object> there) {
        if (here.isEmpty() || there.isEmpty()) {
            return new SeamCause.NotFound();
        }

        Object found = deepest(root, here, there);
        return found != null ? nodeCause(found) : new SeamCause.NotFound();
    }

    private static void compileTree(Object node, SurfaceRules.Context context,
            IdentityHashMap<Object, Object> evaluators) {
        if (evaluators.containsKey(node)) {
            return;
        }

        evaluators.put(node, node instanceof SurfaceRules.RuleSource rule
                ? rule.apply(context)
                : ((SurfaceRules.ConditionSource) node).apply(context));
        for (Object child : childrenOf(node)) {
            compileTree(child, context, evaluators);
        }
    }

    private static Object answerOf(Object evaluator, int blockX, int blockY, int blockZ) {
        if (evaluator instanceof SurfaceRules.Condition condition) {
            return condition.test();
        }

        Object state = ((SurfaceRules.SurfaceRule) evaluator).tryApply(blockX, blockY, blockZ);
        return state != null ? state : NO_STATE;
    }

    private static @Nullable Object deepest(Object node, IdentityHashMap<Object, Object> here,
            IdentityHashMap<Object, Object> there) {
        if (Objects.equals(here.get(node), there.get(node))) {
            return null;
        }

        for (Object child : childrenOf(node)) {
            Object found = deepest(child, here, there);
            if (found != null) {
                return found;
            }
        }

        return node;
    }

    private static List<Object> childrenOf(Object node) {
        return switch (node) {
            case SurfaceRules.SequenceRuleSource sequence -> new ArrayList<>(sequence.sequence());
            case SurfaceRules.TestRuleSource test -> List.of(test.ifTrue(), test.thenRun());
            case SurfaceRules.NotConditionSource not -> List.of(not.target());
            default -> List.of();
        };
    }

    private static SeamCause nodeCause(Object node) {
        ResourceLocation type = typeKeyOf(node);
        String className = node.getClass().getName();
        boolean vanilla = type != null
                ? type.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
                : className.startsWith(VANILLA_PACKAGE);
        List<String> candidates = vanilla
                ? AppliedMixins.foreignOn(Stream.concat(Stream.of(className), MATERIAL_CLASSES.stream()).toList())
                : List.of();
        return new SeamCause.Node(type != null ? type.toString() : className, null,
                type != null ? type.getNamespace() : null, vanilla, candidates);
    }

    private static @Nullable ResourceLocation typeKeyOf(Object node) {
        try {
            return node instanceof SurfaceRules.RuleSource rule
                    ? BuiltInRegistries.MATERIAL_RULE.getKey(rule.codec().codec())
                    : BuiltInRegistries.MATERIAL_CONDITION.getKey(((SurfaceRules.ConditionSource) node).codec().codec());
        } catch (UnsupportedOperationException noCodec) {
            ExoticWorlds.LOGGER.debug("[ew/edge] material_node_without_type class={}", node.getClass().getName());
            return null;
        }
    }

    private MaterialCauseSearch() {
    }
}
