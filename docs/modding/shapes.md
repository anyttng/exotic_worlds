# Declaring a World Shape

Part of [Exotic Worlds from Another Mod](../modding.md).

A shape is one class. It answers the spans of each dimension the player puts it on, and the player picks it with the **Shape** button of the **World Shape Settings** screen, on any dimension.

```java
public final class BandShape extends ShapeV2 {
    public static final ParameterV2<Integer> WIDTH = ParameterV2
            .of(Identifier.fromNamespaceAndPath("your_mod", "band_width"), Codec.intRange(16, 512), 64,
                    "gui.your_mod.band_width", List.of(OptionPartV2.number(16, 512)))
            .withPresetWidth((widthChunks, presetWidthChunks) -> presetWidthChunks);

    public BandShape() {
        super(Identifier.fromNamespaceAndPath("your_mod", "band"), "gui.your_mod.band", "gui.your_mod.band.hint");
    }

    @Override
    public List<ParameterV2<?>> getParameters() {
        return List.of(WIDTH);
    }

    @Override
    public LoopSpansV2 spansIn(ShapeDimensionV2 dimension) {
        return LoopSpansV2.ofWidth(Direction.Axis.X, dimension.getValue(WIDTH));
    }
}
```

`ShapesV2.register(new BandShape())` registers it. Call it from your mod's initializer. Registration closes when a world starts, and a call past that throws.

The two keys of the constructor are the shape's name and its one-line description.

## Parameters

A parameter is one value the shape is tuned with. The world stores it per dimension under its id, the page draws it from its parts, and `spansIn` reads it with `getValue`. The parts and their translation keys work as [a feature's controls](features.md#the-options-controls) do.

`withNewWorldValue(dimension -> value)` starts a new world at a value of its own per dimension, a smaller width in the Nether for one. `withPresetWidth((value, widthChunks) -> value)` lets the screen's size presets write the parameter. Without a parameter that takes presets, the preset row is off while the Overworld is on your shape.

## What `spansIn` is given

`ShapeDimensionV2` answers the dimension the shape was chosen for, its parameter values, the Overworld's spans, and the coordinate scales of the Overworld and of this dimension. The Overworld's spans are `null` for the Overworld itself and under an Overworld that loops nothing.

Return `LoopSpansV2.NONE` to leave the dimension as the game makes it.

A dimension set to **From Overworld** is not asked. It takes the Overworld's spans at its own scale, unless the shape overrides `derivedSpans`, which is marked `@ExperimentalV2`.

## The spans

`LoopSpansV2` says which horizontal axes loop and over what span of chunks. Spans are half-open, so `minChunk` is the first chunk inside the world and `maxChunk` the first one past it.

```java
LoopSpansV2.ofWidth(24);                        // both axes, 24 chunks, centered on the origin
LoopSpansV2.ofWidths(24, 48);                   // X over 24 chunks, Z over 48, each centered
LoopSpansV2.ofWidth(Direction.Axis.Z, 24);      // Z alone, X runs to the vanilla world border
LoopSpansV2.of(Direction.Axis.X, -12, 12);      // an explicit span
LoopSpansV2.of(Direction.Axis.X, -12, 12)
        .and(Direction.Axis.Z, -24, 24);        // explicit spans on both axes
LoopSpansV2.ofWidth(24).withSkew(6);            // crossing the Z seam also moves X by 6 chunks
```

Both axes give a torus, one gives a cylinder, neither gives an ordinary world. `withSkew` needs both axes to loop. `scaledDown(scale)` divides every looping axis and the skew and re-centers, and throws where the scale does not divide the skew.

Every looping axis must be at least `LoopSpansV2.getMinWidthChunks()` chunks wide, 16 chunks (256 blocks), and 192 chunks (3072 blocks) in the End. Spans outside that show an error on the page and keep **Done** off until the player changes the parameter behind them.

## Features on your shape

A feature works on your shape once it is attached to the shape's id, as [Adding a feature to a shape](features.md#registering-it) shows.
