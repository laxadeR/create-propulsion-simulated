# ComputerCraft Peripherals

Create: Propulsion registers these peripherals when CC:Tweaked is installed.

## Thruster (`propulsion_thruster`)

- `getObstruction() -> number`
- `setPower(redstonePower)` where `redstonePower` is `0 .. 15`
- `setPowerNormalized(power)` where `power` is `0.0 .. 1.0`
- `getPower() -> number` returns normalized throttle `0.0 .. 1.0`
- `getCurrentThrustPN() -> number`
- `getCurrentThrustKN() -> number`
- `getDisplayedThrustPN() -> number`
- `getDisplayedThrustKN() -> number`
- `getAirflowMs() -> number`
- `getFuelAmountMb() -> number`
- `getFuelCapacityMb() -> number`
- `tanks() -> table`
- `pushFluid(toName[, limit[, fluidName]]) -> number`
- `pullFluid(fromName[, limit[, fluidName]]) -> number`

Attach/detach behavior:
- On attach: thruster switches to peripheral control mode.
- On detach: control returns to normal mode and peripheral power is reset.

Notes:
- Ion thrusters expose the same peripheral type but fluid methods throw because ion thrusters have no fluid tank.

## Creative Thruster (`creative_thruster`)

- `getObstruction() -> number`
- `setPower(redstonePower)` where `redstonePower` is `0 .. 15`
- `setPowerNormalized(power)` where `power` is `0.0 .. 1.0`
- `getPower() -> number` returns normalized throttle `0.0 .. 1.0`
- `setThrustConfig(percent)`
- `getThrustConfig() -> number`
- `getTargetThrustPN() -> number`
- `getTargetThrustKN() -> number`
- `getCurrentThrustPN() -> number`
- `getCurrentThrustKN() -> number`
- `getDisplayedThrustPN() -> number`
- `getDisplayedThrustKN() -> number`
- `getAirflowMs() -> number`

Attach/detach behavior:
- On attach: thruster switches to peripheral control mode.
- On detach: control returns to normal mode and peripheral power is reset.

## Vector Thruster (`vector_thruster`)

This type is used by both normal vector thrusters and liquid vector thrusters.

- `getVectorX() -> number`
- `getVectorY() -> number`
- `getTargetVectorX() -> number`
- `getTargetVectorY() -> number`
- `setVectorX(x)` where `x` is clamped to `-1.0 .. 1.0`
- `setVectorY(y)` where `y` is clamped to `-1.0 .. 1.0`
- `setVector(x, y)` where each value is clamped to `-1.0 .. 1.0`
- `setThrust(power)` where `power` is `0 .. 15`
- `setThrustNormalized(power)` where `power` is `0.0 .. 1.0`
- `setPower(power)` alias of `setThrust`
- `setPowerNormalized(power)` alias of `setThrustNormalized`
- `getThrust() -> number` returns `0 .. 15`
- `getPower() -> number` returns normalized throttle `0.0 .. 1.0`
- `setThrustOutput(thrustOutputPn)`

Error behavior:
- `setThrustOutput` only works on creative vector thrusters.
- On non-creative vector thrusters it throws an error.

Attach/detach behavior:
- On attach: thruster switches to peripheral control mode.
- On detach: control returns to normal mode and peripheral power is reset.

## Stirling Engine (`stirling_engine`)

- `getRpm() -> number`
- `setSpeed(targetSpeed)` (internally clamped to supported scroll levels)
- `setActive(active)`

## Redstone Transmission (`redstone_transmission`)

- `getTransmissionMode() -> string` (`direct` or `incremental`)
- `setTransmissionMode(mode)`
- `getShiftLevel() -> number`
- `setShiftLevel(level)`

Error behavior:
- `setTransmissionMode` throws for invalid mode values.

## Tilt Adapter (`tilt_adapter`)

- `getLeftSignal() -> number`
- `getRightSignal() -> number`
- `setTargetAngle(angle)`

When a computer is attached, tilt target is driven from the peripheral target angle.

## Coral Generator (`coral_generator`)

- `getCoralAmountMb() -> number`
- `getCoralCapacityMb() -> number`
- `getEnergyAmountFe() -> number`
- `getEnergyCapacityFe() -> number`
