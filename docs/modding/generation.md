# Generating Terrain Outside the Chunk Map

Part of [Exotic Worlds from Another Mod](../modding.md).

A mod that computes terrain itself (an LOD generator sampling noise on its own worker threads, a preview, a pre-generator) sees an unbounded vanilla world on a toroidal level. It has to read the level's field the way the chunk map does, and `GenerationProbe` carries what that takes. `GenerationProbe` lives in `api.v1` alone.

**Sample the level's own router, with the fold bound.** Read the `RandomState` the level already holds, `serverLevel.getChunkSource().randomState()`, and sample it inside `GenerationProbe.withFold`. Only the chunk map's own steps bind the fold on their thread, so without the call a noise the chunk map reads folded answers the unbounded value, and heights and biomes stop matching the world a player walks into.

```java
if (WorldGeometriesV2.of(level).isPresent()) {
    GenerationProbe.withFold(level, () -> sampleColumns(level, region));
} else {
    sampleColumns(level, region);
}
```

The binding is per thread and lasts for the call, so each worker thread wraps its own batch. `GenerationProbe.randomState(level, seed)` builds the router the level's shape and options would give another seed. A router built by hand with `RandomState.create` carries no fold.

**Key by the folded position.** A column past the world's edge is a copy of one inside it. Store and look it up by `fold`, as [Which operation to reach for](../modding.md#which-operation-to-reach-for) describes.

**Clear what the chunk map clears.** On a toroidal level the chunk map removes masses of terrain left detached from the ground, in two passes. `GenerationProbe.sweepCrumbs(level, chunk)` applies the first to a chunk filled outside the chunk map, after the carvers. The second runs after lighting, over a 3 by 3 window of chunks.

1. Take `GenerationProbe.snapshotOf(chunk)` of every swept chunk. A chunk that never went through `sweepCrumbs` has none.
2. Put the nine snapshots of a window in row-major order from the lowest corner, the center at index 4.
3. Call `clearBorderCrumbs(center, window, reader)`, where `reader` reads the center chunk and the eight around it. It writes the center chunk alone, so a caller covering a region calls it once per chunk.

`borderCrumbs(window)` names the same positions without writing them. A window with a snapshot missing changes nothing.

**What the API does not cover.** Features, structures and End spikes a mod places with its own code miss what a toroidal level changes about them.
