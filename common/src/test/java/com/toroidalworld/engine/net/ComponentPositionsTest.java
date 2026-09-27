package com.toroidalworld.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class ComponentPositionsTest {
    private static final int WIDTH_IN_CHUNKS = 32;
    private static final int WIDTH = WIDTH_IN_CHUNKS * 16;

    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(WIDTH_IN_CHUNKS)));

    private static final Vec3 ANCHOR = new Vec3(-250.0, 64.0, 0.0);
    private static final BlockPos STORED = new BlockPos(240, 64, 10);
    private static final BlockPos SEATED = STORED.offset(-WIDTH, 0, 0);
    private static final BlockPos DETACHED = new BlockPos(200, 64, 0);

    private static final Identifier LODESTONE_ID = Identifier.withDefaultNamespace("lodestone_tracker");
    private static final Identifier ANCHOR_ID = Identifier.fromNamespaceAndPath("cject", "anchor");
    private static final Identifier NODE_ID = Identifier.fromNamespaceAndPath("cject", "node");
    private static final Identifier MISSING_ID = Identifier.fromNamespaceAndPath("cject", "missing");

    private static final DataComponentType<BlockPos> ANCHOR_TYPE =
            DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).build();
    private static final DataComponentType<BlockPos> NODE_TYPE =
            DataComponentType.<BlockPos>builder().persistent(BlockPos.CODEC).build();

    private static final Map<Identifier, DataComponentType<?>> TYPES = Map.of(
            LODESTONE_ID, DataComponents.LODESTONE_TRACKER, ANCHOR_ID, ANCHOR_TYPE, NODE_ID, NODE_TYPE);

    private static final ComponentPositions.Mover KEEP_DETACHED =
            (stored, seat) -> DETACHED.equals(stored) ? stored : seat.apply(stored);

    private static final UnaryOperator<Object> TOWARD_ANCHOR =
            value -> FoldedValue.toward(TORUS, Level.OVERWORLD, ANCHOR, value);

    private static Map<DataComponentType<?>, ComponentPositions.Mover> resolve(
            Map<Identifier, ComponentPositions.Mover> javaRows, Set<Identifier> declared) {
        return ComponentPositions.resolve(javaRows, declared, TYPES::get);
    }

    private static Map<DataComponentType<?>, ComponentPositions.Mover> declared(Identifier... ids) {
        return resolve(ComponentPositions.javaRows(), Set.of(ids));
    }

    private static ItemStack compassTracking(GlobalPos target) {
        ItemStack compass = new ItemStack(Holder.direct(Items.COMPASS));
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(target), true));
        return compass;
    }

    @Test
    void theLodestoneTrackerIsNamedWithNoDataFile() {
        assertEquals(Set.of(DataComponents.LODESTONE_TRACKER), declared().keySet());
    }

    @Test
    void aDeclaredComponentJoinsTheJavaRows() {
        assertEquals(Set.of(DataComponents.LODESTONE_TRACKER, ANCHOR_TYPE), declared(ANCHOR_ID).keySet());
    }

    @Test
    void anIdNoComponentTypeAnswersIsSkipped() {
        assertEquals(Set.of(DataComponents.LODESTONE_TRACKER, ANCHOR_TYPE), declared(ANCHOR_ID, MISSING_ID).keySet());
    }

    @Test
    void aDeclarationAtARegisteredIdKeepsTheRegistrationsMover() {
        Map<DataComponentType<?>, ComponentPositions.Mover> rows =
                resolve(Map.of(NODE_ID, KEEP_DETACHED), Set.of(NODE_ID));

        assertSame(KEEP_DETACHED, rows.get(NODE_TYPE));
    }

    @Test
    void aNamedPlainPositionIsSeatedOnACopyAndTheStoredStackKeepsItsValue() {
        ItemStack stack = new ItemStack(Holder.direct(Items.STICK));
        stack.set(ANCHOR_TYPE, STORED);

        ItemStack seated = ComponentPositions.seatedIn(declared(ANCHOR_ID), stack, TOWARD_ANCHOR);

        assertNotSame(stack, seated);
        assertEquals(SEATED, seated.get(ANCHOR_TYPE));
        assertEquals(STORED, stack.get(ANCHOR_TYPE));
    }

    @Test
    void anUnnamedPositionIsReadAsStored() {
        ItemStack stack = new ItemStack(Holder.direct(Items.STICK));
        stack.set(ANCHOR_TYPE, STORED);

        assertSame(stack, ComponentPositions.seatedIn(declared(), stack, TOWARD_ANCHOR));
    }

    @Test
    void aPositionAlreadyNearTheAnchorIsTheStackItself() {
        ItemStack stack = new ItemStack(Holder.direct(Items.STICK));
        stack.set(ANCHOR_TYPE, SEATED);

        assertSame(stack, ComponentPositions.seatedIn(declared(ANCHOR_ID), stack, TOWARD_ANCHOR));
    }

    @Test
    void aLodestoneTargetInTheAnchorsDimensionIsSeatedThroughTheRecord() {
        ItemStack seated = ComponentPositions.seatedIn(declared(),
                compassTracking(GlobalPos.of(Level.OVERWORLD, STORED)), TOWARD_ANCHOR);

        assertEquals(Optional.of(GlobalPos.of(Level.OVERWORLD, SEATED)),
                seated.get(DataComponents.LODESTONE_TRACKER).target());
    }

    @Test
    void aLodestoneTargetInAnotherDimensionIsLeftAlone() {
        ItemStack compass = compassTracking(GlobalPos.of(Level.NETHER, STORED));

        assertSame(compass, ComponentPositions.seatedIn(declared(), compass, TOWARD_ANCHOR));
    }

    @Test
    void aRegistrationsMoverDecidesWhatMoves() {
        Map<DataComponentType<?>, ComponentPositions.Mover> rows = resolve(Map.of(NODE_ID, KEEP_DETACHED), Set.of());
        ItemStack detached = new ItemStack(Holder.direct(Items.STICK));
        detached.set(NODE_TYPE, DETACHED);
        ItemStack attached = new ItemStack(Holder.direct(Items.STICK));
        attached.set(NODE_TYPE, STORED);

        assertSame(detached, ComponentPositions.seatedIn(rows, detached, TOWARD_ANCHOR));
        assertEquals(SEATED, ComponentPositions.seatedIn(rows, attached, TOWARD_ANCHOR).get(NODE_TYPE));
    }
}
