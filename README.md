# Exotic Worlds

A NeoForge and Fabric mod that gives the Minecraft world a fixed size that wraps around: a toroidal world, which loops east to west and north to south, or a cylinder, which loops one way and goes on forever the other. Terrain, structures, mobs and gameplay continue across the world's edge.

How the shape is made to work (terrain, distances and crossing the edge) is in [docs/how-it-works.md](docs/how-it-works.md).

## Versions

- Minecraft **26.3**, **26.2**, **26.1.2** and **1.21.1**
- NeoForge
- Fabric Loader with Fabric API

## Dedicated server

One `level-type` line in `server.properties` turns a dedicated server into a toroidal or cylinder world. The presets, custom sizes and size rules are in [docs/dedicated-server.md](docs/dedicated-server.md).

## For mod developers

Other mods read the world's shape, make their own mechanisms cross the edge and declare shapes of their own through `com.exoticworlds.api.v1`. The Maven coordinate and every walkthrough are in [docs/modding.md](docs/modding.md).

## Building from source

```
./gradlew build
```

The jar lands in `build/libs/`. The Gradle wrapper downloads the required JDK itself.

## License

PolyForm Shield 1.0.0, see [LICENSE](LICENSE).
