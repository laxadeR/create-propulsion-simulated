package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.Function;

import dev.propulsionteam.propulsionsimulated.heat.engine.StirlingEngineBlockEntity;
import dev.propulsionteam.propulsionsimulated.redstone_transmission.RedstoneTransmissionBlockEntity;
import dev.propulsionteam.propulsionsimulated.tilt_adapter.TiltAdapterBlockEntity;
import dev.propulsionteam.propulsionsimulated.thruster.creative_thruster.CreativeThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.thruster.thruster.ThrusterBlockEntity;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dan200.computercraft.api.peripheral.IPeripheral;

public class ComputerBehaviour extends AbstractComputerBehaviour {
    protected IPeripheral peripheral;
    protected Supplier<IPeripheral> peripheralSupplier;

    private static final Map<Class<? extends SmartBlockEntity>, Function<SmartBlockEntity, IPeripheral>> PERIPHERAL_FACTORIES = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <T extends SmartBlockEntity> void register(Class<T> clazz, Function<T, IPeripheral> factory) {
        PERIPHERAL_FACTORIES.put(clazz, be -> factory.apply((T) be));
    }

    static {
        register(ThrusterBlockEntity.class, ThrusterPeripheral::new);
        register(CreativeThrusterBlockEntity.class, CreativeThrusterPeripheral::new);
        register(StirlingEngineBlockEntity.class, StirlingEnginePeripheral::new);
        register(RedstoneTransmissionBlockEntity.class, RedstoneTransmissionPeripheral::new);
        register(TiltAdapterBlockEntity.class, TiltAdapterPeripheral::new);
    }

    public ComputerBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
        this.peripheralSupplier = getPeripheralFor(blockEntity);
    }

    public static Supplier<IPeripheral> getPeripheralFor(SmartBlockEntity blockEntity) {
        Function<SmartBlockEntity, IPeripheral> factory = PERIPHERAL_FACTORIES.get(blockEntity.getClass());
        if (factory != null) {
            return () -> factory.apply(blockEntity);
        }

        throw new IllegalArgumentException("No peripheral available for " + blockEntity.getType());
    }

    @Override
    public IPeripheral getPeripheralCapability() {
        if (peripheral == null)
            peripheral = peripheralSupplier.get();
        return peripheral;
    }

    @Override
    public void removePeripheral() {
        peripheral = null;
    }
}
