# Running Exotic Worlds on a Dedicated Server

The five sizes the world-creation screen offers, from Tiny to Huge, ship as world presets for the toroidal world and for the cylinder, and one `server.properties` line picks one. The result is a fully shaped world (overworld, Nether and End) that behaves exactly like one created in singleplayer. Joining players need the mod installed; they receive the world's bounds automatically on login.

## Setup

1. Put the mod jar into the server's `mods/` folder.
2. In `server.properties`, pick a preset **before the first start**, because the world type is read only when the world is created:

```properties
level-type=exotic_worlds\:torus_medium
```

3. Start the server. No datapack needed.

A server that has already run needs a fresh `world` folder, and fresh means the whole folder. A half-cleared one is worse than an untouched one: with `level.dat` still in place and `data/minecraft/world_gen_settings.dat` deleted, the server neither loads the old world nor creates a new one; it stops on `Overworld settings missing` before the world type is ever read. Delete the folder, then put any datapack back into `world/datapacks/`.

| Size | Toroidal `level-type` | Cylinder `level-type` | Overworld | Nether (portal scale) | End |
| --- | --- | --- | --- | --- | --- |
| Tiny | `exotic_worlds:torus_tiny` | `exotic_worlds:cylinder_tiny` | 32 chunks (512 blocks) | 16 chunks (256 blocks), 1:2 | 256 chunks (4096 blocks) |
| Small | `exotic_worlds:torus_small` | `exotic_worlds:cylinder_small` | 64 chunks (1024 blocks) | 16 chunks (256 blocks), 1:4 | 320 chunks (5120 blocks) |
| Medium | `exotic_worlds:torus_medium` | `exotic_worlds:cylinder_medium` | 128 chunks (2048 blocks) | 16 chunks (256 blocks), 1:8 | 384 chunks (6144 blocks) |
| Large | `exotic_worlds:torus_large` | `exotic_worlds:cylinder_large` | 256 chunks (4096 blocks) | 32 chunks (512 blocks), 1:8 | 448 chunks (7168 blocks) |
| Huge | `exotic_worlds:torus_huge` | `exotic_worlds:cylinder_huge` | 512 chunks (8192 blocks) | 64 chunks (1024 blocks), 1:8 | 512 chunks (8192 blocks) |

A toroidal preset loops both horizontal axes at the given width. A cylinder preset loops along X at that width and leaves Z endless like vanilla; a cylinder looping along Z is a custom preset with the two axes swapped (below).

If the id has a typo, vanilla logs a warning (`Failed to parse level-type …, defaulting to minecraft:normal`) and silently creates an ordinary infinite world. Check the first lines of the log if the world comes out as an ordinary infinite world.

WorldWeaver, the library behind BetterEnd and BetterNether, replaces `level-type` with its own default preset, and the log says nothing. Set `force_default_world_preset` to `false` in `config/wover/main.json` (the file is written on the first start), then create the world again from a fresh `world` folder.

## Custom sizes

