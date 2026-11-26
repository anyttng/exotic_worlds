# Adding a Feature to a Shape

Part of [Exotic Worlds from Another Mod](../modding.md).

A feature changes how a world of some shape generates, and may give the player one option to tune it with.

## The class

Extend `ToggleFeatureV2` for an on/off switch, `OptionFeatureV2<T>` for any other value, or `FeatureV2` for a feature with no option. Then implement the interface of each moment the feature acts at.

```java
public final class BandBeacons extends ToggleFeatureV2 implements StructureStartsMomentV2 {
    public static final BandBeacons FEATURE = new BandBeacons();

    private BandBeacons() {
        super(Identifier.fromNamespaceAndPath("your_mod", "band_beacons"), "gui.your_mod.band_beacons");
    }

    @Override
    public List<StructureStartsV2.Added> atStructureStarts(StructureStartsV2 starts) {
        if (!starts.getStructureSet().is(BEACON_SET)) {
            return List.of();
        }

        Holder<Structure> beacon = starts.getStructureSet().value().structures().get(0).structure();
        ChunkPos center = new ChunkPos(0, 0);
        return starts.canStart(beacon, center) ? List.of(new StructureStartsV2.Added(beacon, center)) : List.of();
    }
}
```

A moment is called only in a dimension whose shape carries the feature, and only while the feature is on, so the method checks neither. An `OptionFeatureV2` says which values count as on by overriding `isOn(value)`.

## Registering it

```java
FeaturesV2.register(BandBeacons.FEATURE);
ShapesV2.attach(ShapesV2.TORUS, BandBeacons.FEATURE, DimensionScopeV2.of(List.of(LevelStem.OVERWORLD)));
```

Call both from your mod's initializer. Features run within one moment in the order of the `register` calls. `attach` puts the feature on one shape in the dimensions of its scope, and the shape's page lists options in the order of the `attach` calls. Exotic Worlds' own features and options come first, then each mod's in the alphabetical order of namespaces. `DimensionScopeV2.ALL` covers every dimension, a mod's own included. The shape may be `ShapesV2.TORUS`, `ShapesV2.CYLINDER`, [a shape of your own](shapes.md) or another mod's, by its id.

## The moments

| Interface | Called | What the feature does |
| --- | --- | --- |
| `RandomStateMomentV2` | once per dimension as its noise state is built, before any chunk generates | samples the looped terrain, sets a noise offset |
| `NoiseMomentV2` | for each terrain noise as it is compiled onto the loop | makes the noise finer, or marks it as one it shifts |
| `StructureStartsMomentV2` | once per structure set, before the first structure is generated or searched for | returns starts to add beside the set's own |
| `SpawnMomentV2` | as a new world looks for its spawn | returns the chunk the search starts from |

Beside its own members, the object each method takes answers `getGeometry()`, `getDimension()` and `getValue(feature)`, the value of your option in that dimension.

To shift a noise, answer `true` from `isOffsetting(noise)` for it and set the amount with `setNoiseOffset(offset)` in `atRandomState`.

In `atStructureStarts`, `getPicks()` lists the chunks the set's own placement picks, and `canStart(structure, chunk)` runs the game's own test for a chunk. Check the set before calling `getPicks()`, which runs a structure check per picked chunk. A start outside the world, on a chunk the placement already picked, or of a structure the set does not hold is dropped with a warning.

## The option's controls

The last constructor argument of `OptionFeatureV2` lists the controls that edit the value, top to bottom. A part is one control, built with `OptionPartV2.toggle()`, `OptionPartV2.choice(values, valueKey)` or `OptionPartV2.number(min, max)`. A value with several pieces takes one part per piece, built with the record constructors `Toggle`, `Choice`, `WholeNumber` and `Row`, each given how to read its piece, how to write it back and when it is enabled.

Every text is a translation key under the caption key.

| Key | Text |
| --- | --- |
| `<caption>` | the option's name |
| `<caption>.<part key>` | the name of a part that has a key |
| `<part caption>.<value key>` | the name of a choice value |
| `<part caption>.hint.<value key>` | one line about that value, where a toggle's values are `on` and `off` |
| `<part caption>.hint.intro`, `<part caption>.hint.note` | optional lines above and below the values |

## A line under the controls

```java
FeatureViewsV2.registerLine(BandBeacons.FEATURE, (isOn, spans) -> beaconCountLine(spans));
```

Call it from your client entry point. The line is asked every frame, so keep it cheap, and an empty component hides it.

## The experimental parts

Members marked `@ExperimentalV2` may change between releases. They are the chunk moments (`LevelMomentV2`, `CarvedChunkMomentV2`, `LitChunkMomentV2`, `ChunkDataMomentV2`).
