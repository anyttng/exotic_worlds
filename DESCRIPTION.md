# Exotic Worlds

**Sail west and come home from the east.** Exotic Worlds wraps your world around. Walk, fly or sail across the world's edge and you come back in from the opposite side. There is no barrier and no teleport, and nothing marks the spot where you crossed.

<img src="https://raw.githubusercontent.com/anyttng/exotic_worlds/main/docs/images/flight.avif" alt="Flying straight across a tiny toroidal world and arriving back at the landmark from the opposite side">

Make a world small enough that every player on the server lives a short walk away, or just keep going in one direction until you spot your own base ahead of you. Mobs, arrows, maps and compasses all treat the world as round; the details are in [how it works](https://github.com/anyttng/exotic_worlds/blob/main/docs/how-it-works.md).

## World shapes

### Toroidal

The world loops both ways. Go east and you come back from the west, go north and you come back from the south.

<img src="https://raw.githubusercontent.com/anyttng/exotic_worlds/main/docs/images/torus-map.png" width="480" alt="A tiny toroidal world on JourneyMap">

*A tiny toroidal world on JourneyMap. The same world repeats in every direction, and the white lines mark the edge.*

<img src="https://raw.githubusercontent.com/anyttng/exotic_worlds/main/docs/images/skewed-torus-map.png" width="480" alt="A tiny toroidal world with a shift on JourneyMap">

*The same world with a shift. Each row of copies is offset sideways from the rows next to it.*

### Cylinder

The world loops one way, east to west or north to south, and goes on forever the other way, as in vanilla. The Nether and the End loop along the same direction.

<img src="https://raw.githubusercontent.com/anyttng/exotic_worlds/main/docs/images/cylinder-map.png" width="480" alt="A tiny cylinder world on JourneyMap">

*A cylinder. The world repeats along X and runs on along Z.*

## Settings

Pick a shape under **World Shape** on the world-creation screen, then tune it:

- **Size**: five presets from Tiny to Huge, or your own size in chunks, with X and Z set separately.
- **Nether scale**: 1:2, 1:4 or 1:8 against the overworld. The End gets a size of its own.
- **Shift** (toroidal only): crossing the north or south edge also moves you east or west, so getting back to where you started can take several laps.
- **Compact biomes**: shrinks biomes so a full range of climates fits into a small world. Without it a small world may be all forest or all desert.
- **Guaranteed land** and **Guaranteed fortress and bastion**: keep a small world from coming out all ocean, and a small Nether from having no fortress or bastion at all.

The shape is chosen when the world is created. An existing world cannot be converted into a shaped one, and a shaped world cannot be turned back.

## Dedicated servers

One line in `server.properties` creates a shaped world. Use `level-type=exotic_worlds:torus_medium` for a toroidal world or `level-type=exotic_worlds:cylinder_medium` for a cylinder. Both come in five sizes (`tiny`, `small`, `medium`, `large`, `huge`). Custom sizes and the full walkthrough are in the [dedicated server guide](https://github.com/anyttng/exotic_worlds/blob/main/docs/dedicated-server.md).

## Compatibility

Runs on **NeoForge** and **Fabric**.

- [Sodium](https://modrinth.com/mod/sodium) and [Iris Shaders](https://modrinth.com/mod/iris).
- [JourneyMap](https://modrinth.com/mod/journeymap), [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) and [Xaero's World Map](https://modrinth.com/mod/xaeros-world-map).
- [Distant Horizons](https://modrinth.com/mod/distanthorizons): the far side of the world shows across the world's edge, out to half the world width.

Every mod we have checked against a shaped world, with the game versions and loaders it applies to, is in the [supported mods list](https://github.com/anyttng/exotic_worlds/blob/main/docs/supported-mods.md).

## Bug reports

Found something broken, especially near the world's edge? Report it on the [issue tracker](https://github.com/anyttng/exotic_worlds/issues).
