# Exotic Worlds

**Sail west and come home from the east.** Exotic Worlds wraps your world around: walk, fly or sail across the world's edge and you arrive from the opposite side. No barrier, no teleport, nothing to see where you crossed.

<img src="https://raw.githubusercontent.com/anyttng/exotic_worlds/main/docs/images/flight.avif" alt="Flying straight across a tiny toroidal world and arriving back at the landmark from the opposite side">

## Before you download

- The world shape is chosen **at world creation**: an existing vanilla world cannot be converted into a toroidal or cylinder one (or back).

## Features

- A **World Shape** option on the world-creation screen: pick a shape, set the size in chunks.
- Terrain wraps: mountains, biomes and structures carry on across the world's edge, with no mirrored wall or cut-off cliff.
- The game works across the edge, not just the blocks: mob AI and pathfinding, projectiles and explosions, sounds and particles, compasses and lodestones, villages and raids, distance-based advancements all treat the world as round. In short: [how it works](https://github.com/anyttng/exotic_worlds/blob/main/docs/how-it-works.md).
- The Nether and the End take the shape too: the Nether scaled to the overworld, the End with its own size.
- Modded terrain and dimensions come along: a world-generation mod that changes vanilla's terrain settings keeps its terrain, wrapped; a modded dimension that generates the vanilla way takes the world's shape, and one with its own generator is left as its mod makes it.

## World shapes

- **Toroidal**: both horizontal axes loop. You can also set a shift: crossing the Z edge then moves you along X too, so arriving back where you started can take several laps.

  <img src="https://raw.githubusercontent.com/anyttng/exotic_worlds/main/docs/images/torus-map.png" width="480" alt="A tiny toroidal world on JourneyMap">

  *A tiny toroidal world on JourneyMap: the same world repeats in every direction, and the white lines mark the world's edge.*

  <img src="https://raw.githubusercontent.com/anyttng/exotic_worlds/main/docs/images/skewed-torus-map.png" width="480" alt="A tiny toroidal world with a shift on JourneyMap">

  *The same with a shift: each row of copies is shifted along X against the rows next to it.*

- **Cylinder**: one horizontal axis loops, the other goes on forever like vanilla; you pick which axis loops, and the Nether and the End loop along the same one.

  <img src="https://raw.githubusercontent.com/anyttng/exotic_worlds/main/docs/images/cylinder-map.png" width="480" alt="A tiny cylinder world on JourneyMap">

  *A cylinder: the world repeats along X and runs on along Z.*

## Dedicated servers

One line in `server.properties` creates a shaped world: `level-type=exotic_worlds:torus_medium` for a toroidal one, `level-type=exotic_worlds:cylinder_medium` for a cylinder, each in five sizes: `tiny`, `small`, `medium`, `large`, `huge`. Custom sizes and the full walkthrough are in the [dedicated server guide](https://github.com/anyttng/exotic_worlds/blob/main/docs/dedicated-server.md).

## Compatibility

Runs on **NeoForge** and **Fabric**.

- [Sodium](https://modrinth.com/mod/sodium) and [Iris Shaders](https://modrinth.com/mod/iris).
- [JourneyMap](https://modrinth.com/mod/journeymap), [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) and [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map).
- [Distant Horizons](https://modrinth.com/mod/distanthorizons): the far side of the world shows across the world's edge, out to half the world width.

Every mod checked against the world's shape, with the game versions and loaders each one applies to, is in the [supported mods list](https://github.com/anyttng/exotic_worlds/blob/main/docs/supported-mods.md).

## Bug reports

Found something broken, especially anything that behaves differently near the world's edge? Report it on the [issue tracker](https://github.com/anyttng/exotic_worlds/issues).
