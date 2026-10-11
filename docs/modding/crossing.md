# Making Your Own Mechanism Cross the Seam

Part of [Exotic Worlds from Another Mod](../modding.md).

The folds in [Exotic Worlds from Another Mod](../modding.md) move a coordinate you are holding. This part is about the coordinates you are *sending*, and about groups of them that have to stay together.

Every vanilla packet that carries a world position is already rewritten on its way out: the server's canonical position becomes whichever copy that client is holding, and back again on the way in. A payload of yours is too, when it is a record that keeps each world position in a component of its own; for the rest, a registered rewriter says which field is a position.

## The positions file

What a type cannot say about its positions goes into one data file per namespace, shipped in your own jar or in a datapack:

`data/<namespace>/exotic_worlds/positions.json`

It takes five keys, each described below where it is used: `payloads` and `deny` for payload records, `block_entities` and `entities` for NBT, `components` for item components. The server reads every namespace's file at start and on `/reload` and joins them; a resource pack on the client adds none.

## A payload of your own

A payload declared as a record needs no code. Each component whose declared type is `BlockPos`, `Vec3`, `ChunkPos`, `SectionPos` or `GlobalPos`, or an `Optional` or a `List` of one, is folded on its way out and on its way back, and the record is rebuilt through its canonical constructor; a `GlobalPos` folds only in the dimension the player stands in. A record with no such component, or none that moved, is handed on as it is.

What a record cannot say takes a rewriter: a position written into a byte array by hand, a position inside a component of any other type, a `long` holding a packed position.

```java
PacketRewriters.registerClientboundPayload(StationCursorPayload.class, (payload, context) ->
        new StationCursorPayload(context.toClient(payload.target())));

PacketRewriters.registerServerboundPayload(StationPickPayload.class, (payload, context) ->
        new StationPickPayload(context.toServer(payload.picked())));
```

`SeamContext` is the two frames one connection has. `toClient` takes a canonical position to the copy that client holds, the one to write into anything it will draw, place or measure against. `toServer` folds a position the client sent back into the world's bounds. Both come in `BlockPos`, `Vec3` and (clientbound) `ChunkPos` forms, both hand the argument instance itself back when nothing moved, and `context.shape()` is the level's whole geometry for anything the two do not cover.

Register from your mod's initialiser, beside everything else: the table closes at `MinecraftServer.runServer`, before the levels load, and a later registration throws rather than being silently half-effective. A rewriter runs for payloads of that exact class, on a folding level and nowhere else; on an ordinary world nothing you registered is ever called. Where one is registered the record fold does not run for that class; the record fold spells each position the way `toClient` and `toServer` do, so moving a payload between the two routes changes nothing its receiver reads.

A context belongs to the packet being rewritten. Never hold one past the call.

