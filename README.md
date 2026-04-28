<p align="center">
  <img width="300" height="300" src="https://github.com/SergeyFeduk/Create-Propulsion/blob/main/src/main/resources/icon.png">
</p>

<h1 align="center">Create: Propulsion - Sable Edition</h1>

<p align="left">
  A Sable / Create Aeronautics focused fork of Create: Propulsion for NeoForge 1.21.1.
  Original here: [Create: Propulsion](https://github.com/SergeyFeduk/Create-Propulsion)
</p>

# What Was Changed In This Fork

## Sable and Simulated Support
- Remade the mod to work with Simulated/Sable's system.
- Thruster and related systems updated to actually provide thrust, lift.

## Fuel and heat tuning
- Added data-driven thruster fuel compatibility for:
  - Create: Diesel Generators (`createdieselgenerators`)
  - TFMG (`tfmg`)
  - Immersive Engineering (`immersiveengineering`)
  - Immersive Petroleum (`immersivepetroleum`)
  - Mekanism (`mekanism`)
  - Mekanism: Generators (`mekanismgenerators`)
  - Northstar Redux (`northstar`)
  - Stellaris (`stellaris`)
- Added configurable fluid efficiency multipliers that affect both:
  - Thrusters
  - Liquid Burners
- Default behavior includes baseline fuels at ~100% and lava at 75%.

## Gameplay polish
- Thruster goggles now reflect fuel efficiency correctly (not just obstruction).
- Thruster hazard behavior is tied to the damage setting (damage + ignition both toggle together).

# Main Features

## Methods of propulsion
- Thruster: Provides thrust to vehicles. Controllable by redstone.
- Creative Thruster: Produces a variable amount of thrust based on redstone signal level and its configuration. Particle type is configurable.

## Redstone components
- Redstone Transmission: A block that allows precise control of RPM with redstone signals. Has two modes: Direct and Incremental. 
## Heat system
- Solid Burner: Produces a moderate amount of heat by burning solid fuels.
- Liquid Burner: Produces a large amount of heat by burning liquid fuels. Allows liquid to pass through it.
- Stirling engine: Generates RPM when heat is provided from below. 

# Notes

- Many systems are configurable in server/client config.
- Fuel support is data-driven (`data/createpropulsion/thruster_fuels`) and can be extended with datapacks.

# Wiki

- [KubeJS API](wiki/KubeJS-API.md)
- [ComputerCraft Peripherals](wiki/ComputerCraft-Peripherals.md)
- [Datapack example usage](wiki/Datapack-Example-Usage.md)
