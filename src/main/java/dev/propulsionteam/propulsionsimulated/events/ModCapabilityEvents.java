package dev.propulsionteam.propulsionsimulated.events;

import dev.propulsionteam.propulsionsimulated.heat.burners.liquid.LiquidBurnerBlockEntity;
import dev.propulsionteam.propulsionsimulated.heat.burners.liquid.PassthroughFluidHandler;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.thruster.thruster.ThrusterBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class ModCapabilityEvents {
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            PropulsionBlockEntities.THRUSTER_BLOCK_ENTITY.get(),
            ModCapabilityEvents::getThrusterFluidHandler
        );

        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            PropulsionBlockEntities.LIQUID_BURNER_BLOCK_ENTITY.get(),
            ModCapabilityEvents::getLiquidBurnerFluidHandler
        );
    }

    private static IFluidHandler getThrusterFluidHandler(ThrusterBlockEntity blockEntity, Direction side) {
        if (blockEntity.tank == null) {
            return null;
        }
        if (side != null && side != blockEntity.getFluidCapSide()) {
            return null;
        }
        return blockEntity.tank.getPrimaryHandler();
    }

    private static IFluidHandler getLiquidBurnerFluidHandler(LiquidBurnerBlockEntity blockEntity, Direction side) {
        IFluidHandler primaryHandler = blockEntity.getPrimaryFluidHandler();
        if (primaryHandler == null) {
            return null;
        }
        if (side == null) {
            return primaryHandler;
        }
        return new PassthroughFluidHandler(blockEntity, side);
    }
}
