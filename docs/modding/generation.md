# Hooking World Generation

Part of [Exotic Worlds from Another Mod](../modding.md).

## Hooking a generation moment

`GenerationHooks.atRandomState(key, hook)` runs a hook as a level's `RandomState` finishes building: the moment its noise router exists and nothing has sampled it yet, so rewriting a noise in that router still reaches every chunk.

```java
GenerationHooks.atRandomState("band_floor", (randomState, shape, options, seaLevel) -> {
    if (shape.loops(Direction.Axis.X) || !shape.loops(Direction.Axis.Z) || !options.get(OPTION)) {
        return;
    }

    liftTheCoastNoise(randomState.router(), shape.widthBlocks(Direction.Axis.Z));
});
```

`OPTION` is the world option declared in [Declaring a world option](shapes.md#declaring-a-world-option).

**A hook gates itself.** Every registered hook runs for every folding level of every world, whatever shape made it and whichever mod declared that shape; a hook is not scoped to the shape it was registered beside. A world builds one `RandomState` per dimension, so it runs once for the overworld, once for the nether and once for the End. Read `shape` and `options` and return early unless both are what the hook is for; reaching for a span on an axis that does not loop throws.

The key orders the hooks against one another and, like an option key, must be unique across every mod. Registration closes at the same boundary as everything else.

### Adding structure starts

`GenerationHooks.atStructureStarts(key, hook)` asks a hook once per structure set of a folding level, before the level generates or searches for its first structure start, for starts to add beside the ones the set's own placement picks. An added start is generated in its chunk and found by `/locate` and every other structure search.

```java
GenerationHooks.atStructureStarts("band_beacons", set -> {
    if (!set.options().get(OPTION) || !set.structureSet().is(BEACON_SET)) {
        return List.of();
    }

    Holder<Structure> beacon = set.structureSet().value().structures().get(0).structure();
    ChunkPos centre = new ChunkPos(0, 0);
    return set.canStart(beacon, centre) ? List.of(new StructureStarts.Added(beacon, centre)) : List.of();
});
```

The hook gates itself, as a `RandomState` hook does: it runs for every set of every folding level, so return an empty list unless the set, the shape and the options are what it is for. Do that before calling `picks()`, which runs a structure check per picked chunk.

- `picks()` lists the chunks inside the bounds that the set's own placement picks, each with the structure that starts there, or empty where every structure of the set fails its checks. Only a random-spread placement is listed; any other answers an empty list. It throws `IllegalStateException` unless both horizontal axes loop.
- `canStart(structure, chunk)` runs the game's own test for a chunk inside the bounds, biome check included.

Every answer depends on the seed, the shape and the options alone, so a hook that reads only these returns the same starts on every load. A start outside the bounds, on a chunk the placement already picked, or of a structure the set does not hold is dropped with a warning. The key works as for `atRandomState`.

## Generating terrain outside the chunk map

A mod that computes terrain itself (an LOD generator sampling noise on its own worker threads, a preview, a pre-generator) reads an unbounded vanilla world on a toroidal level unless it reads the level's field the way the chunk map does. `GenerationProbe` carries what that takes.

**Sample the level's own router, with the fold bound.** Read the `RandomState` the level already holds, `serverLevel.getChunkSource().randomState()`, and sample it inside `GenerationProbe.withFold`. Only the chunk map's own steps bind the fold on their thread, so without the call a noise the chunk map reads folded answers the unbounded value, and heights and biomes stop matching the world a player walks into.

```java
if (ExoticWorldsApi.shapeOf(level).isPresent()) {
    GenerationProbe.withFold(level, () -> sampleColumns(level, region));
} else {
    sampleColumns(level, region);
}
```

The binding is per thread and lasts for the call, so each worker thread wraps its own batch. `GenerationProbe.randomState(level, seed)` builds the router the level's shape and options would give another seed; a router built by hand with `RandomState.create` carries no fold.

**Key by the folded position.** A column past the world's edge is a copy of one inside it: store and look it up by `fold`, as [Which operation to reach for](../modding.md#which-operation-to-reach-for) describes.

**Clear what the chunk map clears.** On a toroidal level the chunk map removes masses of terrain left detached from the ground, in two passes. `GenerationProbe.sweepCrumbs(level, chunk)` applies the first to a chunk filled outside the chunk map, after the carvers. The second runs after lighting, over a 3 × 3 window of chunks:

1. Take `GenerationProbe.snapshotOf(chunk)` of every swept chunk; a chunk that never went through `sweepCrumbs` has none.
2. Put the nine snapshots of a window in row-major order from the lowest corner, the centre at index 4.
3. Call `clearBorderCrumbs(centre, window, reader)`, where `reader` reads the centre chunk and the eight around it. It writes the centre chunk alone, so a caller covering a region calls it once per chunk.

`borderCrumbs(window)` names the same positions without writing them. A window with a snapshot missing changes nothing.

**What the API does not cover.** Features, structures and End spikes a mod places with its own code miss what a toroidal level changes about them: structures sit on a folded sector grid, the level adds structure starts of its own, and none of it reaches code that computes placement itself.