A `BlockPos` component that is not a world position (an offset, a size, a position local to a contraption) is folded all the same, and two positions that belong together, such as the corners of one selection, are folded each on its own, so a pair straddling the seam arrives a world apart. Say what the type cannot in a `payloads` row of [the positions file](#the-positions-file): keyed by the payload type id, it names record components and gives each a role.

```json
{
  "payloads": {
    "your_mod:structure_bounds": { "origin": "world", "size": "untouched" },
    "your_mod:selection": { "from": "group", "to": "group" }
  }
}
```

- `world` folds the component as a world position, as it would be unnamed.
- `untouched` crosses as sent, both ways.
- `group` members move together, by the one move that takes the first of them (in the record's component order, past an empty `Optional` or a `null`) into the world's bounds on the way in, or to the copy nearest the player on the way out. The others keep their distance from it, past the bounds if that is where it puts them.

A component the row does not name folds by its type. A name the record does not declare is skipped, with a warning in the server log. A row reaches the record's own components, not those of a record nested in one. Where two namespaces give one component different roles, it folds by its type and the server log says so.

To leave a whole payload alone, list its id under `deny`: it then crosses untouched in both directions, whatever a `payloads` row says.

```json
{
  "deny": [ "your_mod:raw_cursor" ]
}
```

A rewriter registered for the class still runs.

## Positions your block entity or entity stores

A block entity's sync tag and an entity's spawn data are NBT, and nothing in a tag says which number is a world position. Name those keys in [the positions file](#the-positions-file), and each one reaches the client on the copy of the world that client holds: no code, and no dependency on Exotic Worlds.

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

A row is keyed by the block entity type or entity type id. Inside it, a key whose value is a shape is a position at the top level of the tag; a key whose value is an object names a compound, and each key inside it is a position in that compound; a key whose value is a list of one object names a list of compounds, and each key of that object is a position in every compound of the list. One level of nesting is all a row describes.

A shape says how the position is written:

- `block_pos`: an int array of three, as `BlockPos.CODEC` writes it
- `packed_long`: a long, as `BlockPos.asLong` writes it
- `vec3_list`: a list of three doubles, as `Vec3.CODEC` writes it

A value whose tag type does not match its shape is left as it is.

`block_entities` rows apply to the tag a client receives for the block entity, with its chunk or on its own update. `entities` rows apply on NeoForge alone, to the spawn data of an entity implementing `IEntityWithComplexSpawn` whose buffer opens with a compound tag; Fabric has no such buffer.

The server sends the rows to each client as it joins and after each reload. A row naming an id no mod registers is logged and skipped. Where Exotic Worlds already carries a position at the same key for that block entity or entity, its own row is kept and yours is logged.

## Positions your item components store

A data component that holds a world position reaches the client as the server stored it, and a stack is not re-sent as the player walks, so the client reads it seated instead. Name the component type id in the same file:

```json
{
  "components": [ "your_mod:linked_anchor" ]
}
```

On a toroidal world, a read of a named component through `ItemStack.get` or `getOrDefault` on the client's own thread answers the copy nearest the player; the stack keeps the server's value, and a read through `getComponents()` is not seated. The value's form is read as a payload record's is: a `BlockPos`, `Vec3`, `ChunkPos`, `SectionPos` or `GlobalPos`, an `Optional` or a `List` of one, or a record holding them; a `GlobalPos` is seated only in the dimension the player stands in.

A stack a creative-mode client puts into a slot has each named component folded back into the world's bounds on its way in, so a seated value your client code writes back into the stack never reaches the server as it was written.

A component whose `BlockPos` is not a world position must not be named. `minecraft:lodestone_tracker` is named already. The list is read and sent like the rows above; an id naming no registered component type is logged and skipped.

## A particle type of your own

```java
PacketRewriters.registerParticle(BeaconTrailOption.class, (particle, context, clientOrigin) ->
        new BeaconTrailOption(context.toClient(particle.destination())));
```

`clientOrigin` is where the particle itself lands in the client's frame. Seat a position the particle points at near that rather than near the player, or a long trail flips across the seam halfway along.

## A group that has to stay rigid

A contraption, a vehicle, a multiblock, a set of linked nodes: anything whose members are meaningless apart. Seating each member with `nearestCopy` tears it: two blocks four apart, but on opposite sides of half a world width from your reference, pick different copies and end up a world away from each other.

Take one shift from an anchor instead, and apply it to every member:

```java
SeamShift shift = shape.shiftToNearestCopy(player.position(), contraption.anchor());
for (BlockPos member : contraption.blocks()) {
    render(shift.apply(member));
}
Vec3 heading = shift.orientation().applyToDelta(contraption.velocity());
```

`apply` comes in `Vec3`, `BlockPos`, `ChunkPos` and `AABB` forms; the box form moves rigidly too, so its size never changes and it is never split at a seam. `isIdentity()` is the normal answer for a group already in the right copy, and there every `apply` hands its argument straight back.

A shift is not a vector, which is why subtracting a seated anchor from the original and adding that difference to the rest is wrong: across a seam that mirrors, the shift also turns the space it moves. `orientation()` is that turn, and `applyToDelta` carries a velocity or a facing through it.

`ToroidalShape.nearestCopy(ref, box)` is the same rigid move for a single box you do not need a shift object for.

## Anchors the client already has

```java
BlockPos seated = ClientAnchors.nearestToCamera(marker);
```

`ClientAnchors` reads the anchor off the running client, so you state which one you mean and nothing else: `nearestToPlayer` in `BlockPos`, `Vec3` and `ChunkPos`, `nearestToCamera` for a point. Reaching for the wrong one shows: something drawn folds toward the camera, and in third person or a spectator's view that is not where the player is.

With no level, no player or no folding world every member hands the argument straight back, so a call needs no guard of its own.

`ClientAnchors.heldCopy` answers a different question: which copy of a position the client is *holding*, not which is nearest. A client near a seam is sent one copy of a chunk and not the others, and which one is the server's choice. Anything you key by a block position on the client (a block entity, a rendered overlay, a cached state) has to be keyed by the copy that is actually there:

```java
BlockPos held = ClientAnchors.heldCopy(canonical);
if (held == null) {
    return;
}
```

`null` means the client holds none of them. Treat it as "not loaded"; using the canonical position instead puts your entry on a chunk that is not there.

`heldCopy(chunkPos, holds)` asks the same for a chunk of your own storage: `holds` is your test of whether a given copy is the one present, and the answer is that copy or `null`.
