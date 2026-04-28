# ComputerCraft Peripherals

Create: Propulsion registers the following peripherals when CC:Tweaked is installed.

## Thruster (`propulsion_thruster`)

- `getObstruction() -> number`
- `setPower(power)` where `power` is normalized `0.0 .. 1.0`
- `getPower() -> number`
- `getFuelName() -> string`
- `getFuelThrustMultiplier() -> number`
- `getFuelConsumptionMultiplier() -> number`
- `tanks() -> table`
- `pushFluid(toName[, limit[, fluidName]]) -> number`
- `pullFluid(fromName[, limit[, fluidName]]) -> number`

When attached, control mode switches to peripheral control.  
When detached, power is reset to `0` and control mode returns to normal.

## Creative Thruster (`creative_thruster`)

- `getObstruction() -> number`
- `setPower(power)` where `power` is normalized `0.0 .. 1.0`
- `getPower() -> number`
- `setThrustConfig(percent)`
- `getThrustConfig() -> number`
- `getTargetThrustKN() -> number`

Also switches control mode on attach/detach like normal thrusters.

## Stirling Engine (`stirling_engine`)

- `getRpm() -> number`
- `setSpeed(targetSpeed)`
  - Internally clamped to supported scroll levels.
- `setActive(active)`

## Redstone Transmission (`redstone_transmission`)

- `getTransmissionMode() -> string` (`direct` or `incremental`)
- `setTransmissionMode(mode)`
- `getShiftLevel() -> number`
- `setShiftLevel(level)`

`setTransmissionMode` throws an error for invalid values.
