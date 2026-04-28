package dev.propulsionteam.propulsionsimulated.thruster.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.thruster.FluidThrusterProperties;
import dev.propulsionteam.propulsionsimulated.thruster.ThrusterFuelManager;
import dev.propulsionteam.propulsionsimulated.thruster.ThrusterParticleType;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

public class ThrusterBlockEntity extends AbstractThrusterBlockEntity {
    public static final float BASE_FUEL_CONSUMPTION = 2;
    public static final int BASE_MAX_THRUST = 600000;
    public SmartFluidTankBehaviour tank;

    public ThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public ThrusterBlockEntity(BlockPos pos, BlockState state) {
        this(PropulsionBlockEntities.THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        float thrust = 0;
        float currentPower = getPower();

        //This thruster only works if it has valid fuel and power
        if (isWorking() && currentPower > 0) {
            var properties = getFuelProperties(fluidStack().getFluid());
            float obstructionEffect = calculateObstructionEffect();
            float thrustPercentage = Math.min(currentPower, obstructionEffect);

            if (thrustPercentage > 0 && properties != null) {
                int tick_rate = PropulsionConfig.THRUSTER_TICKS_PER_UPDATE.get();
                int consumption = calculateFuelConsumption(currentPower, properties.consumptionMultiplier, tick_rate);
                FluidStack drainedStack = tank.getPrimaryHandler().drain(consumption, IFluidHandler.FluidAction.EXECUTE);
                int fuelConsumed = drainedStack.getAmount();

                if (fuelConsumed > 0) {
                    float consumptionRatio = (float) fuelConsumed / (float) consumption;
                    float thrustMultiplier = PropulsionConfig.THRUSTER_THRUST_MULTIPLIER.get().floatValue();
                    float fuelEfficiency = ThrusterFuelManager.getEfficiency(fluidStack().getFluid());
                    thrust = BASE_MAX_THRUST * thrustMultiplier * thrustPercentage * properties.thrustMultiplier * fuelEfficiency * consumptionRatio;
                }
            }
        }
        thrusterData.setThrust(thrust);
        isThrustDirty = false;
    }

    @Override
    protected boolean isWorking() {
        return validFluid();
    }

    @Override
    protected boolean shouldEmitParticles() {
        if (!super.shouldEmitParticles()) {
            return false;
        }
        FluidThrusterProperties properties = getFuelProperties(fluidStack().getFluid());
        return properties != null && properties.particleType != ThrusterParticleType.NONE;
    }

    public Direction getFluidCapSide() {
        return getBlockState().getValue(ThrusterBlock.FACING);
    }

    @Override
    protected double getNozzleOffsetFromCenter() {
        return 0.95;
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (fluidStack().isEmpty()) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.no_fuel")).style(ChatFormatting.RED);
        } else if (!validFluid()) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.wrong_fuel")).style(ChatFormatting.RED);
        } else if (!isPowered()) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.not_powered")).style(ChatFormatting.GOLD);
        } else if (emptyBlocks == 0) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.obstructed")).style(ChatFormatting.RED);
        } else {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.working")).style(ChatFormatting.GREEN);
        }
    }

    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addThrusterDetails(tooltip, isPlayerSneaking);
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getPrimaryHandler());
    }

    @Override
    protected float getFuelEfficiencyMultiplier() {
        FluidStack currentFluid = fluidStack();
        if (currentFluid.isEmpty()) {
            return 1.0f;
        }
        return ThrusterFuelManager.getEfficiency(currentFluid.getFluid());
    }

    public FluidStack fluidStack() {
        return tank.getPrimaryHandler().getFluid();
    }

    public boolean validFluid() {
        if (fluidStack().isEmpty()) return false;
        return getFuelProperties(fluidStack().getFluid()) != null;
    }

    public FluidThrusterProperties getFuelProperties(Fluid fluid) {
        return ThrusterFuelManager.getProperties(fluid);
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        FluidThrusterProperties properties = getFuelProperties(fluidStack().getFluid());
        if (properties == null) {
            return super.createParticleOptions();
        }
        FluidThrusterProperties resolvedProperties = properties;
        if (properties.useFluidColor) {
            int fluidColor = IClientFluidTypeExtensions.of(fluidStack().getFluid()).getTintColor(fluidStack()) & 0xFFFFFF;
            resolvedProperties = new FluidThrusterProperties(
                properties.thrustMultiplier,
                properties.consumptionMultiplier,
                properties.particleType,
                properties.overrideTextures,
                fluidColor,
                true
            );
        }
        return resolvedProperties.particleType.createParticleOptions(resolvedProperties);
    }

    private int calculateFuelConsumption(float powerPercentage, float fluidPropertiesConsumptionMultiplier, int tick_rate) {
        float base_consumption = BASE_FUEL_CONSUMPTION * PropulsionConfig.THRUSTER_CONSUMPTION_MULTIPLIER.get().floatValue();
        return (int) Math.ceil(base_consumption * powerPercentage * fluidPropertiesConsumptionMultiplier * tick_rate);
    }
}
