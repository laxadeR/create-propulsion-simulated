package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity.ControlMode;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

public class ThrusterPeripheral extends SyncedPeripheral<ThrusterBlockEntity> {
    private final FluidMethods fluidMethods = new FluidMethods();

    public ThrusterPeripheral(ThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "propulsion_thruster";
    }

    @LuaFunction
    public final int getObstruction() {
        return blockEntity.getUnobstructedBlocks();
    }

    @LuaFunction(mainThread = true)
    public final void setPower(int redstonePower) {
        blockEntity.setRedstonePower(redstonePower);
    }

    @LuaFunction(mainThread = true)
    public final void setPowerNormalized(double power) {
        int redstonePower = Mth.floor(Mth.clamp(power, 0.0d, 1.0d) * 15.0d + 1.0e-6d);
        blockEntity.setRedstonePower(redstonePower);
    }

    @LuaFunction(mainThread = true)
    public final double getPower() {
        return blockEntity.getThrottle();
    }

    @LuaFunction
    public final double getCurrentThrustPN() {
        return blockEntity.getCurrentThrust();
    }

    @LuaFunction
    public final double getCurrentThrustKN() {
        return getCurrentThrustPN() / 1000.0d;
    }

    @LuaFunction
    public final double getDisplayedThrustPN() {
        return blockEntity.getDisplayedThrustPnForTooltip();
    }

    @LuaFunction
    public final double getDisplayedThrustKN() {
        return getDisplayedThrustPN() / 1000.0d;
    }

    @LuaFunction
    public final double getAirflowMs() {
        return blockEntity.getDisplayedAirflowMsForTooltip();
    }

    @LuaFunction(mainThread = true)
    public final int getFuelAmountMb() {
        return blockEntity.getFuelAmountMb();
    }

    @LuaFunction(mainThread = true)
    public final int getFuelCapacityMb() {
        return blockEntity.getFuelCapacityMb();
    }

    //IFluidHandler methods passthrough
    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String, ?>> tanks() throws LuaException {
        IFluidHandler handler = getHandler();
        return this.fluidMethods.tanks(handler);
    }

    @LuaFunction(mainThread = true)
    public final int pushFluid(IComputerAccess computer, String toName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        IFluidHandler handler = getHandler();
        return this.fluidMethods.pushFluid(handler, computer, toName, limit, fluidName);
    }

    @LuaFunction(mainThread = true)
    public final int pullFluid(IComputerAccess computer, String fromName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        IFluidHandler handler = getHandler();
        return this.fluidMethods.pullFluid(handler, computer, fromName, limit, fluidName);
    }

    private IFluidHandler getHandler() throws LuaException {
        if (blockEntity.isIon()) {
            throw new LuaException("Ion thruster has no fluid tank");
        }
        IFluidHandler handler = blockEntity.getFluidHandler(blockEntity.getFacing());
        if (handler == null) throw new LuaException("Fluid tank not available");
        return handler;
    }

    //Boilerplate
    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof ThrusterPeripheral otherThruster) {
            return this.blockEntity == otherThruster.blockEntity;
        }
        return false;
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
