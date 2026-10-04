# KubeJS and datapacks

NTM: DE works with [KubeJS](https://kubejs.com) 2101.7+ (NeoForge 1.21.1) from **1.0.0-beta.5**.
Everything here is optional: without KubeJS the mod behaves exactly the same.

All NTM recipes, machine recipes included, are ordinary datapack recipes. That gives you two layers:

- **Works with any KubeJS** (and plain datapacks): remove recipes by id or type, add recipes as raw JSON,
  change tags, override NTM data files.
- **NTM's KubeJS plugin** (ships inside the mod): typed builders for machine recipes,
  `replaceInput` / `replaceOutput`, and input/output filters that understand NTM's items and fluids.

Server scripts go into `kubejs/server_scripts/*.js`. `/reload` applies changes without restarting.

## Finding recipe ids and types

Machine recipe ids look like `hbm:<machine>/<name>`, for example `hbm:assembly_machine/ass.acidizer`,
and their type is `hbm:<machine>` (`hbm:assembly_machine`, `hbm:shredder`, ...). Crafting table recipes
are `hbm:<item>` with the vanilla types. The mod jar is a zip: every recipe is a file under
`data/hbm/recipe/`, and its path is its id (`data/hbm/recipe/assembly_machine/ass.acidizer.json` is
`hbm:assembly_machine/ass.acidizer`). Copying such a file is also the easiest way to start a new recipe.

## Removing recipes

```js
ServerEvents.recipes(event => {
    // one recipe
    event.remove({ id: 'hbm:assembly_machine/ass.acidizer' })
    // every recipe of one machine
    event.remove({ type: 'hbm:rock_mill' })
    // by output or input inside one machine (needs the NTM plugin for machine types)
    event.remove({ type: 'hbm:crystallizer', output: 'hbm:nugget_arsenic' })
    event.remove({ type: 'hbm:shredder', input: 'minecraft:cobblestone' })
    // crafting table recipes of NTM items work like any other mod's
    event.remove({ output: 'hbm:anvil_iron', type: 'minecraft:crafting_shaped' })
})
```

## Adding machine recipes

### Typed builders

`event.recipes.hbm.<machine>(outputs, inputs[, duration[, power]])`, plus these functions:

| Function | Meaning |
|---|---|
| `.inputFluids([...])` | fluids consumed |
| `.outputFluids([...])` | fluids produced |
| `.duration(ticks)` | processing time in ticks |
| `.power(he)` | energy use (HE per tick) |
| `.merge({...})` | any other field of the recipe JSON (see [machine-specific fields](#machine-specific-fields)) |
| `.id('kubejs:...')` | recipe id (recommended, otherwise KubeJS generates one) |

```js
ServerEvents.recipes(event => {
    // 4 steel ingots (any item in the tag) + a basic circuit -> emerald, 100 ticks, 200 HE/t
    event.recipes.hbm.assembly_machine('minecraft:emerald', ['4x #c:ingots/steel', 'hbm:circuit_basic'], 100, 200)
        .id('kubejs:ass_emerald')

    // chemical plant with fluids in and out
    event.recipes.hbm.chemical_plant(['hbm:ingot_bakelite'], ['hbm:powder_coal'])
        .inputFluids([Fluid.of('hbm:sulfuric_acid', 500)])
        .outputFluids([{ type: 'hbm:hydrogen', amount: 250 }])
        .duration(60)
        .power(50)
        .id('kubejs:chem_bakelite')

    // centrifuge: a chance output and a counted output
    event.recipes.hbm.centrifuge([{ id: 'minecraft:gold_nugget', chance: 0.3 }, '2x minecraft:iron_nugget'], ['minecraft:raw_iron'])
        .duration(100)
        .power(100)
        .id('kubejs:centrifuge_raw_iron')

    // a field only the press has: the stamp
    event.recipes.hbm.press('hbm:plate_iron', ['9x minecraft:iron_nugget'])
        .merge({ stamp: 'plate' })
        .id('kubejs:press_nugget_plate')
})
```

**Inputs** (items): `'hbm:motor'`, `'4x hbm:motor'`, `'#c:ingots/steel'`, `'4x #c:ingots/steel'`.

**Outputs** (items):
- `'hbm:plate_iron'` or `'2x hbm:plate_iron'`;
- with a chance: `{ id: 'minecraft:flint', count: 2, chance: 0.5 }` (`item` works in place of `id`);
- a weighted pool, the same JSON NTM uses: `[{ data: { id: 'minecraft:gravel' }, weight: 95 }, { data: { id: 'hbm:powder_quartz' }, weight: 5 }]`.

**Fluids**: `Fluid.of('hbm:sulfuric_acid', 1000)`, a plain fluid id (1000 mB), or NTM's own form
`{ type: 'hbm:sulfuric_acid', amount: 1000, pressure: 1 }` when the recipe needs pressure.

Machines with builders: `anvil_construction`, `anvil_smithing`, `arc_furnace`, `arc_welder`,
`assembly_machine`, `blast_furnace`, `breeder`, `catalytic_reformer`, `centrifuge`, `chemical_plant`,
`coker`, `combination_oven`, `compressor`, `cracking_tower`, `crystallizer`, `custom_machine`,
`cyclotron`, `electrolyser_fluid`, `electrolyser_metal`, `exposure_chamber`, `fluid_breeder`,
`fraction_tower`, `fuel_pool`, `fusion`, `hydrotreater`, `lemegeton`, `liquefaction`, `magic`, `mixer`,
`outgasser`, `particle_accelerator`, `plasma_forge`, `precision_assembler`, `press`, `purex`,
`pyro_oven`, `radiolysis`, `refinery`, `rock_mill`, `shredder`, `silex`, `soldering`, `solidification`,
`space_assembler`, `supercomputer`, `vacuum_refinery`.

### Raw JSON (any machine)

`event.custom` takes the recipe exactly as it looks in NTM's data files
(`data/hbm/recipe/<machine>/*.json` inside the mod jar). This also covers the machines that have no
builder yet (rotary furnace, crucible, gas centrifuge, ammo press, ...).

```js
ServerEvents.recipes(event => {
    event.custom({
        type: 'hbm:shredder',
        input_items: [{ ingredient: 'minecraft:amethyst_shard' }],
        output_items: [{ id: 'hbm:powder_coal', count: 2 }]
    }).id('kubejs:shredder_amethyst')
})
```

### Machine-specific fields

Set them with `.merge({...})` on a builder (or directly in `event.custom`). Existing recipes keep theirs
when you edit them with `replaceInput` / `replaceOutput`.

| Machine | Field | Values seen in NTM's own recipes |
|---|---|---|
| `press` | `stamp` | `flat`, `plate`, `wire`, `circuit`, `c9`, `c50`, `printing1`…`printing8` |
| `silex` | `laser_strength` | `ir`, `visible`, `uv`, `gamma` |
| `silex` | `fluid_consumed`, `fluid_produced` | mB, e.g. `100`, `900` |
| `crystallizer` | `productivity` | `0.05`…`0.3` |
| `anvil_construction` | `tier`, `overlay` | tier e.g. `1`…`5`; overlay `construction`, `recycling`, `smithing` |
| `anvil_smithing` | `kind`, `shape`, `tier` | kind `hot`, `mold`, `poison`, `rename`; shape `ingots`, `plates`, `billets`, ... |
| `breeder` | `flux` | e.g. `100`…`2000` |
| `cyclotron` | `amat` | antimatter use, e.g. `10`…`1000` |
| `plasma_forge` | `ignition_temp` | e.g. `500000`…`50000000` |
| `particle_accelerator` | `momentum` | e.g. `100`…`12500` |
| `fusion` | `ignition_temp`, `output_temp`, `neutron_flux`, `red`, `green`, `blue` | see NTM's fusion recipes |
| `soldering` | `pcb`, `solder` | lists of inputs in the same form as `input_items` |
| `arc_furnace`, `electrolyser_metal` | `output_materials`, `output_material(_2)` | `{ material: 'iron', amount: 144 }` |

When in doubt, open a recipe of the same machine in the mod jar and copy its fields.

## Editing existing recipes

```js
ServerEvents.recipes(event => {
    // items, inside one machine type or anywhere
    event.replaceInput({ type: 'hbm:assembly_machine' }, 'hbm:motor', 'minecraft:stick')
    event.replaceOutput({ type: 'hbm:shredder' }, 'minecraft:gravel', 'minecraft:sand')
    // fluids: the amount and pressure of the original stay
    event.replaceInput({ type: 'hbm:chemical_plant' }, Fluid.of('hbm:petroleum'), Fluid.of('hbm:oil'))
    // crafting table recipes of NTM items, as usual
    event.replaceInput({ output: 'hbm:anvil_lead' }, '#c:ingots/lead', 'minecraft:copper_ingot')
})
```

Replaced outputs keep their count unless you give one (`'3x minecraft:sand'`), and keep their chance.

## Tags

NTM recipes ask for common tags a lot (`#c:ingots/steel`, `#c:plates/copper`, ...). Adding an item to
such a tag makes every NTM recipe accept it:

```js
ServerEvents.tags('item', event => {
    event.add('c:ingots/steel', 'mymod:steel_ingot')
})
```

## Data files (datapacks)

Files under `kubejs/data/` work like a datapack, and a file with the same path as one in the mod jar
replaces it. Useful NTM folders under `data/hbm/`:

| Folder | What it controls |
|---|---|
| `recipe/` | all recipes (`recipe/<machine>/` for machines) |
| `fluid_property/` | fluid temperature, flammability, corrosion, color of fluid containers, ... |
| `hbm/hazard/`, `hbm/rad_source/` | radiation and other hazards of items |
| `hbm/machine_config/`, `hbm/explosion_config/`, `hbm/mob_config/` | machine, explosion and mob settings |
| `hbm/custom_machine/` | data-defined machines |

Example: `kubejs/data/hbm/fluid_property/sulfuric_acid.json` with `"temperature": 90` makes sulfuric acid
90 °C. The color in `fluid_property` tints fluid containers (canisters, cells, tanks, buckets); the fluid
square in tooltips and JEI and the tanks in machine GUIs use per-fluid textures instead, which you change
with a resource pack (or `kubejs/assets/hbm/textures/gui/fluids/<fluid>.png`).

## Limitations

- `replaceInput` / `replaceOutput` don't touch recipes that scripts add themselves; write those the way
  you want them in the first place.
- Crafting table recipes that take "any container with N mB of a fluid" (C4, solid fuel, ...) can be
  removed or replaced as a whole, but not edited piece by piece yet.
- Machines without a builder (see [Raw JSON](#raw-json-any-machine)) need `event.custom`.
- Pressure of a new fluid only comes from the `{ type, amount, pressure }` form.

Something doesn't work as described? Please open an
[issue](https://github.com/SecretAgent12/NTM-Decentralized-Edition/issues) with the script and
`logs/kubejs/server.log`.
