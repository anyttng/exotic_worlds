package com.exoticworlds.engine.noise;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.ExoticWorlds;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.MaterialSystem;
import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.condition.NotCondition;
import net.minecraft.world.level.levelgen.material.rule.ConditionRule;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;
import net.minecraft.world.level.levelgen.material.rule.SequenceRule;

final class MaterialCauseSearch {
    private static final String VANILLA_PACKAGE = "net.minecraft.";
    private static final Object NO_STATE = new Object();

    private static final List<String> MATERIAL_CLASSES = Stream.of(MaterialSystem.class, MaterialRuleContext.class)
            .map(Class::getName)
            .toList();

    private record Found(Object node, @Nullable Identifier holder) {
    }

    static final class Recorder implements MaterialRule {
        private final MaterialRule root;
        private final SeamCauseSearch.Point target;
        private final IdentityHashMap<Object, Object> answers = new IdentityHashMap<>();

        Recorder(MaterialRule root, SeamCauseSearch.Point target) {
            this.root = root;
            this.target = target;
        }

        @Override
        public RuleEvaluator compile(MaterialRuleContext context) {
            RuleEvaluator rootRule = this.root.compile(context);
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
        public MapCodec<? extends MaterialRule> codec() {
            throw new UnsupportedOperationException("A recorder is never serialized");
        }

        IdentityHashMap<Object, Object> answers() {
            return this.answers;
        }
    }

    static SeamCause causeOf(MaterialRule root, IdentityHashMap<Object, Object> here,
            IdentityHashMap<Object, Object> there) {
        if (here.isEmpty() || there.isEmpty()) {
            return new SeamCause.NotFound();
        }

        Found found = deepest(root, null, here, there);
        return found != null ? nodeCause(found) : new SeamCause.NotFound();
    }

    private static void compileTree(Object node, MaterialRuleContext context,
            IdentityHashMap<Object, Object> evaluators) {
        if (evaluators.containsKey(node)) {
            return;
        }

        evaluators.put(node, node instanceof MaterialRule rule
                ? rule.compile(context)
                : ((MaterialCondition) node).compile(context));
        for (Object child : childrenOf(node)) {
            compileTree(child, context, evaluators);
        }
    }

    private static Object answerOf(Object evaluator, int blockX, int blockY, int blockZ) {
        if (evaluator instanceof ConditionEvaluator condition) {
            return condition.test();
        }

        Object state = ((RuleEvaluator) evaluator).tryApply(blockX, blockY, blockZ);
        return state != null ? state : NO_STATE;
    }

    private static @Nullable Found deepest(Object node, @Nullable Identifier holder,
            IdentityHashMap<Object, Object> here, IdentityHashMap<Object, Object> there) {
        if (Objects.equals(here.get(node), there.get(node))) {
            return null;
        }

        Identifier enclosing = holderKeyOf(node, holder);
        for (Object child : childrenOf(node)) {
            Found found = deepest(child, enclosing, here, there);
            if (found != null) {
                return found;
            }
        }

        return new Found(node, holder);
    }

    private static List<Object> childrenOf(Object node) {
        return switch (node) {
            case SequenceRule sequence -> new ArrayList<>(sequence.sequence());
            case ConditionRule condition -> List.of(condition.ifTrue(), condition.thenRun());
            case NotCondition not -> List.of(not.target());
            case MaterialRule.HolderHolder holder -> List.of(holder.holder().value());
            case MaterialCondition.HolderHolder holder -> List.of(holder.holder().value());
            default -> List.of();
        };
    }

    private static @Nullable Identifier holderKeyOf(Object node, @Nullable Identifier enclosing) {
        Holder<?> holder = switch (node) {
            case MaterialRule.HolderHolder rule -> rule.holder();
            case MaterialCondition.HolderHolder condition -> condition.holder();
            default -> null;
        };
        return holder != null ? holder.unwrapKey().map(ResourceKey::identifier).orElse(enclosing) : enclosing;
    }

    private static SeamCause nodeCause(Found found) {
        Object node = found.node();
        String holder = found.holder() != null ? found.holder().toString() : null;
        Identifier type = typeKeyOf(node);
        String className = node.getClass().getName();
        boolean vanilla = type != null
                ? type.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
                : className.startsWith(VANILLA_PACKAGE);
        List<String> candidates = vanilla
                ? AppliedMixins.foreignOn(Stream.concat(Stream.of(className), MATERIAL_CLASSES.stream()).toList())
                : List.of();
        return new SeamCause.Node(type != null ? type.toString() : className, holder,
                type != null ? type.getNamespace() : null, vanilla, candidates);
    }

    private static @Nullable Identifier typeKeyOf(Object node) {
        try {
            return node instanceof MaterialRule rule
                    ? BuiltInRegistries.MATERIAL_RULE_TYPE.getKey(rule.codec())
                    : BuiltInRegistries.MATERIAL_CONDITION_TYPE.getKey(((MaterialCondition) node).codec());
        } catch (UnsupportedOperationException noCodec) {
            ExoticWorlds.LOGGER.debug("[ew/edge] material_node_without_type class={}", node.getClass().getName());
            return null;
        }
    }

    private MaterialCauseSearch() {
    }
}
