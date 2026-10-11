# Exotic Worlds from Another Mod

Exotic Worlds exposes one package to other mods: `com.exoticworlds.api.v1`. It does three things. It answers what the world's shape is and folds coordinates into it, so a mod that measures distance, keys storage by position or draws a marker keeps working when the world loops. It lets a mod carry its own packets, particles and rigid groups across the seam, which nothing but that mod's author can do for them. And it lets a mod declare a world shape of its own, which the player then picks on the create-world screen beside the ones this mod ships.

Everything outside that package is internal: it moves between releases without notice, and mixins into it are unsupported.

## Depending on it

```groovy
repositories {
    maven {
        name = 'Exotic Worlds'
        url = 'https://raw.githubusercontent.com/anyttng/exotic_worlds/maven/'
    }
}

dependencies {
    compileOnly 'com.exoticworlds:exotic-worlds-api:<mod version>'
}
```

The API version is the mod version; `maven-metadata.xml` under the coordinate lists what is published. Sources and javadoc are published beside the jar. The API follows semantic versioning against the mod version: within a major version, members are not removed or changed incompatibly.

`compileOnly` is the point: the artifact carries the API package alone, and at runtime the classes come from the copy of Exotic Worlds the player installed. Declare the mod as an optional dependency of yours and check that it is loaded before you touch any `com.exoticworlds` class; the mod id is `exotic_worlds`. A class that names one of them fails to load when the mod is absent, so keep those calls in a class you reach only after the check, not in a field initialiser of a class that always loads.

## Server side

`ExoticWorldsApi.shapeOf(level)` answers the geometry of a level whose own engine knows its bounds: a `ServerLevel`, or anything delegating to one. It is empty when no axis of that level loops, which is the normal answer on a world that is not toroidal.

```java
Optional<ToroidalShape> shape = ExoticWorldsApi.shapeOf(level);
if (shape.isEmpty()) {
    return distance(from, to);
}
```

The view is immutable and cheap; hold it for as long as the level lives.

`ExoticWorldsApi.travelOf(player, axis)` answers how far a player has travelled toward the next lap of the dimension they are in, in blocks, signed with the direction of travel. Pacing back and forth cancels out, crossing the seam does not count as a world width, and one whole width comes off each time a lap closes.

## Client side

A client level's own engine believes the world is infinite. That is deliberate: it is what keeps vanilla rendering and chunk loading working across the seam, and it is why `ExoticWorldsApi.shapeOf` answers empty for a `ClientLevel` even in a world that loops. The bounds the server declared ride apart on the client level, and `ExoticWorldsClientApi` is what reads them:

```java
Optional<ToroidalShape> shape = ExoticWorldsClientApi.shapeOf(clientLevel);
```

Every looping level arrives on login and on dimension change, before the first chunk, so a shape is there by the time anything is drawn. `ExoticWorldsClientApi.shapeOf(dimension)` takes a `ResourceKey<Level>` instead and answers for a dimension the player is not standing in, which is what a fullscreen map browsing another dimension folds by.

Client coordinates near the seam run whole world widths away from the server's, because the client holds whichever copy it was sent. Fold before you key anything by position, and measure against the player rather than against raw coordinates.

## Which operation to reach for

`ToroidalShape` offers three ways to move a coordinate, and they are not interchangeable:

- **`fold`**: the canonical position inside the world's bounds. Use it wherever a position becomes a key: a map, a set, a saved record, a hash. Two coordinates a lap apart are the same place, and only `fold` makes them the same key.
- **`nearestCopy(ref, target)`**: the copy of `target` closest to `ref`. Use it wherever something is drawn or compared: a renderer, a range check, a squared distance. Something just across the seam reads as beside the reference instead of a world away.
- **`shortestDelta(from, to)`**: the vector from one to the other, measured through the seam where that is shorter. Use it wherever a direction or a signed distance is the answer: a waypoint arrow, a compass, a distance readout. It equals `nearestCopy(from, to).subtract(from)`.

Two more answer for a rectangle rather than a reference point, which is what a map needs. `copiesMeeting(box)` lists every copy of the world that meets a horizontal rectangle, each as the `SeamShift` that carries the world there: a map asks it once per frame to know which tiles to draw. `copiesInside(box, pos)` lists every copy of one position inside the rectangle, in `Vec3` and `BlockPos` forms: a marker on that map asks it. Near a seam, or where the rectangle is wider than the world, both answer more than one copy. Build a copy from these, never by adding `widthBlocks`: on a skewed shape a copy one row over is also moved sideways.

Ask `loops(axis)` before any per-axis member: `minChunk`, `maxBlock`, `widthChunks` and the rest throw `IllegalArgumentException` on an axis that does not loop, and `Direction.Axis.Y` never loops.

## The `==` guarantee

Every fold hands back the argument instance itself when it moved nothing: `fold(pos) == pos`, `nearestCopy(ref, target) == target`, and likewise for the `value()` inside an `Oriented` result. A caller may read that `==` as "nothing moved" and skip the copy, the allocation or the write it would otherwise make:

```java
BlockPos folded = shape.fold(pos);
if (folded != pos) {
    rebuildWhateverWasKeyedBy(pos, folded);
}
```

`shortestDelta` is the one exception: its result is neither of its arguments, so it can only answer by value.

## The two capability flags

`ToroidalShape` carries two flags that say which shortcuts a shape allows. A plain torus and a cylinder answer `true` to both.

`decomposesPerAxis()` is false where crossing the seam on one axis also moves or flips the other. A player can create such a world today: a toroidal one with a shift per lap, where crossing the Z seam also moves X by the chosen number of chunks. There the per-axis members (`foldCoord`, `foldBlock`, `foldChunk`, `nearestCoord`) throw `IllegalStateException`, because one axis cannot be folded without the other; only the whole-position folds answer correctly. Reach for `fold(pos)` over `foldBlock(axis, coord)` where either will do, and the question never arises.

`preservesLocalIndices()` is false on a shape whose seam mirrors, where crossing it reverses a position's index inside its own chunk. Anything keyed by the low bits of a coordinate must be rebuilt across such a fold rather than carried over. No shape answers `false` yet, because the engine refuses to build a mirrored fold; the flag is in the API so that code written now survives the day one lands.

Across a mirrored seam a direction, a velocity or an offset that travelled with a position points the wrong way. The `Oriented` forms (`foldOriented`, `nearestCopyOriented`) report what the fold did, and `applyToDelta` carries a vector through the same turn:

```java
ToroidalShape.Oriented<Vec3> folded = shape.foldOriented(position);
Vec3 heading = folded.orientation().applyToDelta(velocity);
```

`Orientation.IDENTITY` is the only orientation an unmirrored shape ever reports (which today means the only one you will see), and `preservesHandedness()` tells a half turn from a genuine mirror.

## The rest of the API

- [Making your own mechanism cross the seam](modding/crossing.md): packets, particles, the positions your blocks, entities and items store, rigid groups, client anchors.
- [Declaring a world shape](modding/shapes.md): a shape of your own, its settings screen, world options.
- [Hooking world generation](modding/generation.md): generation hooks, structure starts, terrain computed outside the chunk map.
