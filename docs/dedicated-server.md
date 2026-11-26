# Running Exotic Worlds on a Dedicated Server

The five sizes the world-creation screen offers, from Tiny to Huge, ship as world presets for the toroidal world and for the cylinder, and one `server.properties` line picks one. The result is a fully shaped world (overworld, Nether and End) that behaves exactly like one created in singleplayer. Joining players need the mod installed, and they receive the world's bounds automatically on login.

## Setup

1. Put the mod jar into the server's `mods/` folder.
2. In `server.properties`, pick a preset **before the first start**, because the world type is read only when the world is created:

```properties
level-type=exotic_worlds\:torus_medium
```

3. Start the server. No datapack needed.

A server that has already run needs a fresh `world` folder, and fresh means the whole folder. A half-cleared one is worse than an untouched one. With `level.dat` still in place and `data/minecraft/world_gen_settings.dat` deleted, the server neither loads the old world nor creates a new one, and it stops on `Overworld settings missing` before the world type is ever read. Delete the folder, then put any datapack back into `world/datapacks/`.

| Size | Toroidal `level-type` | Cylinder `level-type` | Overworld | Nether (portal scale) | End |
| --- | --- | --- | --- | --- | --- |
| Tiny | `exotic_worlds:torus_tiny` | `exotic_worlds:cylinder_tiny` | 32 chunks (512 blocks) | 16 chunks (256 blocks), 1:2 | 208 chunks (3328 blocks) |
| Small | `exotic_worlds:torus_small` | `exotic_worlds:cylinder_small` | 64 chunks (1024 blocks) | 16 chunks (256 blocks), 1:4 | 240 chunks (3840 blocks) |
| Medium | `exotic_worlds:torus_medium` | `exotic_worlds:cylinder_medium` | 128 chunks (2048 blocks) | 16 chunks (256 blocks), 1:8 | 304 chunks (4864 blocks) |
| Large | `exotic_worlds:torus_large` | `exotic_worlds:cylinder_large` | 256 chunks (4096 blocks) | 32 chunks (512 blocks), 1:8 | 432 chunks (6912 blocks) |
| Huge | `exotic_worlds:torus_huge` | `exotic_worlds:cylinder_huge` | 512 chunks (8192 blocks) | 64 chunks (1024 blocks), 1:8 | 688 chunks (11008 blocks) |

A toroidal preset loops both horizontal axes at the given width. A cylinder preset loops along X at that width and leaves Z endless like vanilla. A cylinder looping along Z is a custom preset with the two axes swapped (below).

If the id has a typo, vanilla logs a warning (`Failed to parse level-type …, defaulting to minecraft:normal`) and silently creates an ordinary infinite world. Check the first lines of the log if the world comes out as an ordinary infinite world.

WorldWeaver, the library behind BetterEnd and BetterNether, replaces `level-type` with its own default preset, and the log says nothing. Set `force_default_world_preset` to `false` in `config/wover/main.json` (the file is written on the first start), then create the world again from a fresh `world` folder.

## Custom sizes

