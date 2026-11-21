package com.exoticworlds.engine.gen.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.SeamTransform;

import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

class MovedStructureStartTest {
    private static final int WIDTH = 64 * 16;

    @Test
    void aJunctionMovesItsSourceAndKeepsItsHeights() {
        JigsawJunction junction = new JigsawJunction(500, 70, -20, 3, StructureTemplatePool.Projection.RIGID);
        DeckTransformation lap = new DeckTransformation(SeamTransform.translation(-WIDTH, WIDTH));

        JigsawJunction moved = MovedStructureStart.movedJunction(junction, lap);

        assertEquals(500 - WIDTH, moved.getSourceX());
        assertEquals(70, moved.getSourceGroundY());
        assertEquals(-20 + WIDTH, moved.getSourceZ());
        assertEquals(3, moved.getDeltaY());
        assertEquals(StructureTemplatePool.Projection.RIGID, moved.getDestProjection());
    }

    @Test
    void aClockwiseTurnCarriesAJunctionToItsTurnedCell() {
        JigsawJunction junction = new JigsawJunction(500, 70, -20, 3, StructureTemplatePool.Projection.RIGID);
        DeckTransformation turn = new DeckTransformation(new SeamTransform(-1, 1, WIDTH, -WIDTH, true));

        JigsawJunction moved = MovedStructureStart.movedJunction(junction, turn);

        assertEquals(new JigsawJunction(20 + WIDTH - 1, 70, 500 - WIDTH, 3, StructureTemplatePool.Projection.RIGID),
                moved);
    }

    @Test
    void aCounterclockwiseTurnCarriesAJunctionToItsTurnedCell() {
        JigsawJunction junction = new JigsawJunction(500, 70, -20, 3, StructureTemplatePool.Projection.RIGID);
        DeckTransformation turn = new DeckTransformation(new SeamTransform(1, -1, WIDTH, -WIDTH, true));

        JigsawJunction moved = MovedStructureStart.movedJunction(junction, turn);

        assertEquals(new JigsawJunction(-20 + WIDTH, 70, -500 - WIDTH - 1, 3, StructureTemplatePool.Projection.RIGID),
                moved);
    }

    @Test
    void theIdentityLeavesAJunctionWhereItWas() {
        JigsawJunction junction = new JigsawJunction(500, 70, -20, 3, StructureTemplatePool.Projection.TERRAIN_MATCHING);

        JigsawJunction moved = MovedStructureStart.movedJunction(junction, DeckTransformation.IDENTITY);

        assertEquals(junction, moved);
    }
}
