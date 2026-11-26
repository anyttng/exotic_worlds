package com.exoticworlds.engine.noise.check;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;

class MaterialCauseSearchTest {
    private static final String FOREIGN_NAMESPACE = "example_mod";

    private record Leaf(String name) implements MaterialRule {
        @Override
        public RuleEvaluator compile(MaterialRuleContext context) {
            throw new UnsupportedOperationException("A test leaf is never compiled");
        }

        @Override
        public MapCodec<? extends MaterialRule> codec() {
            throw new UnsupportedOperationException("A test leaf has no codec");
        }
    }

    private record Gate(String name) implements MaterialCondition {
        @Override
        public ConditionEvaluator compile(MaterialRuleContext context) {
            throw new UnsupportedOperationException("A test gate is never compiled");
        }

        @Override
        public MapCodec<? extends MaterialCondition> codec() {
            throw new UnsupportedOperationException("A test gate has no codec");
        }
    }

    private record Merged(MaterialRule base, Map<String, MaterialRule> sources, List<MaterialCondition> gates,
            String label) implements MaterialRule {
        @Override
        public RuleEvaluator compile(MaterialRuleContext context) {
            throw new UnsupportedOperationException("A test wrapper is never compiled");
        }

        @Override
        public MapCodec<? extends MaterialRule> codec() {
            throw new UnsupportedOperationException("A test wrapper has no codec");
        }
    }

    @Test
    void aForeignRecordNodeHandsOutTheNodesItsComponentsHold() {
        Leaf base = new Leaf("base");
        Leaf foreign = new Leaf("foreign");
        Gate gate = new Gate("gate");
        Merged merged = new Merged(base, Map.of(FOREIGN_NAMESPACE, foreign), List.of(gate), "label");

        assertEquals(List.of(base, foreign, gate), MaterialCauseSearch.childrenOf(merged));
    }

    @Test
    void aNodeWithoutNodeComponentsIsALeaf() {
        assertEquals(List.of(), MaterialCauseSearch.childrenOf(new Leaf("alone")));
    }

    @Test
    void theCauseIsTheRuleInsideTheForeignWrapperThatAnswersDifferently() {
        Leaf base = new Leaf("base");
        Leaf foreign = new Leaf("foreign");
        Merged merged = new Merged(base, Map.of(FOREIGN_NAMESPACE, foreign), List.of(), "label");
        IdentityHashMap<Object, Object> here = new IdentityHashMap<>();
        IdentityHashMap<Object, Object> there = new IdentityHashMap<>();
        here.put(merged, Blocks.NETHERRACK.defaultBlockState());
        there.put(merged, Blocks.BASALT.defaultBlockState());
        here.put(base, Blocks.NETHERRACK.defaultBlockState());
        there.put(base, Blocks.NETHERRACK.defaultBlockState());
        here.put(foreign, Blocks.NETHERRACK.defaultBlockState());
        there.put(foreign, Blocks.BASALT.defaultBlockState());

        SeamCause cause = MaterialCauseSearch.causeOf(merged, here, there);

        assertEquals(new SeamCause.Node(Leaf.class.getName(), null, null, false, List.of()), cause);
    }
}
