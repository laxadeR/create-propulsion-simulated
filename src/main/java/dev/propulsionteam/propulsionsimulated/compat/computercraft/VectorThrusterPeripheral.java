package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dev.propulsionteam.propulsionsimulated.content.thruster.creative_vector_thruster.CreativeVectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity.ControlMode;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class VectorThrusterPeripheral extends SyncedPeripheral<VectorThrusterBlockEntity> {
    public VectorThrusterPeripheral(VectorThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "vector_thruster";
    }

    // --- Vector control (float -1..1 coordinates) --------------------------

    @LuaFunction
    public final double getVectorX() {
        return blockEntity.getCurrentVectorX();
    }

    @LuaFunction
    public final double getVectorY() {
        return blockEntity.getCurrentVectorY();
    }

    @LuaFunction
    public final double getTargetVectorX() {
        return blockEntity.getTargetVectorX();
    }

    @LuaFunction
    public final double getTargetVectorY() {
        return blockEntity.getTargetVectorY();
    }

    @LuaFunction(mainThread = true)
    public final void setVectorX(double x) {
        blockEntity.setVectorCoordinates((float) Mth.clamp(x, -1.0, 1.0), blockEntity.getTargetVectorY());
    }

    @LuaFunction(mainThread = true)
    public final void setVectorY(double y) {
        blockEntity.setVectorCoordinates(blockEntity.getTargetVectorX(), (float) Mth.clamp(y, -1.0, 1.0));
    }

    @LuaFunction(mainThread = true)
    public final void setVector(double x, double y) {
        blockEntity.setVectorCoordinates((float) Mth.clamp(x, -1.0, 1.0), (float) Mth.clamp(y, -1.0, 1.0));
    }

    // --- Throttle ----------------------------------------------------------

    @LuaFunction(mainThread = true)
    public final void setThrust(int power) {
        blockEntity.setRedstonePower(Mth.clamp(power, 0, 15));
    }

    @LuaFunction(mainThread = true)
    public final void setThrustNormalized(double power) {
        int redstonePower = Mth.floor(Mth.clamp(power, 0.0d, 1.0d) * 15.0d + 1.0e-6d);
        blockEntity.setRedstonePower(redstonePower);
    }

    @LuaFunction(mainThread = true)
    public final void setPower(int power) {
        setThrust(power);
    }

    @LuaFunction(mainThread = true)
    public final void setPowerNormalized(double power) {
        setThrustNormalized(power);
    }

    @LuaFunction
    public final int getThrust() {
        return blockEntity.getLegacyPowerInt();
    }

    @LuaFunction
    public final double getPower() {
        return blockEntity.getPower();
    }

    @LuaFunction(mainThread = true)
    public final void setThrustOutput(double thrustOutputPn) throws LuaException {
        if (blockEntity instanceof CreativeVectorThrusterBlockEntity creativeVectorThruster) {
            creativeVectorThruster.setThrustOutput((float) Math.max(0.0d, thrustOutputPn));
            return;
        }
        throw new LuaException("setThrustOutput is only available on creative vector thrusters");
    }

    @Override
    public void attach(@NotNull IComputerAccess computer) {
        super.attach(computer);
        blockEntity.setDigitalInput(Mth.clamp(blockEntity.getPower(), 0.0f, 1.0f));
        blockEntity.setControlMode(ControlMode.PERIPHERAL);
    }

    @Override
    public void detach(@NotNull IComputerAccess computer) {
        super.detach(computer);
        blockEntity.setDigitalInput(0.0f);
        blockEntity.setRedstonePower(0);
        blockEntity.setControlMode(ControlMode.NORMAL);
    }
}
