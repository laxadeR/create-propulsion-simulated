# KubeJS API

Create: Propulsion exposes `ThrusterFuelManager` as a global in KubeJS scripts.

This API registers fuels for both:
- Thrusters
- Liquid Burners

Both systems share the same fuel registry.

## Global

- `ThrusterFuelManager`

## Methods

- `registerScriptedFuel(String fluidId, Map<String, Object> settings)`
  - Single supported KubeJS registration API.
  - Supports keys:
    - `thrustMultiplier` / `thrust_multiplier`
    - `consumptionMultiplier` / `consumption_multiplier`
    - `particle`
    - `overrideTextures` / `override_textures`
    - `overrideColor` / `override_color`
    - `useFluidColor` / `use_fluid_color`
  - Returns `true` when registration succeeds.
- `overrideFuel(String fluidIdToOverride, Map<String, Object> settings)`
  - Explicit alias for overriding an existing fuel entry.
- `removeFuel(String fuelIdToRemove)`
  - Removes/disables a fuel id, including datapack/default/tag-based matches.

## Particle names

- `plume` (default)
- `plasma`
- `none` (disables thruster exhaust particles for that fuel)

## Example (`kubejs/server_scripts/propulsion_fuels.js`)

```js
ServerEvents.loaded(event => {
  // Prevent duplicate registrations on script reloads.
  ThrusterFuelManager.clearScriptedFuels()

  ThrusterFuelManager.registerScriptedFuel('minecraft:lava', {
    thrustMultiplier: 1.0,
    consumptionMultiplier: 1.0,
    particle: 'plume'
  })

  ThrusterFuelManager.registerScriptedFuel('createpropulsion:turpentine', {
    thrustMultiplier: 1.2,
    consumptionMultiplier: 0.8,
    particle: 'plasma'
  })

  // Example with custom textures + RGB tint.
  ThrusterFuelManager.registerScriptedFuel('minecraft:water', {
    thrustMultiplier: 0.05,
    consumptionMultiplier: 1.0,
    particle: 'plume',
    overrideTextures: ['createpropulsion:plume_0', 'createpropulsion:plume_1'],
    useFluidColor: true
  })

  // Override existing fuel behavior explicitly.
  ThrusterFuelManager.overrideFuel('minecraft:lava', {
    thrustMultiplier: 0.8,
    consumptionMultiplier: 1.25,
    particle: 'plasma'
  })

  // Remove a fuel entirely.
  ThrusterFuelManager.removeFuel('minecraft:water')
})
```

## Notes

- Invalid fluid ids are ignored and logged.
- `particleName` falls back to `plume` if unknown.
- `overrideTextureIds` accepts resource ids in the particle atlas (e.g. `modid:plume_0`).
- `overrideColor` is optional and clamped to `0x000000..0xFFFFFF`.
- `useFluidColor` tints particles using the actual fluid color from the fluid stack.
