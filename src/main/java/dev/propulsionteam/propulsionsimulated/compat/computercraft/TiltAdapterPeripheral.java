package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dev.propulsionteam.propulsionsimulated.tilt_adapter.TiltAdapterBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;

public class TiltAdapterPeripheral extends SyncedPeripheral<TiltAdapterBlockEntity> {
    public TiltAdapterPeripheral(TiltAdapterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "tilt_adapter";
    }

    @LuaFunction
    public final double getLeftSignal() {
        return blockEntity.getLeft();
    }

    @LuaFunction
    public final double getRightSignal() {
        return blockEntity.getRight();
    }

    @LuaFunction(mainThread = true)
    public final void setTargetAngle(double angle) {
        blockEntity.setComputerTargetAngle((float) angle);
    }
}
