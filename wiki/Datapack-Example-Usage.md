# Datapack Example Usage

Fuel definitions are data-driven under:

`data/<namespace>/thruster_fuels/<path>.json`

These fuels are used by both Thrusters and Liquid Burners.

## JSON schema

```json
{
  "fluid": "namespace:fluid_name",
  "thrust_multiplier": 1.0,
  "consumption_multiplier": 1.0,
  "particle": "plume",
  "override_textures": [
    "createpropulsion:plume_0",
    "createpropulsion:plume_1"
  ],
  "override_color": 6750207,
  "use_fluid_color": false,
  "required_mod": "optional_mod_id"
}
```

## Fields

- `fluid` (required): fluid id
- `thrust_multiplier` (required): thrust scaling
- `consumption_multiplier` (required): fuel usage scaling
- `particle` (optional): `plume`, `plasma`, or `none` (default: `plume`)
- `override_textures` (optional): list of particle texture ids from the particle atlas
- `override_color` (optional): RGB color int, e.g. `0x66CCFF`
- `use_fluid_color` (optional): use the fluid's own tint color for particles
- `required_mod` (optional): only loads if that mod is present

## Example datapack files

Path:

`data/my_pack/thruster_fuels/kerosene_plasma.json`

Contents:

```json
{
  "fluid": "tfmg:kerosene",
  "thrust_multiplier": 1.15,
  "consumption_multiplier": 0.9,
  "particle": "plasma",
  "override_textures": [
    "createpropulsion:plasma_0",
    "createpropulsion:plasma_1",
    "createpropulsion:plasma_2"
  ],
  "use_fluid_color": true,
  "required_mod": "tfmg"
}
```

`data/my_pack/thruster_fuels/water_green_plume.json`

```json
{
  "fluid": "minecraft:water",
  "thrust_multiplier": 0.05,
  "consumption_multiplier": 1.0,
  "particle": "plume",
  "override_color": 3407718
}
```

Notes:

- `override_color` uses a decimal RGB integer (`3407718 == 0x33FF66`).
- If `use_fluid_color` is `true`, that fluid tint is used dynamically.
- If both are present, `use_fluid_color` takes priority over `override_color`.

## Reload

- Use `/reload` to re-read datapack fuel files.
- On reload, fuel data is synced to connected clients automatically.
