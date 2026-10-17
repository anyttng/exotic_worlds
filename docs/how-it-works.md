# How Exotic Worlds Works

A shaped world has a fixed width. The terrain repeats at exactly that width, every distance is measured to the nearest copy of its target, and whatever walks off one side comes back in on the other. This page takes those one at a time.

## The shape lives inside the world

The width, and which axes loop, are stored in the world's chunk generators, the same place vanilla keeps its terrain settings. They are written there as the world is made, so the shape is picked on the world-creation screen (or with one `level-type` line on a server), and an existing world cannot be converted into a shaped one, or back. When you join, the server sends your client the shape of every dimension, so the client measures the world the same way the server does.

## The terrain repeats instead of ending

Vanilla terrain comes from noise sampled on a grid of cells that runs on forever. The mod resizes that grid so a whole number of cells fits into one lap of the world, and wraps the sampling around the border. The block one step past the +X border is built from exactly the same numbers as the block at the −X border, so a mountain, a river or a biome continues across the world's edge. Caves and the End islands come off the same wrapped noise, and structures are placed on a grid that wraps with the world.

One lap of a small world is short, so a full spread of climates may not fit into it and everything comes out one biome. The **Compact biomes** option shrinks the biome noise until a full climate range fits into one lap, while coastlines and terrain keep their usual size.

A world type from another mod may bring a terrain generator of its own. The world-creation screen warns about it, and a shape can still be chosen. The world gets its fixed width, and distances, crossings and everything else on this page work as usual. Only the terrain does not repeat, because the mod has no hold on noise it does not recognize, so the ground stops short in a cliff at the world's edge. A dimension another mod adds with a generator of its own takes its shape from the overworld and ends up the same way.

## Distances are measured to the nearest copy

Picture the finished map tiled over an endless plane, every tile identical. The game asks distance questions all the time: how far to that player, which way to that block, is that mob in range. The mod answers each one with the nearest of those copies, in a single place that the rest of the game reads through.

This one answer makes the world behave as round rather than just look it. Mob AI and pathfinding, projectiles and explosions, sounds and particles, compasses and lodestones, villages and raids, distance-based advancements all go through it. A mod that measures distance in its own code does not get this for free; which mods Exotic Worlds makes work across the world's edge is in the [supported mods list](supported-mods.md).

## Crossing the world's edge

Coordinates stay inside the world's bounds. An entity that steps over the +X border is moved to the −X side in the same tick, together with whatever it is riding, its speed and the path it was walking, so a mob crossing the border does not lose the target it was chasing. On a toroidal world with a shift, crossing the Z border also moves it along X by the shift.

You see none of that happen, because everything the server sends your client (chunks, entity positions, block updates, waypoints) is rewritten to the copy nearest to you. A chunk on the far side of the border arrives at the coordinates right next to you instead of at the opposite end of the map.
