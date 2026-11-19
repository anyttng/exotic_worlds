# Declaring a World Shape

Part of [Exotic Worlds from Another Mod](../modding.md).

A shape is one class. It has an id and answers the spans of each dimension the player puts it on. The player picks it with the **Shape** button of the **World Shape Settings** screen, on any dimension.

```java
public final class BandShape implements DimensionShape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("your_mod", "band");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public LoopSpans spans(DimensionContext context) {
        return LoopSpans.ofWidth(Direction.Axis.X, context.options().get(BandWidth.OPTION));
    }
}
```

`new BandShape().register()` registers it. Call that from your mod's initializer: the registry closes at `MinecraftServer.runServer`, before the levels load, and a `register` past that throws.

The shape's name and its one-line description come from `gui.<namespace>.world_shape.<path>` and that key plus `.hint`; for the shape above, `gui.your_mod.world_shape.band` and `gui.your_mod.world_shape.band.hint`.

A shape has no screen and no settings of its own. What the player tunes are world options that name it, declared below.

## What `spans` is given

`DimensionContext` carries the dimension the shape was chosen for, the options chosen on that dimension's page, the Overworld's spans, and the coordinate scales of the Overworld and of this dimension. The Overworld's spans are `null` for the Overworld itself and under an Overworld that loops nothing.

Return `LoopSpans.NONE` to leave the dimension as the game makes it.

A world stores the spans and the options, never the shape. When the player re-creates the world, a dimension shows your shape again if `spans` returns the stored spans for the stored options. So make `spans` depend on its context alone.

A dimension set to **From Overworld** is not asked: it takes the Overworld's spans at its own size.

## The geometry it declares

`LoopSpans` is the whole of it: which horizontal axes loop and over what span of chunks. Spans are half-open: `minChunk` is the first chunk inside the world, `maxChunk` the first one past it.

```java
LoopSpans.ofWidth(24);                        // both axes, 24 chunks, centered on the origin
LoopSpans.ofWidths(24, 48);                   // X over 24 chunks, Z over 48, each centered
LoopSpans.ofWidth(Direction.Axis.Z, 24);      // Z alone; X runs to the vanilla world border
LoopSpans.of(Direction.Axis.X, -12, 12);      // an explicit span
LoopSpans.of(Direction.Axis.X, -12, 12)
        .and(Direction.Axis.Z, -24, 24);      // explicit spans on both axes
LoopSpans.ofWidth(24).withSkew(6);            // crossing the Z seam also moves X by 6 chunks
```

Which flat surface the world ends up being is read off the spans, never declared: both axes give a torus, one gives a cylinder, neither gives an ordinary world. `withSkew` needs both axes to loop, and makes a torus whose axes do not fold independently ([The two capability flags](../modding.md#the-two-capability-flags)); `skewChunks()` reads it back, brought into the range from minus half the X width to plus half. `scaledDown(scale)` divides every looping axis and the skew, and re-centers. It throws where the scale does not divide the skew.

Every looping axis must be at least `LoopSpans.minChunkWidth()` chunks wide, 16 chunks (256 blocks), and 192 chunks (3072 blocks) in the End. Spans outside that show an error on the page and keep **Done** off until the player changes the option behind them.

## Size presets

The preset row sets a size, and a shape has no size field, so a preset reaches it through its options.

```java
@Override
public boolean takesPresets() {
    return true;
}

@Override
public GenerationOptions withPreset(int chunkWidth, GenerationOptions options) {
    return options.with(BandWidth.OPTION, chunkWidth);
}
```

Without `takesPresets`, the preset row is off while the Overworld is on your shape. The row finds the current preset by calling `withPreset` again, so return the options unchanged when they already hold that size.

## Declaring a world option

A world option is a value the player picks, stored in the world and read back at load: the codec key, where it sits among the other options, its codec and its default.

```java
public static final WorldOption<Integer> OPTION =
        new WorldOption<>("band_width", 100, Codec.intRange(16, 512), 64);

WorldOptions.register(OPTION);
```

The key is written into the world's generators and must be unique across every mod. `position` orders the option against the others on the page; equal positions fall back to the key.

`GenerationOptions` is the value set a dimension carries: `get(option)` to read, `with(option, value)` to write, `GenerationOptions.DEFAULT` to start from. Only a value that differs from its option's default is written to the world file.

## Showing the option

A `DimensionOption` says where the option is offered and which controls the screen draws for it. You bring no widget.

```java
DimensionOption.of(BandWidth.OPTION, "gui.your_mod.band_width", List.of(OptionPart.number(16, 512)))
        .forShapes(List.of(BandShape.ID))
        .register();
```

`forShapes` and `forDimensions` narrow it; without them the option is offered for every shape on every dimension. `BuiltInShapes.TORUS` and `BuiltInShapes.CYLINDER` name the shapes Exotic Worlds ships. A dimension with no shape stores no options, so nothing is offered there.

A part is one control: `OptionPart.toggle()`, `OptionPart.choice(values, valueKey)` or `OptionPart.number(min, max)`. An option whose value has several pieces lists one part per piece, built with the record constructors `OptionPart.Toggle`, `OptionPart.Choice` and `OptionPart.WholeNumber`. Each takes how to read its piece from the value, how to write it back, and when to show it.

Every text is a translation key under the caption key:

| Key | Text |
| --- | --- |
| `<caption>` | the option's name |
| `<caption>.<part key>` | the name of a part that has a key |
| `<part caption>.<value key>` | the name of a choice value |
| `<part caption>.hint.<value key>` | one line about that value; `on` and `off` for a toggle |
| `<part caption>.hint.intro`, `<part caption>.hint.note` | optional lines above and below the values |

`DimensionOptionLines.register(option, line)` adds one line of text under the option's controls, worked out from the value and the dimension's spans. Register it from the client side only; an empty component shows no line.

## What the v1 declarations no longer do

`ShapeModule`, `ShapeCustomizers`, `WorldOptionControl`, `WorldOptionContext` and `WorldOptionControls` are deprecated. They still compile and load, and nothing shows them: a `ShapeModule` shape is in no list, no button opens a `ShapeCustomizers` screen, and no screen builds a `WorldOptionControls` control. Move a shape to `DimensionShape` and an option's control to `DimensionOption`.

`WorldOption`, `WorldOptions`, `GenerationOptions`, `LoopSpans` and `ShapeDimensions` are unchanged.
