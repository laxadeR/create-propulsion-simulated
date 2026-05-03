package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.ComputerBehaviour;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

public class IonThrusterBlockEntity extends ThrusterBlockEntity {
    private int energyStored;
    private double energyDrainAccumulator;
    private long lastEnergyDrainGameTime = -1L;
    private double lastConsumedFePerTick;

    private final IEnergyStorage energyHandler = new IEnergyStorage() {
        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }
            final boolean wasEmpty = energyStored <= 0;
            final int accepted = Math.min(maxReceive, Math.max(0, getEnergyCapacity() - energyStored));
            if (!simulate && accepted > 0) {
                energyStored += accepted;
                setChanged();
                // Recalculate immediately only when transitioning from unpowered to powered.
                if (wasEmpty) {
                    dirtyThrust();
                }
                notifyUpdate();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStored;
        }

        @Override
        public int getMaxEnergyStored() {
            return getEnergyCapacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public IonThrusterBlockEntity(final BlockPos pos, final BlockState state) {
        super(PropulsionBlockEntities.ION_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    protected IonThrusterBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        // Ion thrusters should evaluate power/consumption every server tick so FE usage is stable and responsive.
        this.isThrustDirty = true;
        super.tick();
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        // Keep ion/vector tankless, but still inherit base behaviours like CC + plume damage.
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
        behaviours.add(new ThrusterDamager(this));
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        float thrust = 0;
        float currentPower = getPower();

        if (currentPower > 0 && energyStored > 0) {
            float obstructionEffect = calculateObstructionEffect();
            float thrustPercentage = Math.min(currentPower, obstructionEffect);

            if (thrustPercentage > 0) {
                long currentGameTime = level != null ? level.getGameTime() : 0L;
                int ticksElapsed = 1;
                if (lastEnergyDrainGameTime >= 0L) {
                    ticksElapsed = (int) Math.max(0L, currentGameTime - lastEnergyDrainGameTime);
                }
                lastEnergyDrainGameTime = currentGameTime;

                // Config value is FE/t at full throttle; scale by throttle and elapsed ticks.
                double requestedDrain = energyDrainAccumulator
                        + (double) ticksElapsed * thrustPercentage * PropulsionConfig.ION_THRUSTER_FE_PER_TICK_AT_FULL_THROTTLE.get();
                int totalDrain = (int) Math.floor(requestedDrain);
                energyDrainAccumulator = requestedDrain - totalDrain;

                int consumed = Math.min(energyStored, totalDrain);
                if (consumed > 0) {
                    energyStored -= consumed;
                    float consumptionRatio = (float) consumed / (float) totalDrain;
                    float thrustMultiplier = PropulsionConfig.THRUSTER_THRUST_MULTIPLIER.get().floatValue();
                    float baseThrustPn = (float) (getBaseThrust() * 1000.0); // convert configured base to pN-scale like other thrusters
                    baseThrustPn *= (float) calculateAtmosphericFactor();
                    thrust = baseThrustPn * thrustMultiplier * thrustPercentage * consumptionRatio;
                }
                lastConsumedFePerTick = ticksElapsed > 0 ? (double) consumed / (double) ticksElapsed : 0.0d;
            } else {
                lastConsumedFePerTick = 0.0d;
            }
        } else {
            lastConsumedFePerTick = 0.0d;
        }
        // Mark dirty if energy was depleted to force thrust recalculation
        if (energyStored == 0 && thrust == 0) {
            isThrustDirty = true;
        } else {
            isThrustDirty = false;
        }
        setThrustAndSync(thrust);
        // Sync energy/consumption values used in goggles.
        setChanged();
        notifyUpdate();
    }

    @Override
    public FluidStack fluidStack() {
        return FluidStack.EMPTY;
    }

    @Override
    public boolean validFluid() {
        return true;
    }

    @Override
    public boolean isIon() {
        return true;
    }

    @Override
    protected double getParticleCountMultiplier() {
        return PropulsionConfig.ION_THRUSTER_PARTICLE_COUNT_MULTIPLIER.get();
    }

    @Override
    protected double getParticleVelocityMultiplier() {
        return PropulsionConfig.ION_THRUSTER_PARTICLE_VELOCITY_MULTIPLIER.get();
    }

    @Override
    protected boolean supportsMultiblock() {
        return false;
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        return new IonParticleData();
    }

    @Override
    protected boolean isWorking() {
        return energyStored > 0;
    }

    @Override
    public boolean shouldEmitParticles() {
        return getThrottle() > 0 && energyStored > 0;
    }

    @Override
    public IFluidHandler getFluidHandler(final Direction side) {
        return null;
    }

    public IEnergyStorage getEnergyHandler(final Direction side) {
        if (side == null) {
            return this.energyHandler;
        }
        if (side != this.getEnergyInputSide()) {
            return null;
        }
        return this.energyHandler;
    }

    @Override
    public boolean isVisuallyActive() {
        return this.getThrottle() > 0.0d && this.energyStored > 0;
    }

    @Override
    public boolean tryConsumeFuelBucket(final Player player, final InteractionHand hand, final ItemStack heldStack) {
        return false;
    }

    @Override
    public int getFuelAmountMb() {
        return this.energyStored;
    }

    @Override
    public int getFuelCapacityMb() {
        return this.getEnergyCapacity();
    }

    @Override
    protected double getBaseThrust() {
        return Math.min(PropulsionConfig.ION_THRUSTER_BASE_THRUST.get(), this.getRawThrustCap());
    }

    @Override
    protected double getRawThrustCap() {
        return PropulsionConfig.ION_THRUSTER_MAX_THRUST.get();
    }

    public int getEnergyStoredFe() {
        return this.energyStored;
    }

    public int getEnergyCapacity() {
        return PropulsionConfig.ION_THRUSTER_ENERGY_CAPACITY_FE.get();
    }

    protected Direction getEnergyInputSide() {
        return this.getFacing();
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (this.getThrottle() <= 0.0d) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.not_powered"))
                    .style(ChatFormatting.GOLD);
        }
        if (this.energyStored <= 0) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.no_energy"))
                    .style(ChatFormatting.RED);
        }
        return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.working"))
                .style(ChatFormatting.GREEN);
    }

