package com.exoticworlds.core;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CuboidFace.BoxVector;
import com.exoticworlds.core.CuboidFace.Side;

import net.minecraft.core.Direction;

public record CuboidAtlas(List<CuboidFace> faces, List<CuboidEdge> edges, List<CuboidVertex> vertices) {
    private static final BoxVector BOX_CORNER = new BoxVector(0, 0, 0);
    private static final BoxVector ALONG_A = new BoxVector(1, 0, 0);
    private static final BoxVector AGAINST_A = new BoxVector(-1, 0, 0);
    private static final BoxVector ALONG_B = new BoxVector(0, 1, 0);
    private static final BoxVector ALONG_C = new BoxVector(0, 0, 1);
    private static final BoxVector AGAINST_C = new BoxVector(0, 0, -1);

    private record Layout(int a, int b, int c, int minX, int minZ) {
        int frontX() {
            return this.minX + this.c;
        }

        int eastX() {
            return frontX() + this.a;
        }

        int backX() {
            return eastX() + this.c;
        }

        int endX() {
            return backX() + this.a;
        }

        int stripZ() {
            return this.minZ + this.c;
        }

        int bottomZ() {
            return stripZ() + this.b;
        }

        int endZ() {
            return bottomZ() + this.c;
        }
    }

    public static CuboidAtlas of(int aChunks, int bChunks, int cChunks, int minChunkX, int minChunkZ) {
        if (aChunks < 1 || bChunks < 1 || cChunks < 1) {
            throw new IllegalArgumentException(
                    "A cuboid edge is at least one chunk, got " + aChunks + " x " + bChunks + " x " + cChunks);
        }

        Layout layout = new Layout(
                aChunks * CoordinateConstants.CHUNK_WIDTH,
                bChunks * CoordinateConstants.CHUNK_WIDTH,
                cChunks * CoordinateConstants.CHUNK_WIDTH,
                minChunkX * CoordinateConstants.CHUNK_WIDTH,
                minChunkZ * CoordinateConstants.CHUNK_WIDTH);
        return new CuboidAtlas(facesOf(layout), edgesOf(layout), verticesOf(layout));
    }

    public @Nullable CuboidFace faceAt(int blockX, int blockZ) {
        for (CuboidFace face : this.faces) {
            if (face.contains(blockX, blockZ)) {
                return face;
            }
        }

        return null;
    }

    private static List<CuboidFace> facesOf(Layout layout) {
        int a = layout.a();
        int b = layout.b();
        int c = layout.c();
        return List.of(
                new CuboidFace(Side.WEST, layout.minX(), layout.stripZ(), c, b, BOX_CORNER, ALONG_C, ALONG_B),
                new CuboidFace(Side.FRONT, layout.frontX(), layout.stripZ(), a, b,
                        new BoxVector(0, 0, c), ALONG_A, ALONG_B),
                new CuboidFace(Side.EAST, layout.eastX(), layout.stripZ(), c, b,
                        new BoxVector(a, 0, c), AGAINST_C, ALONG_B),
                new CuboidFace(Side.BACK, layout.backX(), layout.stripZ(), a, b,
                        new BoxVector(a, 0, 0), AGAINST_A, ALONG_B),
                new CuboidFace(Side.TOP, layout.frontX(), layout.minZ(), a, c, BOX_CORNER, ALONG_A, ALONG_C),
                new CuboidFace(Side.BOTTOM, layout.frontX(), layout.bottomZ(), a, c,
                        new BoxVector(0, b, c), ALONG_A, AGAINST_C));
    }

    private static List<CuboidEdge> edgesOf(Layout layout) {
        SeamTransform topToBack = halfTurn(layout.frontX() + layout.endX(), layout.minZ() + layout.stripZ());
        SeamTransform bottomToBack = halfTurn(layout.frontX() + layout.endX(), layout.bottomZ() + layout.endZ());
        return List.of(
                inside(Side.WEST, Direction.EAST, Side.FRONT, Direction.WEST),
                inside(Side.FRONT, Direction.EAST, Side.EAST, Direction.WEST),
                inside(Side.EAST, Direction.EAST, Side.BACK, Direction.WEST),
                inside(Side.TOP, Direction.SOUTH, Side.FRONT, Direction.NORTH),
                inside(Side.FRONT, Direction.SOUTH, Side.BOTTOM, Direction.NORTH),
                new CuboidEdge(Side.TOP, Direction.NORTH, Side.BACK, Direction.NORTH, topToBack),
                new CuboidEdge(Side.TOP, Direction.WEST, Side.WEST, Direction.NORTH,
                        counterclockwiseAbout(layout.frontX(), layout.stripZ())),
                new CuboidEdge(Side.TOP, Direction.EAST, Side.EAST, Direction.NORTH,
                        clockwiseAbout(layout.eastX(), layout.stripZ())),
                new CuboidEdge(Side.BOTTOM, Direction.SOUTH, Side.BACK, Direction.SOUTH, bottomToBack),
                new CuboidEdge(Side.BOTTOM, Direction.WEST, Side.WEST, Direction.SOUTH,
                        clockwiseAbout(layout.frontX(), layout.bottomZ())),
                new CuboidEdge(Side.BOTTOM, Direction.EAST, Side.EAST, Direction.SOUTH,
                        counterclockwiseAbout(layout.eastX(), layout.bottomZ())),
                new CuboidEdge(Side.WEST, Direction.WEST, Side.BACK, Direction.EAST,
                        SeamTransform.translation(layout.endX() - layout.minX(), 0)));
    }

    private static List<CuboidVertex> verticesOf(Layout layout) {
        int a = layout.a();
        int b = layout.b();
        int c = layout.c();
        return List.of(
                new CuboidVertex(new BoxVector(0, 0, c), List.of(Side.WEST, Side.FRONT, Side.TOP)),
                new CuboidVertex(new BoxVector(a, 0, c), List.of(Side.FRONT, Side.EAST, Side.TOP)),
                new CuboidVertex(new BoxVector(0, b, c), List.of(Side.BOTTOM, Side.FRONT, Side.WEST)),
                new CuboidVertex(new BoxVector(a, b, c), List.of(Side.BOTTOM, Side.EAST, Side.FRONT)),
                new CuboidVertex(new BoxVector(a, 0, 0), List.of(Side.EAST, Side.BACK, Side.TOP)),
                new CuboidVertex(new BoxVector(a, b, 0), List.of(Side.BACK, Side.EAST, Side.BOTTOM)),
                new CuboidVertex(BOX_CORNER, List.of(Side.BACK, Side.WEST, Side.TOP)),
                new CuboidVertex(new BoxVector(0, b, 0), List.of(Side.WEST, Side.BACK, Side.BOTTOM)));
    }

    private static CuboidEdge inside(Side first, Direction firstSide, Side second, Direction secondSide) {
        return new CuboidEdge(first, firstSide, second, secondSide, SeamTransform.IDENTITY);
    }

    private static SeamTransform halfTurn(int doubledCenterX, int doubledCenterZ) {
        return new SeamTransform(-1, -1, doubledCenterX, doubledCenterZ);
    }

    private static SeamTransform clockwiseAbout(int pivotX, int pivotZ) {
        return new SeamTransform(-1, 1, pivotX + pivotZ, pivotZ - pivotX, true);
    }

    private static SeamTransform counterclockwiseAbout(int pivotX, int pivotZ) {
        return new SeamTransform(1, -1, pivotX - pivotZ, pivotZ + pivotX, true);
    }
}