Two ready packs sit next to this page. [`datapacks/torus/`](datapacks/torus) loops both axes, and [`datapacks/cylinder/`](datapacks/cylinder) loops X alone. Both carry a 192-chunk (3072-block) overworld, a 24-chunk (384-block) Nether at 1:8 and a 256-chunk (4096-block) End, and the toroidal one also states `climate_compression` in its `custom` form. Copy the folder into `world/datapacks/` **before the first server start**, point `server.properties` at `my_pack\:torus` or `my_pack\:cylinder`, and edit the numbers to the size you want. Change the overworld and the Nether has to follow it, as [Size rules](#size-rules) describes.

For a size outside the preset spread, define your own world preset in a datapack:

```
world/datapacks/my-toroidal/
├── pack.mcmeta
└── data/
    └── my_pack/
        └── worldgen/
            └── world_preset/
                └── my_toroidal.json
```

The `world/` folder does not exist yet on a fresh server. Create it with just the datapack inside, and the server builds the rest around it and enables the pack automatically on first start. The preset at `data/<namespace>/worldgen/world_preset/<file>.json` is picked with `level-type=<namespace>\:<file>`, where the backslash escapes the colon `.properties` would otherwise read as the end of the key.

`pack.mcmeta`:

```json
{
  "pack": {
    "description": "Custom toroidal world preset",
    "min_format": 121,
    "max_format": 121
  }
}
```

121 is the data pack format of Minecraft 26.3. For another game version, read `pack_version.data_major` out of the `version.json` inside the game jar.

`my_toroidal.json` carries the `tiny` configuration. In the `exotic_worlds:shape` object of each generator, `id` names the shape, `exotic_worlds:torus` or `exotic_worlds:cylinder`, and `wrapping` gives its bounds, which you change per dimension.

```json
{
  "dimensions": {
    "minecraft:overworld": {
      "type": "minecraft:overworld",
      "generator": {
        "type": "minecraft:noise",
        "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:overworld" },
        "settings": "minecraft:overworld",
        "exotic_worlds:shape": {
          "id": "exotic_worlds:torus",
          "wrapping": {
            "x": { "min_chunk": -16, "max_chunk": 16 },
            "z": { "min_chunk": -16, "max_chunk": 16 }
          },
          "climate_compression": false
        }
      }
    },
    "minecraft:the_nether": {
      "type": "minecraft:the_nether",
      "generator": {
        "type": "minecraft:noise",
        "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:nether" },
        "settings": "minecraft:nether",
        "exotic_worlds:shape": {
          "id": "exotic_worlds:torus",
          "wrapping": {
            "x": { "min_chunk": -8, "max_chunk": 8 },
            "z": { "min_chunk": -8, "max_chunk": 8 }
          },
          "climate_compression": false
        }
      }
    },
    "minecraft:the_end": {
      "type": "minecraft:the_end",
      "generator": {
        "type": "minecraft:noise",
        "biome_source": { "type": "minecraft:the_end" },
        "settings": "minecraft:end",
        "exotic_worlds:shape": {
          "id": "exotic_worlds:torus",
          "wrapping": {
            "x": { "min_chunk": -128, "max_chunk": 128 },
            "z": { "min_chunk": -128, "max_chunk": 128 }
          },
          "climate_compression": false
        }
      }
    }
  }
}
```

Then point `server.properties` at it with `level-type=my_pack\:my_toroidal`. Left at `exotic_worlds\:torus_medium`, the world is created from that preset and the pack changes nothing, with no warning in the log to say so.

For a Superflat world use `"type": "minecraft:flat"` with the flat generator's `"settings"` object, and put the same `exotic_worlds:shape` object beside it.

A pack written for an earlier version with `"type": "exotic_worlds:toroidal"` or `"exotic_worlds:toroidal_flat"` still loads as it is.

For a cylinder, set `id` to `exotic_worlds:cylinder`, give the looping axis its bounds and write the other axis as an empty object, the same axis in all three dimensions. This `wrapping` makes the overworld of a 64-chunk (1024-block) cylinder looping along Z:

```json
"wrapping": {
  "x": {},
  "z": { "min_chunk": -32, "max_chunk": 32 }
}
```

The empty object is required, because an axis left out of `wrapping` is an error, not an endless axis.

For a toroidal world with a shift per lap, add `skew_chunks` to the overworld's `wrapping`. Crossing the Z edge then also moves X by that many chunks. This `wrapping` makes a 128-chunk (2048-block) toroidal overworld whose Z crossing moves X by 16 chunks:

```json
"wrapping": {
  "x": { "min_chunk": -64, "max_chunk": 64 },
  "z": { "min_chunk": -64, "max_chunk": 64 },
  "skew_chunks": 16
}
```

The Nether's `wrapping` takes the overworld's skew divided by the portal scale, here `"skew_chunks": 2` at 1:8 ([Size rules](#size-rules)). The End is left without one, as the world-creation screen leaves it.

### Size rules

`min_chunk` and `max_chunk` are a half-open chunk range, so `-16 … 16` means 32 chunks (512 blocks), block bounds −256 … 255. The world-creation screen enforces these rules for you. A hand-written preset is checked at startup instead. A width under the floor stops the server before anything is generated, and the rest are named as `BROKEN` on the `World shape:` lines, logged at `WARN`:

- **Overworld** needs every looping axis at least 16 chunks (256 blocks) wide, centered on zero as the example does (`min_chunk = -width/2`).
- **Nether** needs at least 16 chunks (256 blocks) on every looping axis, even and centered. Portals link by the ratio of the two widths on each axis, so any pair of widths lines up at the world's edge. For vanilla's 1:8 travel, make the Nether an eighth of the overworld. An overworld that is a multiple of 16 chunks and 128 or wider always allows it. With `skew_chunks`, the Nether's skew is the overworld's scaled by the same ratio, and it must come out a whole number of chunks (`BROKEN nether skew`).
- **End** is independent of the other two. It needs at least 192 chunks (3072 blocks) on every looping axis (`BROKEN End width`), and in a cylinder it loops along the same axis as the overworld. Smaller Ends lose the outer island ring (no end cities, no elytra) and let gateway teleports reach across the world's edge. A dimension another mod adds with the End's island terrain follows the same rule.

## World options

Next to `wrapping`, the `exotic_worlds:shape` object takes the options the world-creation screen offers. Each dimension carries its own, so an option goes into the shape object of every dimension it should apply to.

**`climate_compression`** (toroidal and cylinder) shrinks biomes so a full climate fits into one lap of the world, while coastlines and terrain keep their usual size. It takes either a boolean (`true` for Auto, `false` for Off) or an object naming the mode:

```json
"climate_compression": { "mode": "strong" }
```

```json
"climate_compression": { "mode": "custom", "factor": 6 }
```

- `off` keeps biomes at their vanilla size, so a small world may come out all forest or all desert.
- `auto` shrinks them just enough for one climate per lap, and on a world already wide enough it changes nothing.
- `strong` shrinks them four times over, and more where fitting the climate needs more, never less than `auto`.
- `custom` shrinks them exactly `factor` times, a whole number from 1 to 64. `factor` is read in this mode alone.

**Leaving the field out means `auto`, not off.** Every shipped preset writes `"climate_compression": false`, so a hand-written preset started from one keeps that line, or gets compressed biomes it never asked for.

**`guaranteed_land`** (toroidal only) keeps a world from coming out all ocean. Where a lap would leave nowhere to stand, `true` raises the land level just enough for one island. A world that already has land is left as it is. It defaults to `false`.

**`guaranteed_nether_complexes`** (toroidal only) keeps a small Nether from coming out without a nether fortress or a bastion remnant. With `true`, each one the world did not place on its own is placed in the chunk nearest the world's center whose biome allows it. A dimension that places neither is left alone. Where no biome in the Nether allows one, it stays missing and the server log names it. It defaults to `false`.

**`spawn`** (toroidal and cylinder) sets where a new world starts looking for its spawn, in chunks. With `"spawn": { "x": 25, "z": -24 }` the search starts at block 400, -384 instead of 0, 0, and the player appears near there. The world, its edges and its terrain stay as they are. A point past the world's edge is taken at its copy inside the world. It is read on the Overworld alone. Without it the world looks for its spawn as it would without Exotic Worlds, and `{ "x": 0, "z": 0 }` is a point like any other. A terrain mod that picks the spawn its own way may ignore it.

## Dimensions other mods add

A dimension another mod or a datapack adds takes the overworld's shape by default, its width scaled by that dimension's coordinate scale. Where no whole number of chunks comes out, it stays an ordinary infinite world.

To choose its shape yourself, list it under `exotic_worlds:dimensions` on the overworld's generator, beside `exotic_worlds:shape`. An entry with `wrapping` gives that dimension its own shape and options, written as they are inside `exotic_worlds:shape`. An empty entry keeps it an ordinary infinite world.

```json
"exotic_worlds:dimensions": {
  "my_mod:sky": {
    "wrapping": {
      "x": { "min_chunk": -24, "max_chunk": 24 },
      "z": { "min_chunk": -16, "max_chunk": 16 }
    },
    "climate_compression": false
  },
  "my_mod:mines": {}
}
```

The list works on an overworld generator with no `exotic_worlds:shape` too, so an ordinary overworld can sit beside toroidal mod dimensions. A dimension the list leaves out keeps the default above. The Overworld, the Nether and the End cannot be listed, because each one takes its shape from its own generator. The `World shape:` line of a listed dimension reports `shape=table`.

## A datapack that redefines a dimension

A datapack that declares `minecraft:overworld`, `minecraft:the_nether` or `minecraft:the_end` cannot take the shape away. Such a dimension entry normally replaces the world's own generator for that dimension. Here it is rebuilt on the shape the world was created with, so the pack's terrain choice applies and the world keeps looping. The `World shape:` line logged at startup reports `shape=restored` and names the pack's generator. `shape=stamp` means that generator kept its own class and the shape was stamped onto it, and `shape=stored` means it could take no shape at all and the world's own was kept.