Two ready packs sit next to this page: [`datapacks/torus/`](datapacks/torus) loops both axes, [`datapacks/cylinder/`](datapacks/cylinder) loops X alone. Both carry a 192-chunk (3072-block) overworld, a 24-chunk (384-block) Nether at 1:8 and a 256-chunk (4096-block) End; the toroidal one also states `climate_compression` in its `custom` form. Copy the folder into `world/datapacks/` **before the first server start**, point `server.properties` at `my_pack\:torus` or `my_pack\:cylinder`, and edit the numbers to the size you want. Change the overworld and the Nether has to follow it, as [Size rules](#size-rules) describes.

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

The `world/` folder does not exist yet on a fresh server: create it with just the datapack inside; the server builds the rest around it and enables the pack automatically on first start. The preset resolves as `data/<namespace>/worldgen/world_preset/<file>.json` → `level-type=<namespace>\:<file>`, where the backslash escapes the colon `.properties` would otherwise read as the end of the key.

`pack.mcmeta`:

```json
{
  "pack": {
    "description": "Custom toroidal world preset",
    "min_format": 107,
    "max_format": 107
  }
}
```

107 is the data pack format of Minecraft 26.2. For another game version, read `pack_version.data_major` out of the `version.json` inside the game jar.

`my_toroidal.json` (the example carries the `tiny` configuration; change the `wrapping` bounds per dimension):

```json
{
  "dimensions": {
    "minecraft:overworld": {
      "type": "minecraft:overworld",
      "generator": {
        "type": "exotic_worlds:toroidal",
        "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:overworld" },
        "settings": "minecraft:overworld",
        "wrapping": {
          "x": { "min_chunk": -16, "max_chunk": 16 },
          "z": { "min_chunk": -16, "max_chunk": 16 }
        },
        "climate_compression": false
      }
    },
    "minecraft:the_nether": {
      "type": "minecraft:the_nether",
      "generator": {
        "type": "exotic_worlds:toroidal",
        "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:nether" },
        "settings": "minecraft:nether",
        "wrapping": {
          "x": { "min_chunk": -8, "max_chunk": 8 },
          "z": { "min_chunk": -8, "max_chunk": 8 }
        },
        "climate_compression": false
      }
    },
    "minecraft:the_end": {
      "type": "minecraft:the_end",
      "generator": {
        "type": "exotic_worlds:toroidal",
        "biome_source": { "type": "minecraft:the_end" },
        "settings": "minecraft:end",
        "wrapping": {
          "x": { "min_chunk": -128, "max_chunk": 128 },
          "z": { "min_chunk": -128, "max_chunk": 128 }
        },
        "climate_compression": false
      }
    }
  }
}
```

Then point `server.properties` at it: `level-type=my_pack\:my_toroidal`. Left at `exotic_worlds\:torus_medium`, the world is created from that preset and the pack changes nothing, with no warning in the log to say so.

For a Superflat world use `"type": "exotic_worlds:toroidal_flat"` with a `"settings"` object of the flat generator instead of the noise settings id.

For a cylinder, give the looping axis its bounds and write the other axis as an empty object, the same axis in all three dimensions. This `wrapping` makes the overworld of a 64-chunk (1024-block) cylinder looping along Z:

```json
"wrapping": {
  "x": {},
  "z": { "min_chunk": -32, "max_chunk": 32 }
}
```

The empty object is required: an axis left out of `wrapping` is an error, not an endless axis.

For a toroidal world with a shift per lap, add `skew_chunks` to the overworld's `wrapping`: crossing the Z edge then also moves X by that many chunks. This `wrapping` makes a 128-chunk (2048-block) toroidal overworld whose Z crossing moves X by 16 chunks:

```json
"wrapping": {
  "x": { "min_chunk": -64, "max_chunk": 64 },
  "z": { "min_chunk": -64, "max_chunk": 64 },
  "skew_chunks": 16
}
```

The Nether's `wrapping` takes the overworld's skew divided by the portal scale, here `"skew_chunks": 2` at 1:8 ([Size rules](#size-rules)). The End is left without one, as the world-creation screen leaves it.

### Size rules

`min_chunk`/`max_chunk` are a half-open chunk range: `-16 … 16` means 32 chunks (512 blocks), block bounds −256 … 255. The world-creation screen enforces these rules for you. A hand-written preset is checked at startup instead: a width under the floor stops the server before anything is generated, and the rest are named as `BROKEN` on the `World shape:` lines, logged at `WARN`:

- **Overworld**: every looping axis at least 16 chunks (256 blocks), centered on zero as the example does (`min_chunk = -width/2`).
- **Nether**: the overworld width divided by the portal scale, and the scale must divide it exactly (`BROKEN portal scale`); the Nether itself must stay at least 16 chunks (256 blocks) wide. An uneven ratio breaks portal linking near the world's edge. Choose the overworld as a multiple of 16 chunks, 128 or wider, and 1:8 always divides, with the Nether even and centered. With `skew_chunks`, the scale must divide the skew too, and the Nether's skew is the overworld's divided by the scale (`BROKEN nether skew`).
- **End**: independent of the other two, at least 192 chunks (3072 blocks) on every looping axis (`BROKEN End width`), and in a cylinder looping along the same axis as the overworld. Smaller Ends lose the outer island ring (no end cities, no elytra) and let gateway teleports reach across the world's edge.

## World options

Next to `wrapping`, the generator takes the options the world-creation screen offers. Each dimension carries its own, so an option goes on the generator of every dimension it should apply to.

**`climate_compression`** (toroidal and cylinder) shrinks biomes so a full climate fits into one lap of the world; coastlines and terrain keep their usual size. It takes either a boolean (`true` for Auto, `false` for Off) or an object naming the mode:

```json
"climate_compression": { "mode": "strong" }
```

```json
"climate_compression": { "mode": "custom", "factor": 6 }
```

- `off`: biomes keep their vanilla size, so a small world may come out all forest or all desert.
- `auto`: shrinks them just enough for one climate per lap; on a world already wide enough it changes nothing.
- `strong`: shrinks them four times over, and more where fitting the climate needs more, never less than `auto`.
- `custom`: shrinks them exactly `factor` times, a whole number from 1 to 64. `factor` is read in this mode alone.

**Leaving the field out means `auto`, not off.** Every shipped preset writes `"climate_compression": false`, so a hand-written preset started from one keeps that line, or gets compressed biomes it never asked for.

**`guaranteed_land`** (toroidal only) keeps a world from coming out all ocean: where a lap would leave nowhere to stand, `true` raises the land level just enough for one island. A world that already has land is left as it is. It defaults to `false`.

**`guaranteed_nether_complexes`** (toroidal only) keeps a small Nether from coming out without a nether fortress or a bastion remnant. With `true`, each one the world did not place on its own is placed in the chunk nearest the world's center whose biome allows it. A dimension that places neither is left alone. Where no biome in the Nether allows one, it stays missing and the server log names it. It defaults to `false`.

## A datapack that redefines a dimension

A datapack that declares `minecraft:overworld`, `minecraft:the_nether` or `minecraft:the_end` cannot take the shape away. Such a dimension entry normally replaces the world's own generator for that dimension; here it is rebuilt on the shape the world was created with, so the pack's terrain choice applies and the world keeps looping. The `World shape:` line logged at startup reports `shape=restored` and names the pack's generator. `shape=stamp` means that generator kept its own class and the shape was stamped onto it; `shape=stored` means it could take no shape at all and the world's own was kept.
