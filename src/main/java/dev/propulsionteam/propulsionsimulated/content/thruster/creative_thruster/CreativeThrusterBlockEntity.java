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
        powerBehaviour.value = 49;
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
            float thrustMultiplier = PropulsionConfig.CREATIVE_THRUSTER_THRUST_MULTIPLIER.get().floatValue();
            float powerMultiplier = powerBehaviour.getTargetThrust();
            float baseThrustPn = powerMultiplier * 1000.0f; // powerBehaviour value is already divided by 1000 for display
            baseThrustPn *= (float) calculateAtmosphericFactor();
            thrust = thrustMultiplier * currentPower * baseThrustPn;
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
    protected double getParticleCountMultiplier() {
        return PropulsionConfig.CREATIVE_THRUSTER_PARTICLE_COUNT_MULTIPLIER.get();
    }

    @Override
    protected double getParticleVelocityMultiplier() {
        return PropulsionConfig.CREATIVE_THRUSTER_PARTICLE_VELOCITY_MULTIPLIER.get();
    }

    @Override
    public PlumeType getPlumeType() {
        return plumeType;
    }

    @Override
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection) {
        this.emptyBlocks = OBSTRUCTION_LENGTH;
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

        // "Particle: Plasma"
        LangBuilder particleBuilder = CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle")).text(": ")
                .style(ChatFormatting.WHITE);

        switch (plumeType) {
            case PLASMA -> particleBuilder.add(CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.plasma"))
                    .style(ChatFormatting.AQUA));
            case ION -> particleBuilder.add(CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.ion"))
                    .style(ChatFormatting.BLUE));
            case PLUME -> particleBuilder.add(CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.plume"))
                    .style(ChatFormatting.GOLD));
            case NONE -> particleBuilder.add(CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.none"))
                    .style(ChatFormatting.DARK_GRAY));
        }

        particleBuilder.forGoggles(tooltip);
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
