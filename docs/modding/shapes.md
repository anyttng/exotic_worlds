# Declaring a World Shape

Part of [Exotic Worlds from Another Mod](../modding.md).

A shape is one declaration: an id, the settings it starts from, what it writes into the world's dimensions at creation, and how it reads itself back out of an existing world.

```java
public static final ShapeModule<BandSettings> MODULE = ShapeModule.of(
        Identifier.fromNamespaceAndPath("your_mod", "band"),
        BandSettings.DEFAULT,
        BandDimensions::apply,
        BandDimensions::read);
```

`MODULE.register()` enrols it. Call that from your mod's initialiser: the registry closes at `MinecraftServer.runServer`, before the levels load, and a `register` past that throws instead of being silently half-effective.

The settings type is yours and Exotic Worlds never looks inside it. `MODULE.settings()` is what a Customize screen reads and `MODULE.settings(chosen)` what it writes back; the module holds the player's choice until the world is made.

The shape's name and tooltip come from `gui.<namespace>.world_shape.<path>` and that key plus `.hint`; for the module above, `gui.your_mod.world_shape.band` and `gui.your_mod.world_shape.band.hint`.

## The geometry it declares

`LoopSpans` is the whole of it: which horizontal axes loop and over what span of chunks. Spans are half-open: `minChunk` is the first chunk inside the world, `maxChunk` the first one past it.

```java
LoopSpans.ofWidth(24);                        // both axes, 24 chunks, centred on the origin
LoopSpans.ofWidths(24, 48);                   // X over 24 chunks, Z over 48, each centred
LoopSpans.ofWidth(Direction.Axis.Z, 24);      // Z alone; X runs to the vanilla world border
LoopSpans.of(Direction.Axis.X, -12, 12);      // an explicit span
LoopSpans.of(Direction.Axis.X, -12, 12)
        .and(Direction.Axis.Z, -24, 24);      // explicit spans on both axes
LoopSpans.ofWidth(24).withSkew(6);            // crossing the Z seam also moves X by 6 chunks
```

Which flat surface the world ends up being is read off the spans, never declared: both axes give a torus, one gives a cylinder, neither gives an ordinary world. `withSkew` needs both axes to loop, and makes a torus whose axes do not fold independently ([The two capability flags](../modding.md#the-two-capability-flags)); `skewChunks()` reads it back, brought into the range from minus half the X width to plus half. `scaledDown(scale)` divides every looping axis and the skew, and re-centres: how the nether is normally derived from the overworld, so the vanilla portal ratio still lands inside the world. It throws where the scale does not divide the skew.

`ShapeDimensions` writes those spans into the three vanilla stems, and reads them back:

```java
public static WorldDimensions apply(WorldDimensions dimensions, BandSettings settings) {
    LoopSpans spans = settings.spans();
    return ShapeDimensions.withSpans(dimensions, spans, spans.scaledDown(8), spans, settings.options());
}

public static @Nullable BandSettings read(WorldDimensions dimensions) {
    LoopSpans spans = ShapeDimensions.spansOf(dimensions, LevelStem.OVERWORLD);
    if (spans == null || spans.loops(Direction.Axis.X) || !spans.loops(Direction.Axis.Z)) {
        return null;
    }

    return new BandSettings(spans, ShapeDimensions.optionsOf(dimensions, LevelStem.OVERWORLD));
}
```

`read` is asked of every registered shape when an existing world is opened, and the first one to answer non-null owns it. So refuse anything you did not write: check which axes loop and what `skewChunks()` answers, not merely that a shape is there. `spansOf` answers `null` for a stem carrying no shape of ours, and for a mirrored one, which spans cannot state; a skewed stem comes back with its skew.

The dimensions handed to `apply` have had any earlier shape stripped, so state your whole geometry rather than amending someone else's. A stem whose generator cannot take a shape is left alone.

## The settings screen

`ShapeCustomizers` maps the shape's id to the screen behind the Customize button; a shape with no customizer registered leaves that button dark. Register it from the client side only.

```java
ShapeCustomizers.register(MODULE.id(), parent -> new BandSettingsScreen(parent, MODULE.settings(), MODULE::settings));
```

## Declaring a world option

A world option is a value the player picks at creation, stored in the world and read back at load. It is a declaration, not a screen: the codec key, where it sits among the other options, its codec and its default.

```java
public static final WorldOption<Boolean> OPTION =
        new WorldOption<>("band_floor", 100, Codec.BOOL, Boolean.FALSE);

WorldOptions.register(OPTION);
```

The key is written into the world's generators and must be unique across every mod. `position` orders the option against the others on a settings screen; equal positions fall back to the key.

`GenerationOptions` is the value set a world carries: `get(option)` to read, `with(option, value)` to write, `GenerationOptions.DEFAULT` to start from. Only a value that differs from its option's default is written to the world file, so an option nobody touched costs nothing on disk.

The client half is one control per option:

```java
WorldOptionControls.register(OPTION, BandFloorControl::new);
```

A `WorldOptionControl` adds its own widgets, commits its value into the options, and may veto **Done** while its value is unusable. It reaches the screen around it only through `WorldOptionContext`: the parent screen, the loop width the screen states on each axis (`loopChunkWidth(axis)`; the no-argument form answers the narrower one), the current options, a change signal and a rebuild request. A shape's settings screen builds them all with `WorldOptionControls.createAll(context)`, in registry order.
