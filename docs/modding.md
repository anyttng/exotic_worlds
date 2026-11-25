# Exotic Worlds from Another Mod

Exotic Worlds exposes two packages to other mods, `com.exoticworlds.api.v2` and the older `com.exoticworlds.api.v1`. Write new code against `api.v2`. [What still works in `api.v1`](modding/v1.md) lists what the older package still does.

Everything outside those packages is internal. It moves between releases without notice, and mixins into it are unsupported.

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

The API version is the mod version, and `maven-metadata.xml` under the coordinate lists what is published. Sources and javadoc are published beside the jar. Within a major version, members are not removed or changed incompatibly, except those marked `@ExperimentalV2`.

`compileOnly` matters. The artifact carries the API package alone, and at runtime the classes come from the copy of Exotic Worlds the player installed. Declare the mod as an optional dependency of yours, under the mod id `exotic_worlds`, and check that it is loaded before you touch any `com.exoticworlds` class. A class that names one of them fails to load when the mod is absent, so keep those calls in a class you reach only after the check, not in a field initializer of a class that always loads.

## Server side

`WorldGeometriesV2.of(level)` answers the geometry of a level whose own engine knows its bounds, a `ServerLevel` or anything delegating to one. It is empty when no axis of that level loops, which is the normal answer on a world that is not toroidal.

```java
Optional<WorldGeometryV2> geometry = WorldGeometriesV2.of(level);
if (geometry.isEmpty()) {
    return distance(from, to);
}
```

The view is immutable and cheap, so hold it for as long as the level lives.

`ExoticWorldsApi.travelOf(player, axis)` answers how far a player has traveled toward the next lap of the dimension they are in, in blocks, signed with the direction of travel. Pacing back and forth cancels out, crossing the seam does not count as a world width, and one whole width comes off each time a lap closes. It lives in `api.v1` alone.

## Client side

`WorldGeometriesV2.of` answers empty for a `ClientLevel` even in a world that loops. `ClientGeometriesV2` in `api.v2.geometry.client` reads the bounds the server declared.

```java
Optional<WorldGeometryV2> geometry = ClientGeometriesV2.of(clientLevel);
```

Every looping level arrives on login and on dimension change, before the first chunk, so a geometry is there by the time anything is drawn. `ClientGeometriesV2.of(dimension)` takes a `ResourceKey<Level>` and answers for a dimension the player is not standing in, which a fullscreen map browsing another dimension folds by.

Client coordinates near the seam run whole world widths away from the server's, because the client holds whichever copy it was sent. Fold before you key anything by position, and measure against the player rather than against raw coordinates.

## Which operation to reach for

`WorldGeometryV2` offers three ways to move a coordinate, and they are not interchangeable.

- **`fold`** gives the canonical position inside the world's bounds. Use it wherever a position becomes a key, such as a map, a set, a saved record or a hash. Two coordinates a lap apart are the same place, and only `fold` makes them the same key.
- **`nearestCopy(ref, target)`** gives the copy of `target` closest to `ref`. Use it wherever something is drawn or compared, such as a renderer or a range check. Something just across the seam then reads as beside the reference instead of a world away.
- **`shortestDelta(from, to)`** gives the vector from one to the other, measured through the seam where that is shorter. Use it wherever a direction or a signed distance is the answer, such as a waypoint arrow, a compass or a distance readout.

Two more answer for a rectangle rather than a reference point, as a map needs. `copiesMeeting(box)` lists every copy of the world that meets a horizontal rectangle, each as the `SeamShiftV2` that carries the world there. A map asks it once per frame to know which tiles to draw. `copiesInside(box, pos)` lists every copy of one position inside the rectangle, in `Vec3` and `BlockPos` forms, and a marker on that map asks it. Near a seam, or where the rectangle is wider than the world, both answer more than one copy. Build a copy from these, never by adding a width, because on a skewed shape a copy one row over is also moved sideways.

Where only how far matters, `sqrDistanceBlocks(from, to)` answers the squared length of `shortestDelta(from, to)` without building either vector. For an area query, `areBoxesOverlapping(first, second)` says whether two boxes meet in some copy of the world. `split(box)` lists the pieces the seam cuts a box into, each folded into the world, and a scan of the area walks those pieces rather than the box.

Every member takes a whole position, and none folds one axis alone. Read a bound or a width from `getSpans()`, and ask `isLooped(axis)` before `getMinChunk`, `getMaxChunk` or `getWidthChunks`, which throw on an axis that does not loop.

## The `==` guarantee

Every fold hands back the argument instance itself when it moved nothing, so `fold(pos) == pos` and `nearestCopy(ref, target) == target`. A caller may read that `==` as "nothing moved" and skip the copy, the allocation or the write it would otherwise make.

```java
BlockPos folded = geometry.fold(pos);
if (folded != pos) {
    rebuildWhateverWasKeyedBy(pos, folded);
}
```

`shortestDelta` is the one exception. Its result is neither of its arguments, so it can only answer by value.

## The rest of the API

- [Adding a feature to a shape](modding/features.md), for generation rules and the options the player sets.
- [Declaring a world shape](modding/shapes.md), for a shape of your own and its parameters.
- [Making your own mechanism cross the seam](modding/crossing.md), for packets, particles, stored positions, rigid groups and client anchors.
- [Generating terrain outside the chunk map](modding/generation.md), for a mod that computes terrain itself.
- [What still works in `api.v1`](modding/v1.md), for the older package.