    @Override
    protected void addThrusterDetails(final List<Component> tooltip, final boolean isPlayerSneaking) {
        super.addThrusterDetails(tooltip, isPlayerSneaking);
        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.energy_container"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);
        CreateLang.builder()
                .add(Component.literal(" "))
                .add(Component.literal(Integer.toString(this.energyStored)).withStyle(ChatFormatting.AQUA))
                .add(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .add(Component.literal(Integer.toString(this.getEnergyCapacity())).withStyle(ChatFormatting.AQUA))
                .add(Component.literal(" FE").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);
        CreateLang.builder()
                .add(Component.literal(" "))
                .add(Component.literal(String.format(java.util.Locale.ROOT, "%.1f", this.lastConsumedFePerTick)).withStyle(ChatFormatting.AQUA))
                .add(Component.literal(" FE/t").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        tag.putInt("EnergyStored", this.energyStored);
        tag.putDouble("EnergyDrainAccumulator", this.energyDrainAccumulator);
        tag.putDouble("LastConsumedFePerTick", this.lastConsumedFePerTick);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        this.energyStored = tag.getInt("EnergyStored");
        this.energyDrainAccumulator = tag.getDouble("EnergyDrainAccumulator");
        this.lastConsumedFePerTick = tag.getDouble("LastConsumedFePerTick");
        this.lastEnergyDrainGameTime = -1L;
        this.clampEnergyToCapacity();
        super.read(tag, registries, clientPacket);
        // Ion thrusters are always single-block; strip any accidental multi state from old data.
        this.width = 1;
        this.controllerPos = null;
        this.updateConnectivity = false;
    }

    private void clampEnergyToCapacity() {
        this.energyStored = Math.clamp(this.energyStored, 0, this.getEnergyCapacity());
    }
}
