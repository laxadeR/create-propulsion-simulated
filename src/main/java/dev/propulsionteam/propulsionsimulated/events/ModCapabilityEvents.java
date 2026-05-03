package dev.propulsionteam.propulsionsimulated.events;

import dev.propulsionteam.propulsionsimulated.content.heat.burners.liquid.LiquidBurnerBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.heat.burners.liquid.PassthroughFluidHandler;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.IonThrusterBlockEntity;
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

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            PropulsionBlockEntities.ION_THRUSTER_BLOCK_ENTITY.get(),
            (be, side) -> ((IonThrusterBlockEntity) be).getEnergyHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            PropulsionBlockEntities.CORAL_GENERATOR_BLOCK_ENTITY.get(),
            (be, side) -> ((CoralGeneratorBlockEntity) be).getFluidHandler(side)
        );
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            PropulsionBlockEntities.CORAL_GENERATOR_BLOCK_ENTITY.get(),
            (be, side) -> ((CoralGeneratorBlockEntity) be).getEnergyHandler(side)
        );
    }

    private static IFluidHandler getThrusterFluidHandler(ThrusterBlockEntity blockEntity, Direction side) {
        return blockEntity.getFluidHandler(side);
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
