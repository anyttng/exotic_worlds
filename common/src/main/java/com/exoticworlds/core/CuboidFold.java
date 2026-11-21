package com.exoticworlds.core;

import java.util.List;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.option.GenerationOptions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CuboidFold implements WorldFold {
    static final int WEST = 0;
    static final int EAST = 1;
    static final int NORTH = 2;
    static final int SOUTH = 3;
    static final int SIDES = 4;
    private static final int NO_BAND = -1;
    static final int NO_FACE = -1;
    static final double NOT_PAST = -1.0;
    private static final double CELL_CENTER = 0.5;
    private static final int CHUNK_CENTER = CoordinateConstants.CHUNK_WIDTH / 2;

    record Crossing(int face, int entrySide, SeamTransform transform) {
    }

    record Rect(int minX, int minZ, int endX, int endZ) {
        static Rect of(CuboidFace face) {
            return new Rect(
                    face.minBlockX(),
                    face.minBlockZ(),
                    face.minBlockX() + face.widthBlocks(),
                    face.minBlockZ() + face.depthBlocks());
        }

        int smallestEdgeChunks() {
            return Math.min(this.endX - this.minX, this.endZ - this.minZ) / CoordinateConstants.CHUNK_WIDTH;
        }

        boolean contains(int blockX, int blockZ) {
            return blockX >= this.minX && blockX < this.endX && blockZ >= this.minZ && blockZ < this.endZ;
        }

        double bandDistance(int side, double x, double z, int blockX, int blockZ) {
            boolean alongZ = side == WEST || side == EAST;
            boolean inSpan = alongZ
                    ? blockZ >= this.minZ && blockZ < this.endZ
                    : blockX >= this.minX && blockX < this.endX;
            if (!inSpan) {
                return NOT_PAST;
            }

            return switch (side) {
                case WEST -> blockX < this.minX ? this.minX - x : NOT_PAST;
                case EAST -> blockX >= this.endX ? x - this.endX : NOT_PAST;
                case NORTH -> blockZ < this.minZ ? this.minZ - z : NOT_PAST;
                default -> blockZ >= this.endZ ? z - this.endZ : NOT_PAST;
            };
        }

        int chunkOvershoot(int chunkX, int chunkZ) {
            int pastX = Math.max(chunksPast(this.minX, this.endX, chunkX), 0);
            int pastZ = Math.max(chunksPast(this.minZ, this.endZ, chunkZ), 0);
            return Math.max(pastX, pastZ);
        }

        private static int chunksPast(int min, int end, int chunk) {
            int minChunk = SectionPos.blockToSectionCoord(min);
            int lastChunk = SectionPos.blockToSectionCoord(end) - 1;
            return Math.max(minChunk - chunk, chunk - lastChunk);
        }
    }

    private final CuboidAtlas atlas;

    private final GenerationOptions generationOptions;

    private final Rect[] faces;

    private final Crossing[][] crossings;

    private final CuboidUnfoldings unfoldings;

    private final CuboidRegions regions;

    private final int maxViewDistance;

    public CuboidFold(CuboidAtlas atlas, GenerationOptions generationOptions) {
        this.atlas = atlas;
        this.generationOptions = generationOptions;
        List<CuboidFace> atlasFaces = atlas.faces();
        this.faces = new Rect[atlasFaces.size()];
        int smallestEdge = Integer.MAX_VALUE;
        for (int face = 0; face < this.faces.length; face++) {
            this.faces[face] = Rect.of(atlasFaces.get(face));
            smallestEdge = Math.min(smallestEdge, this.faces[face].smallestEdgeChunks());
        }

        this.crossings = crossingsOf(atlasFaces, atlas.edges());
        this.unfoldings = CuboidUnfoldings.of(this.crossings);
        this.regions = new CuboidRegions(this.faces, this.crossings);
        this.maxViewDistance = Math.max(1, smallestEdge / 2 - CoordinateConstants.VIEW_DISTANCE_MARGIN);
    }

    private static Crossing[][] crossingsOf(List<CuboidFace> faces, List<CuboidEdge> edges) {
        Crossing[][] crossings = new Crossing[faces.size()][SIDES];
        for (CuboidEdge edge : edges) {
            int first = indexOf(faces, edge.first());
            int second = indexOf(faces, edge.second());
            int firstSide = sideOf(edge.firstSide());
            int secondSide = sideOf(edge.secondSide());
            crossings[first][firstSide] = new Crossing(second, secondSide, edge.transform());
            crossings[second][secondSide] = new Crossing(first, firstSide, edge.transform().inverse());
        }

        return crossings;
    }

    private static int indexOf(List<CuboidFace> faces, CuboidFace.Side side) {
        for (int face = 0; face < faces.size(); face++) {
            if (faces.get(face).side() == side) {
                return face;
            }
        }

        throw new IllegalArgumentException("The atlas has no " + side + " face");
    }

    private static int sideOf(Direction side) {
        return switch (side) {
            case WEST -> WEST;
            case EAST -> EAST;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            default -> throw new IllegalArgumentException("A face has no " + side + " edge");
        };
    }

    static int opposite(int side) {
        return side ^ 1;
    }

    static int faceOf(Rect[] faces, int blockX, int blockZ) {
        for (int face = 0; face < faces.length; face++) {
            if (faces[face].contains(blockX, blockZ)) {
                return face;
            }
        }

        return NO_FACE;
    }

    private int faceOf(int blockX, int blockZ) {
        return faceOf(this.faces, blockX, blockZ);
    }

    private int nearestBand(double x, double z, int blockX, int blockZ) {
        int nearest = NO_BAND;
        double nearestDistance = Double.MAX_VALUE;
        boolean tied = false;
        for (int band = 0; band < this.faces.length * SIDES; band++) {
            double distance = this.faces[band / SIDES].bandDistance(band % SIDES, x, z, blockX, blockZ);
            if (distance == NOT_PAST || distance > nearestDistance) {
                continue;
            }

            tied = distance == nearestDistance;
            nearest = band;
            nearestDistance = distance;
        }

        return tied ? NO_BAND : nearest;
    }

    private SeamTransform moveOf(double x, double z, int blockX, int blockZ) {
        if (faceOf(blockX, blockZ) != NO_FACE) {
            return SeamTransform.IDENTITY;
        }

        int band = nearestBand(x, z, blockX, blockZ);
        if (band == NO_BAND) {
            return SeamTransform.IDENTITY;
        }

        return walk(band / SIDES, band % SIDES, blockX, blockZ);
    }

    private SeamTransform walk(int startFace, int startSide, int startBlockX, int startBlockZ) {
        SeamTransform move = SeamTransform.IDENTITY;
        int face = startFace;
        int side = startSide;
        int blockX = startBlockX;
        int blockZ = startBlockZ;
        while (true) {
            Crossing crossing = this.crossings[face][side];
            int carriedX = crossing.transform().applyCellX(blockX, blockZ);
            int carriedZ = crossing.transform().applyCellZ(blockX, blockZ);
            blockX = carriedX;
            blockZ = carriedZ;
            move = move.then(crossing.transform());
            face = crossing.face();
            if (this.faces[face].contains(blockX, blockZ)) {
                return move;
            }

            side = opposite(crossing.entrySide());
        }
    }

    private SeamTransform moveOfChunk(int chunkX, int chunkZ) {
        int blockX = SectionPos.sectionToBlockCoord(chunkX);
        int blockZ = SectionPos.sectionToBlockCoord(chunkZ);
        return moveOf(blockX + CHUNK_CENTER, blockZ + CHUNK_CENTER, blockX, blockZ);
    }

    private SeamTransform moveOfBlock(int blockX, int blockZ) {
        return moveOf(blockX + CELL_CENTER, blockZ + CELL_CENTER, blockX, blockZ);
    }

    private SeamTransform moveOfCoord(double x, double z) {
        return moveOf(x, z, Mth.floor(x), Mth.floor(z));
    }

    private SeamTransform nearestMove(double refX, double refZ, int refBlockX, int refBlockZ,
            double x, double z, int blockX, int blockZ) {
        SeamTransform refMove = moveOf(refX, refZ, refBlockX, refBlockZ);
        SeamTransform targetMove = moveOf(x, z, blockX, blockZ);
        int refFace = faceOf(refMove.applyCellX(refBlockX, refBlockZ), refMove.applyCellZ(refBlockX, refBlockZ));
        int targetFace = faceOf(targetMove.applyCellX(blockX, blockZ), targetMove.applyCellZ(blockX, blockZ));
        if (refFace == NO_FACE || targetFace == NO_FACE || (refFace == targetFace && refMove.equals(targetMove))) {
            return SeamTransform.IDENTITY;
        }

        SeamTransform unfolding = this.unfoldings.nearest(refFace, targetFace,
                refMove.applyX(refX, refZ), refMove.applyZ(refX, refZ),
                targetMove.applyX(x, z), targetMove.applyZ(x, z));
        return targetMove.then(unfolding).then(refMove.inverse());
    }

    private SeamTransform nearestMoveOfChunk(int refChunkX, int refChunkZ, int chunkX, int chunkZ) {
        int refBlockX = SectionPos.sectionToBlockCoord(refChunkX);
        int refBlockZ = SectionPos.sectionToBlockCoord(refChunkZ);
        int blockX = SectionPos.sectionToBlockCoord(chunkX);
        int blockZ = SectionPos.sectionToBlockCoord(chunkZ);
        return nearestMove(refBlockX + CHUNK_CENTER, refBlockZ + CHUNK_CENTER, refBlockX, refBlockZ,
                blockX + CHUNK_CENTER, blockZ + CHUNK_CENTER, blockX, blockZ);
    }

    private SeamTransform nearestMoveOfBlock(BlockPos ref, BlockPos target) {
        return nearestMove(ref.getX() + CELL_CENTER, ref.getZ() + CELL_CENTER, ref.getX(), ref.getZ(),
                target.getX() + CELL_CENTER, target.getZ() + CELL_CENTER, target.getX(), target.getZ());
    }

    private SeamTransform nearestMoveOfCoord(double refX, double refZ, double x, double z) {
        return nearestMove(refX, refZ, Mth.floor(refX), Mth.floor(refZ), x, z, Mth.floor(x), Mth.floor(z));
    }

    private static int carriedChunkX(SeamTransform move, int chunkX, int chunkZ) {
        return SectionPos.blockToSectionCoord(move.applyCellX(
                SectionPos.sectionToBlockCoord(chunkX), SectionPos.sectionToBlockCoord(chunkZ)));
    }

    private static int carriedChunkZ(SeamTransform move, int chunkX, int chunkZ) {
        return SectionPos.blockToSectionCoord(move.applyCellZ(
                SectionPos.sectionToBlockCoord(chunkX), SectionPos.sectionToBlockCoord(chunkZ)));
    }

    private static DeckTransformation carried(SeamTransform move) {
        return move.isIdentity() ? DeckTransformation.IDENTITY : new DeckTransformation(move);
    }

    private static IllegalStateException noPeriod() {
        return new IllegalStateException("A cuboid has no period");
    }

    @Override
    public WorldLoopBounds bounds() {
        throw noPeriod();
    }

    @Override
    public boolean isWrapped() {
        return true;
    }

    @Override
    public boolean decomposesPerAxis() {
        return false;
    }

    @Override
    public boolean preservesLocalIndices() {
        return false;
    }

    @Override
    public GenerationOptions generationOptions() {
        return this.generationOptions;
    }

    @Override
    public WrapDomain blockDomain(Direction.Axis axis) {
        throw noPeriod();
    }

    @Override
    public WrapDomain chunkDomain(Direction.Axis axis) {
        throw noPeriod();
    }

    @Override
    public TranslationLattice blockLattice() {
        throw noPeriod();
    }

    @Override
    public TranslationLattice chunkLattice() {
        throw noPeriod();
    }

    @Override
    public ToroidalShape shapeView() {
        throw new UnsupportedOperationException("The cuboid fold does not carry shapeView yet");
    }

    @Override
    public CuboidAtlas cuboidAtlas() {
        return this.atlas;
    }

    @Override
    public boolean isOver(Vec3 pos) {
        return isOverBlock(Mth.floor(pos.x), Mth.floor(pos.z));
    }

    @Override
    public boolean isOver(BlockPos pos) {
        return isOverBlock(pos.getX(), pos.getZ());
    }

    @Override
    public boolean isOver(ChunkPos pos) {
        return isOverBlock(SectionPos.sectionToBlockCoord(pos.x()), SectionPos.sectionToBlockCoord(pos.z()));
    }

    private boolean isOverBlock(int blockX, int blockZ) {
        return faceOf(blockX, blockZ) == NO_FACE;
    }

    @Override
    public int chunkOvershoot(ChunkPos pos) {
        int overshoot = Integer.MAX_VALUE;
        for (Rect face : this.faces) {
            overshoot = Math.min(overshoot, face.chunkOvershoot(pos.x(), pos.z()));
        }

        return overshoot;
    }

    @Override
    public int maxViewDistance() {
        return this.maxViewDistance;
    }

    @Override
    public Vec3 fold(Vec3 pos) {
        return moveOfCoord(pos.x, pos.z).apply(pos);
    }

    @Override
    public BlockPos fold(BlockPos pos) {
        return moveOfBlock(pos.getX(), pos.getZ()).apply(pos);
    }

    @Override
    public ChunkPos fold(ChunkPos pos) {
        SeamTransform move = moveOfChunk(pos.x(), pos.z());
        return move.isIdentity()
                ? pos
                : new ChunkPos(carriedChunkX(move, pos.x(), pos.z()), carriedChunkZ(move, pos.x(), pos.z()));
    }

    @Override
    public SectionPos fold(SectionPos pos) {
        SeamTransform move = moveOfChunk(pos.x(), pos.z());
        return move.isIdentity()
                ? pos
                : SectionPos.of(carriedChunkX(move, pos.x(), pos.z()), pos.y(), carriedChunkZ(move, pos.x(), pos.z()));
    }

    @Override
    public long foldBlockNode(long blockNode) {
        int x = BlockPos.getX(blockNode);
        int z = BlockPos.getZ(blockNode);
        SeamTransform move = moveOfBlock(x, z);
        return move.isIdentity()
                ? blockNode
                : BlockPos.asLong(move.applyCellX(x, z), BlockPos.getY(blockNode), move.applyCellZ(x, z));
    }

    @Override
    public long foldChunkKey(long chunkKey) {
        int x = ChunkPos.getX(chunkKey);
        int z = ChunkPos.getZ(chunkKey);
        SeamTransform move = moveOfChunk(x, z);
        return move.isIdentity() ? chunkKey : ChunkPos.pack(carriedChunkX(move, x, z), carriedChunkZ(move, x, z));
    }

    @Override
    public long foldSectionNode(long sectionNode) {
        int x = SectionPos.x(sectionNode);
        int z = SectionPos.z(sectionNode);
        SeamTransform move = moveOfChunk(x, z);
        return move.isIdentity()
                ? sectionNode
                : SectionPos.asLong(carriedChunkX(move, x, z), SectionPos.y(sectionNode), carriedChunkZ(move, x, z));
    }

    @Override
    public Folded<Vec3> foldOriented(Vec3 pos) {
        SeamTransform move = moveOfCoord(pos.x, pos.z);
        return new Folded<>(move.apply(pos), move.orientation());
    }

    @Override
    public Folded<BlockPos> foldOriented(BlockPos pos) {
        SeamTransform move = moveOfBlock(pos.getX(), pos.getZ());
        return new Folded<>(move.apply(pos), move.orientation());
    }

    @Override
    public Folded<ChunkPos> foldOriented(ChunkPos pos) {
        SeamTransform move = moveOfChunk(pos.x(), pos.z());
        return new Folded<>(carried(move).apply(pos), move.orientation());
    }

    @Override
    public DeckTransformation foldTransformation(Vec3 pos) {
        return carried(moveOfCoord(pos.x, pos.z));
    }

    @Override
    public DeckTransformation deckTransformation(ChunkPos chunk, ChunkPos copy) {
        if (chunk.equals(copy)) {
            return DeckTransformation.IDENTITY;
        }

        SeamTransform chunkMove = moveOfChunk(chunk.x(), chunk.z());
        SeamTransform copyMove = moveOfChunk(copy.x(), copy.z());
        if (!carried(chunkMove).apply(chunk).equals(carried(copyMove).apply(copy))) {
            throw new IllegalArgumentException(copy + " is not a copy of " + chunk + " on the cuboid");
        }

        return carried(chunkMove.then(copyMove.inverse()));
    }

    @Override
    public BlockPos reseat(BlockPos pos, ChunkPos copy) {
        return deckTransformation(ChunkPos.containing(pos), copy).apply(pos);
    }

    @Override
    public Vec3 nearestCopy(Vec3 ref, Vec3 target) {
        return nearestMoveOfCoord(ref.x, ref.z, target.x, target.z).apply(target);
    }

    @Override
    public BlockPos nearestCopy(BlockPos ref, BlockPos target) {
        return nearestMoveOfBlock(ref, target).apply(target);
    }

    @Override
    public ChunkPos nearestCopy(ChunkPos ref, ChunkPos target) {
        return carried(nearestMoveOfChunk(ref.x(), ref.z(), target.x(), target.z())).apply(target);
    }

    @Override
    public long nearestCopyChunkKey(long refKey, long chunkKey) {
        int x = ChunkPos.getX(chunkKey);
        int z = ChunkPos.getZ(chunkKey);
        SeamTransform move = nearestMoveOfChunk(ChunkPos.getX(refKey), ChunkPos.getZ(refKey), x, z);
        return move.isIdentity() ? chunkKey : ChunkPos.pack(carriedChunkX(move, x, z), carriedChunkZ(move, x, z));
    }

    @Override
    public Folded<Vec3> nearestCopyOriented(Vec3 ref, Vec3 target) {
        SeamTransform move = nearestMoveOfCoord(ref.x, ref.z, target.x, target.z);
        return new Folded<>(move.apply(target), move.orientation());
    }

    @Override
    public Folded<BlockPos> nearestCopyOriented(BlockPos ref, BlockPos target) {
        SeamTransform move = nearestMoveOfBlock(ref, target);
        return new Folded<>(move.apply(target), move.orientation());
    }

    @Override
    public Folded<AABB> nearestCopyOriented(Vec3 ref, AABB box) {
        SeamTransform move = nearestMoveOfCoord(ref.x, ref.z,
                SeamBoxes.center(box.minX, box.maxX), SeamBoxes.center(box.minZ, box.maxZ));
        return new Folded<>(move.apply(box), move.orientation());
    }

    @Override
    public DeckTransformation nearestCopyTransformation(Vec3 ref, Vec3 target) {
        return carried(nearestMoveOfCoord(ref.x, ref.z, target.x, target.z));
    }

    @Override
    public DeckTransformation nearestCopyTransformation(BlockPos ref, BlockPos target) {
        return carried(nearestMoveOfBlock(ref, target));
    }

    @Override
    public double sqrDistance(double xFrom, double yFrom, double zFrom, double xTo, double yTo, double zTo) {
        SeamTransform move = nearestMoveOfCoord(xFrom, zFrom, xTo, zTo);
        double dx = move.applyX(xTo, zTo) - xFrom;
        double dy = yTo - yFrom;
        double dz = move.applyZ(xTo, zTo) - zFrom;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public int sqrChunkDistance(ChunkPos from, ChunkPos to) {
        SeamTransform move = nearestMoveOfChunk(from.x(), from.z(), to.x(), to.z());
        int dx = carriedChunkX(move, to.x(), to.z()) - from.x();
        int dz = carriedChunkZ(move, to.x(), to.z()) - from.z();
        return dx * dx + dz * dz;
    }

    @Override
    public boolean crossesBounds(AABB box) {
        return this.regions.crosses(box);
    }

    @Override
    public boolean crossesBounds(BoundingBox region) {
        return this.regions.crosses(region);
    }

    @Override
    public List<Folded<AABB>> split(AABB box) {
        return this.regions.split(box);
    }

    @Override
    public List<Folded<BoundingBox>> split(BoundingBox region) {
        return this.regions.split(region);
    }

    @Override
    public boolean regionsOverlap(BoundingBox first, BoundingBox second) {
        return this.regions.regionsOverlap(first, second);
    }

    @Override
    public List<DeckTransformation> copiesTouching(BoundingBox region, int reach) {
        return this.regions.copiesTouching(region, reach);
    }

    @Override
    public boolean foldsOntoItself(BoundingBox region) {
        return this.regions.foldsOntoItself(region);
    }
}
