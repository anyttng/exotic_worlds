# Making Your Own Mechanism Cross the Seam

Part of [Exotic Worlds from Another Mod](../modding.md).

The folds in [Exotic Worlds from Another Mod](../modding.md) move a coordinate you are holding. This part is about the coordinates you are *sending*, and about groups of them that have to stay together.

Every vanilla packet that carries a world position is already rewritten on its way out. A payload of yours is too, when it is a record that keeps each world position in a component of its own. For the rest, a registered rewriter says which field is a position.

## The positions file

What a type cannot say about its positions goes into one data file per namespace, shipped in your own jar or in a datapack.

`data/<namespace>/exotic_worlds/positions.json`

It takes five keys, each described below where it is used. `payloads` and `deny` are for payload records, `block_entities` and `entities` for NBT, `components` for item components.

## A payload of your own

A payload declared as a record needs no code. Each component whose declared type is `BlockPos`, `Vec3`, `ChunkPos`, `SectionPos` or `GlobalPos`, or an `Optional` or a `List` of one, is folded on its way out and on its way back. A `GlobalPos` folds only in the dimension the player stands in.

What a record cannot say takes a rewriter. That covers a position written into a byte array by hand, a position inside a component of any other type, and a `long` holding a packed position.

```java
PacketRewritersV2.registerClientboundPayload(StationCursorPayload.class, (payload, context) ->
        new StationCursorPayload(context.toClient(payload.target())));

PacketRewritersV2.registerServerboundPayload(StationPickPayload.class, (payload, context) ->
        new StationPickPayload(context.toServer(payload.picked())));
```

`toClient` takes a canonical position to the copy that client holds, the one to write into anything it will draw, place or measure against. `toServer` folds a position the client sent back into the world's bounds. Both come in `BlockPos`, `Vec3` and `ChunkPos` forms, both hand the argument instance itself back when nothing moved, and `context.getGeometry()` is the level's whole geometry for anything the two do not cover.

Register from your mod's initializer. A registration after a world has started throws. A rewriter runs for payloads of that exact class, on a folding level and nowhere else, so on an ordinary world nothing you registered is ever called. Where one is registered the record fold does not run for that class.

Never hold a context past the call.

