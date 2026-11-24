package com.exoticworlds.engine.noise.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.ThreadScope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public final class FeatureRecording {
    private static final ThreadScope<FeatureRecording> SCOPE = new ThreadScope<>();

    private static final int TOP_PLACER = 1;
    private static final int SECTION_BLOCKS = SectionPos.SECTION_SIZE * SectionPos.SECTION_SIZE * SectionPos.SECTION_SIZE;
    private static final int SECTION_MASK = SectionPos.SECTION_SIZE - 1;
    private static final int Z_SHIFT = 4;
    private static final int Y_SHIFT = 8;

    sealed interface Step permits Modify, Place {
    }

    record Modify(PlacementModifier modifier, long input, List<Long> outputs) implements Step {
    }

    record Place(Feature feature, long pos, boolean isPlaced, Map<Long, BlockState> written) implements Step {
    }

    static final class Entry {
        private final PlacedFeature placed;
        private final List<Step> steps = new ArrayList<>();
        private boolean isPastTheReach;

        private Entry(PlacedFeature placed) {
            this.placed = placed;
        }

        PlacedFeature getPlaced() {
            return this.placed;
        }

        List<Step> getSteps() {
            return this.steps;
        }

        boolean isPastTheReach() {
            return this.isPastTheReach;
        }
    }

    public static final class Outputs implements Consumer<BlockPos> {
        private final Consumer<BlockPos> output;
        private final PlacementModifier modifier;
        private final long input;
        private final List<Long> positions = new ArrayList<>();
        private final FeatureRecording recording;

        private Outputs(FeatureRecording recording, PlacementModifier modifier, long input, Consumer<BlockPos> output) {
            this.recording = recording;
            this.modifier = modifier;
            this.input = input;
            this.output = output;
        }

        @Override
        public void accept(BlockPos pos) {
            this.positions.add(this.recording.relative(pos));
            this.output.accept(pos);
        }
    }

    private static final class Frame {
        private final HashMap<Long, BlockState> written = new HashMap<>();
        private final IdentityHashMap<ChunkAccess, BlockState[]> taken = new IdentityHashMap<>();
    }

    private final int originX;
    private final int originZ;
    private final List<Entry> entries = new ArrayList<>();
    private @Nullable Frame frame;
    private int depth;

    FeatureRecording(ChunkAccess center) {
        this.originX = center.getPos().getMinBlockX();
        this.originZ = center.getPos().getMinBlockZ();
    }

    static <R> R recording(FeatureRecording recording, Supplier<R> body) {
        return SCOPE.with(recording, body);
    }

    public static @Nullable FeatureRecording getCurrent() {
        return SCOPE.getCurrent();
    }

    List<Entry> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    public void enterPlaced(PlacedFeature placed) {
        if (this.depth == 0) {
            this.entries.add(new Entry(placed));
        }

        this.depth++;
    }

    public void exitPlaced() {
        this.depth--;
    }

    public Outputs modifying(PlacementModifier modifier, BlockPos input, Consumer<BlockPos> output) {
        return new Outputs(this, modifier, this.relative(input), output);
    }

    public void modified(Outputs outputs) {
        if (this.depth == TOP_PLACER) {
            this.step(new Modify(outputs.modifier, outputs.input, List.copyOf(outputs.positions)));
        }
    }

    public void enterFeature() {
        if (this.depth == TOP_PLACER) {
            this.frame = new Frame();
        }
    }

    public void exitFeature(Feature feature, BlockPos pos, boolean isPlaced) {
        Frame closing = this.frame;
        if (this.depth != TOP_PLACER || closing == null) {
            return;
        }

        this.frame = null;
        for (Map.Entry<ChunkAccess, BlockState[]> taken : closing.taken.entrySet()) {
            this.addSectionWrites(taken.getKey(), taken.getValue(), closing.written);
        }

        this.step(new Place(feature, this.relative(pos), isPlaced, Map.copyOf(closing.written)));
    }

    void wrote(BlockPos pos, BlockState state) {
        if (this.frame != null) {
            this.frame.written.put(this.relative(pos), state);
        }
    }

    void took(ChunkAccess chunk) {
        if (this.frame != null) {
            this.frame.taken.computeIfAbsent(chunk, FeatureRecording::statesOf);
        }
    }

    void isPastTheReach() {
        if (!this.entries.isEmpty()) {
            this.entries.getLast().isPastTheReach = true;
        }
    }

    private void step(Step step) {
        if (!this.entries.isEmpty()) {
            this.entries.getLast().steps.add(step);
        }
    }

    private long relative(BlockPos pos) {
        return BlockPos.asLong(pos.getX() - this.originX, pos.getY(), pos.getZ() - this.originZ);
    }

    private static BlockState[] statesOf(ChunkAccess chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        BlockState[] blocks = new BlockState[sections.length * SECTION_BLOCKS];
        for (int index = 0; index < sections.length; index++) {
            for (int cell = 0; cell < SECTION_BLOCKS; cell++) {
                blocks[index * SECTION_BLOCKS + cell] = getStateAt(sections[index], cell);
            }
        }

        return blocks;
    }

    private void addSectionWrites(ChunkAccess chunk, BlockState[] before, Map<Long, BlockState> written) {
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int index = 0; index < sections.length; index++) {
            int bottom = SectionPos.sectionToBlockCoord(chunk.getMinSectionY() + index);
            for (int cell = 0; cell < SECTION_BLOCKS; cell++) {
                BlockState now = getStateAt(sections[index], cell);
                if (now != before[index * SECTION_BLOCKS + cell]) {
                    pos.set(chunk.getPos().getMinBlockX() + (cell & SECTION_MASK), bottom + (cell >> Y_SHIFT),
                            chunk.getPos().getMinBlockZ() + ((cell >> Z_SHIFT) & SECTION_MASK));
                    written.putIfAbsent(this.relative(pos), now);
                }
            }
        }
    }

    private static BlockState getStateAt(LevelChunkSection section, int cell) {
        return section.getBlockState(cell & SECTION_MASK, cell >> Y_SHIFT, (cell >> Z_SHIFT) & SECTION_MASK);
    }
}
