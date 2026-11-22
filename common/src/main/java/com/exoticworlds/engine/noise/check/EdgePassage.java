package com.exoticworlds.engine.noise.check;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidEdge;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

record EdgePassage(WorldFold fold, CuboidAtlas atlas, CuboidEdge edge, CuboidFace first,
        List<SeamCauseSearch.Point> samples) implements SeamPassage {
    private static final int SAMPLES_PER_EDGE = 3;
    private static final int PAST_THE_EDGE = QuartPos.toBlock(1);
    private static final int SAMPLE_Y = 64;

    static List<EdgePassage> of(WorldFold fold, CuboidAtlas atlas) {
        List<EdgePassage> passages = new ArrayList<>();
        for (CuboidEdge edge : atlas.edges()) {
            if (!edge.transform().isIdentity()) {
                CuboidFace first = faceOf(atlas, edge.first());
                passages.add(new EdgePassage(fold, atlas, edge, first, samplesOf(edge, first)));
            }
        }

        return passages;
    }

    private static CuboidFace faceOf(CuboidAtlas atlas, CuboidFace.Side side) {
        for (CuboidFace face : atlas.faces()) {
            if (face.side() == side) {
                return face;
            }
        }

        throw new IllegalArgumentException("The atlas has no " + side + " face");
    }

    private static List<SeamCauseSearch.Point> samplesOf(CuboidEdge edge, CuboidFace first) {
        int endX = first.minBlockX() + first.widthBlocks();
        int endZ = first.minBlockZ() + first.depthBlocks();
        List<SeamCauseSearch.Point> samples = new ArrayList<>();
        for (int sample = 1; sample <= SAMPLES_PER_EDGE; sample++) {
            int alongX = first.minBlockX() + first.widthBlocks() * sample / (SAMPLES_PER_EDGE + 1);
            int alongZ = first.minBlockZ() + first.depthBlocks() * sample / (SAMPLES_PER_EDGE + 1);
            samples.add(switch (edge.firstSide()) {
                case WEST -> new SeamCauseSearch.Point(first.minBlockX() - PAST_THE_EDGE, SAMPLE_Y, alongZ);
                case EAST -> new SeamCauseSearch.Point(endX + PAST_THE_EDGE, SAMPLE_Y, alongZ);
                case NORTH -> new SeamCauseSearch.Point(alongX, SAMPLE_Y, first.minBlockZ() - PAST_THE_EDGE);
                case SOUTH -> new SeamCauseSearch.Point(alongX, SAMPLE_Y, endZ + PAST_THE_EDGE);
                default -> throw new IllegalArgumentException("A face has no " + edge.firstSide() + " edge");
            });
        }

        return List.copyOf(samples);
    }

    @Override
    public SeamCauseSearch.Point away(SeamCauseSearch.Point point) {
        return new SeamCauseSearch.Point(
                Mth.floor(this.edge.transform().applyX(point.x(), point.z())),
                point.y(),
                Mth.floor(this.edge.transform().applyZ(point.x(), point.z())));
    }

    @Override
    public <T> T reading(SeamCauseSearch.Point at, Supplier<T> read) {
        try (GenerationFoldContext.Context.FoldScope _ =
                GenerationFoldContext.context().withFold(this.fold, askingChunk(at))) {
            return read.get();
        }
    }

    ChunkPos askingChunk(SeamCauseSearch.Point at) {
        if (this.atlas.faceAt(at.x(), at.z()) != null) {
            return ChunkPos.containing(new BlockPos(at.x(), at.y(), at.z()));
        }

        int endX = this.first.minBlockX() + this.first.widthBlocks();
        int endZ = this.first.minBlockZ() + this.first.depthBlocks();
        return ChunkPos.containing(new BlockPos(
                Mth.clamp(at.x(), this.first.minBlockX(), endX - 1),
                at.y(),
                Mth.clamp(at.z(), this.first.minBlockZ(), endZ - 1)));
    }
}
