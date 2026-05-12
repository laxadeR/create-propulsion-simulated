package dev.propulsionteam.propulsionsimulated.content.thruster.creative_thruster;

import java.util.List;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plasma.PlasmaParticleData;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter;

public class CreativeThrusterBlockEntity extends AbstractThrusterBlockEntity {
    private CreativeThrusterPowerScrollValueBehaviour powerBehaviour;

    public enum PlumeType {
        PLASMA, ION, PLUME, NONE
    }

    public PlumeType plumeType = PlumeType.PLASMA;

    public CreativeThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public CreativeThrusterBlockEntity(BlockPos pos, BlockState state) {
        this(PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        powerBehaviour = new CreativeThrusterPowerScrollValueBehaviour(this);
        // Start scroll at the configured base thrust value
        double base = PropulsionConfig.CREATIVE_THRUSTER_BASE_THRUST.get();
        double max = PropulsionConfig.CREATIVE_THRUSTER_MAX_THRUST.get();
        int startStep = (int) Math.round((base / max) * (CreativeThrusterPowerScrollValueBehaviour.TOTAL_STEPS - 1));
        powerBehaviour.value = Math.max(0, Math.min(CreativeThrusterPowerScrollValueBehaviour.TOTAL_STEPS - 1, startStep));
        powerBehaviour.withCallback(i -> {
            updateThrust(getBlockState());
            sendData();
        });
        behaviours.add(powerBehaviour);
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        float thrust = 0;
        float currentPower = getPower();
        if (currentPower > 0) {
            float baseThrustPn = (float) (powerBehaviour.getTargetThrust() * getThrustUnitsPerKn());
            baseThrustPn *= (float) calculateAtmosphericFactor();
            thrust = currentPower * baseThrustPn;
        }
        setThrustAndSync(thrust);
        isThrustDirty = false;
    }

    @Override
    protected boolean isWorking() {
        return true;
    }

    @Override
    public boolean isCreative() {
        return true;
    }


    @Override
    public PlumeType getPlumeType() {
        return plumeType;
    }

    @Override
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection) {
        this.emptyBlocks = PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
    }

    // Particles

    public void cyclePlumeType() {
        int ordinal = plumeType.ordinal() + 1;
        if (ordinal >= PlumeType.values().length) {
            ordinal = 0;
        }
        plumeType = PlumeType.values()[ordinal];
        setChanged();
        sendData();
    }

    @Override
    public double getNozzleOffsetFromCenter() {
        return 0.55;
    }

    @Override
    protected double getBaseThrust() {
        return powerBehaviour.getTargetThrust();
    }

    @Override
    protected double getRawThrustCap() {
        return powerBehaviour.getTargetThrust();
    }

    @Override
    public boolean shouldEmitParticles() {
        if (plumeType == PlumeType.NONE)
            return false;

        if (!isPowered())
            return false;

        return hasPlumeSpace();
    }

    private boolean hasPlumeSpace() {
        if (level == null)
            return false;

        Direction facing = getBlockState().getValue(CreativeThrusterBlock.FACING);
        BlockPos plumeOccupiedPosition = worldPosition.relative(facing.getOpposite());
        return !SimulatedThrustAdapter.getBlockStateSafe(level,plumeOccupiedPosition).isFaceSturdy(level, plumeOccupiedPosition, facing);
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        if (plumeType == PlumeType.PLASMA) {
            return new PlasmaParticleData();
        }
        if (plumeType == PlumeType.ION) {
            return new IonParticleData();
        }
        // Default is plume :P
        return super.createParticleOptions();
    }

    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addThrusterDetails(tooltip, isPlayerSneaking);
        addParticleCategory(tooltip);
    }

    private void addParticleCategory(List<Component> tooltip) {
        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        Component particleValue = switch (plumeType) {
            case PLASMA -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.plasma")
                    .withStyle(ChatFormatting.AQUA);
            case ION -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.ion")
                    .withStyle(ChatFormatting.BLUE);
            case PLUME -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.plume")
                    .withStyle(ChatFormatting.GOLD);
            case NONE -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.none")
                    .withStyle(ChatFormatting.DARK_GRAY);
        };

        CreateLang.builder()
                .add(Component.literal("  "))
                .add(particleValue)
                .forGoggles(tooltip);
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (isPowered()) {
            return CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.working"))
                    .style(ChatFormatting.GREEN);
        }
        return CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.not_powered"))
                .style(ChatFormatting.GOLD);
    }

    // CC slop

    public void setThrustConfig(int value) {
        int clamped = Math.max(0, Math.min(value, 99));
        if (powerBehaviour.getValue() != clamped) {
            powerBehaviour.setValue(clamped);
            updateThrust(getBlockState());
            setChanged();
            sendData();
        }
    }

    public int getThrustConfig() {
        return powerBehaviour.getValue();
    }

    public float getTargetThrustNewtons() {
        return powerBehaviour.getTargetThrust();
    }

    public float getCreativeTargetThrust() {
        return powerBehaviour.getTargetThrust();
    }

    // NBT

    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries,
            boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("plumeType", plumeType.ordinal());
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries,
            boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("plumeType")) {
            int idx = compound.getInt("plumeType");
            plumeType = PlumeType.values()[Mth.clamp(idx, 0, PlumeType.values().length - 1)];
        }
    }
}