A `BlockPos` component that is not a world position (an offset, a size, a position local to a contraption) is folded all the same. Two positions that belong together, such as the corners of one selection, are folded each on its own, so a pair straddling the seam arrives a world apart. Say what the type cannot in a `payloads` row of [the positions file](#the-positions-file). The row is keyed by the payload type id, names record components and gives each a role.

```json
{
  "payloads": {
    "your_mod:structure_bounds": { "origin": "world", "size": "untouched" },
    "your_mod:selection": { "from": "group", "to": "group" },
    "your_mod:trail": { "curve": { "tangent": "untouched" } }
  }
}
```

- `world` folds the component as a world position, as it would be unnamed.
- `untouched` crosses as sent, both ways.
- `group` members move together, at any depth. The first of them is taken into the world's bounds on the way in, or to the copy nearest the player on the way out. Members are read in component order, a nested record's components in that record's place, past an empty `Optional` or a `null`. The others move by the same shift and keep their distance from it, past the bounds if that is where it puts them.

A component that holds a record takes an object of its own in place of a role, naming that record's components the same way. It works at any depth and reaches every record an `Optional` or a `List` holds. A role on such a component applies to the whole record.

A component the row does not name folds by its type, at any depth. A name the record does not declare is skipped, with a warning in the server log, and so is an object on a component that holds no record. Two objects that two namespaces give one component join name by name. Where two namespaces give one component different entries, it folds by its type and the server log says so.

To leave a whole payload alone, list its id under `deny`. It then crosses untouched in both directions, whatever a `payloads` row says.

```json
{
  "deny": [ "your_mod:raw_cursor" ]
}
```

A rewriter registered for the class still runs.

## Positions your block entity or entity stores

Name the NBT keys of a block entity's sync tag or an entity's spawn data that hold a world position in [the positions file](#the-positions-file), and each one reaches the client on the copy of the world that client holds, with no code and no dependency on Exotic Worlds.

```json
{
  "block_entities": {
    "your_mod:relay": {
      "Partner": "block_pos",
      "Link": { "Target": "block_pos" },
      "Queue": [ { "Destination": "vec3_list" } ]
    }
  },
  "entities": {
    "your_mod:tether": {
      "Anchor": "packed_long"
    }
  }
}
```

A row is keyed by the block entity type or entity type id. Inside it, a key whose value is a shape is a position at the top level of the tag. A key whose value is an object names a compound, and each key inside it is a position in that compound. A key whose value is a list of one object names a list of compounds, and each key of that object is a position in every compound of the list. One level of nesting is all a row describes.

A shape says how the position is written.

- `block_pos` is an int array of three, as `BlockPos.CODEC` writes it
- `packed_long` is a long, as `BlockPos.asLong` writes it
- `vec3_list` is a list of three doubles, as `Vec3.CODEC` writes it

A value whose tag type does not match its shape is left as it is.

`block_entities` rows apply to the tag a client receives for the block entity, with its chunk or on its own update. `entities` rows apply on NeoForge alone, to the spawn data of an entity implementing `IEntityWithComplexSpawn` whose buffer opens with a compound tag. Fabric has no such buffer.

A row naming an id no mod registers is logged and skipped. Where Exotic Worlds already carries a position at the same key for that block entity or entity, its own row is kept and yours is logged.

## Positions your item components store

Name the type id of a data component that holds a world position in the same file.

```json
{
  "components": [ "your_mod:linked_anchor" ]
}
```

On a toroidal world, a read of a named component through `ItemStack.get` or `getOrDefault` on the client's own thread answers the copy nearest the player. A read through `getComponents()` is not moved. The value's form is read as a payload record's is, so it may be a `BlockPos`, `Vec3`, `ChunkPos`, `SectionPos` or `GlobalPos`, an `Optional` or a `List` of one, or a record holding them. A `GlobalPos` is moved only in the dimension the player stands in.

When a creative-mode client puts a stack into a slot, each named component is folded back into the world's bounds on its way in. A nearest-copy value your client code writes back into the stack therefore never reaches the server as it was written.

A component whose `BlockPos` is not a world position must not be named. `minecraft:lodestone_tracker` is named already. An id naming no registered component type is logged and skipped.

## A particle type of your own

```java
PacketRewritersV2.registerParticle(BeaconTrailOption.class, (particle, context, clientOrigin) ->
        new BeaconTrailOption(context.toClient(particle.destination())));
```

`clientOrigin` is where the particle itself lands in the client's frame. Move a position the particle points at to the copy nearest that, not nearest the player, or a long trail flips across the seam halfway along.

## A group that has to stay rigid

Moving each member of a rigid group (a contraption, a vehicle, a multiblock, a set of linked nodes) with `nearestCopy` on its own tears it.

Take one shift from an anchor instead, and apply it to every member.

```java
SeamShiftV2 shift = geometry.shiftToNearestCopy(player.position(), contraption.anchor());
for (BlockPos member : contraption.blocks()) {
    render(shift.apply(member));
}
Vec3 heading = shift.getOrientation().applyToDelta(contraption.velocity());
```

`apply` comes in `Vec3`, `BlockPos`, `ChunkPos` and `AABB` forms. The box form moves rigidly too, so its size never changes and it is never split at a seam. `isIdentity()` is the normal answer for a group already in the right copy, and there every `apply` hands its argument straight back.

A shift is not a vector, so subtracting the anchor's nearest copy from the original and adding that difference to the rest is wrong. `getOrientation().applyToDelta` carries a velocity or a facing through the same move.

`WorldGeometryV2.nearestCopy(ref, box)` is the same rigid move for a single box you do not need a shift object for.

## Anchors the client already has

`ClientAnchors` lives in `api.v1` alone.

```java
BlockPos nearest = ClientAnchors.nearestToCamera(marker);
```

`nearestToPlayer` comes in `BlockPos`, `Vec3` and `ChunkPos` forms, and `nearestToCamera` takes a point. Something drawn folds toward the camera, and in third person or a spectator's view that is not where the player is.

With no level, no player or no folding world every member hands the argument straight back, so a call needs no guard of its own.

`ClientAnchors.heldCopy` answers a different question, which copy of a position the client is *holding*, not which is nearest. Anything you key by a block position on the client (a block entity, a rendered overlay, a cached state) has to be keyed by the copy that is actually there.

```java
BlockPos held = ClientAnchors.heldCopy(canonical);
if (held == null) {
    return;
}
```

`null` means the client holds none of them. Treat it as "not loaded", because using the canonical position instead puts your entry on a chunk that is not there.

`heldCopy(chunkPos, holds)` asks the same for a chunk of your own storage. `holds` is your test of whether a given copy is the one present, and the answer is that copy or `null`.
