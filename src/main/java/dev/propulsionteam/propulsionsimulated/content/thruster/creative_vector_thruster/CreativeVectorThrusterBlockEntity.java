package dev.propulsionteam.propulsionsimulated.content.thruster.creative_vector_thruster;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter;
import dev.propulsionteam.propulsionsimulated.content.thruster.creative_thruster.CreativeThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.creative_thruster.CreativeThrusterPowerScrollValueBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plasma.PlasmaParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plume.PlumeParticleData;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.utility.GoggleUtils;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

public class CreativeVectorThrusterBlockEntity extends VectorThrusterBlockEntity {
    private CreativeThrusterPowerScrollValueBehaviour powerBehaviour;
    private CreativeThrusterBlockEntity.PlumeType plumeType = CreativeThrusterBlockEntity.PlumeType.PLASMA;

    public CreativeVectorThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.CREATIVE_VECTOR_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        ValueBoxTransform slot = new CreativeVectorThrusterValueBox(true);
        powerBehaviour = new CreativeThrusterPowerScrollValueBehaviour(this, slot, () -> PropulsionConfig.CREATIVE_VECTOR_THRUSTER_MAX_THRUST.get());
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
            float baseThrustPn = powerBehaviour.getTargetThrust() * 1000.0f;
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
        return PropulsionConfig.CREATIVE_VECTOR_THRUSTER_PARTICLE_COUNT_MULTIPLIER.get();
    }

    @Override
    protected double getParticleVelocityMultiplier() {
        return PropulsionConfig.CREATIVE_VECTOR_THRUSTER_PARTICLE_VELOCITY_MULTIPLIER.get();
    }

    @Override
    public CreativeThrusterBlockEntity.PlumeType getPlumeType() {
        return plumeType;
    }

    @Override
    public boolean shouldEmitParticles() {
        if (plumeType == CreativeThrusterBlockEntity.PlumeType.NONE)
            return false;
        if (!isPowered())
            return false;
        return hasPlumeSpace();
    }

    private boolean hasPlumeSpace() {
        if (level == null)
            return false;

        Direction facing = getBlockState().getValue(CreativeVectorThrusterBlock.FACING);
        BlockPos plumeOccupiedPosition = worldPosition.relative(facing.getOpposite());
        return !SimulatedThrustAdapter.getBlockStateSafe(level, plumeOccupiedPosition).isFaceSturdy(level, plumeOccupiedPosition, facing);
    }

    public void cyclePlumeType() {
        int ordinal = plumeType.ordinal() + 1;
        if (ordinal >= CreativeThrusterBlockEntity.PlumeType.values().length) {
            ordinal = 0;
        }
        plumeType = CreativeThrusterBlockEntity.PlumeType.values()[ordinal];
        setChanged();
        sendData();
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        if (plumeType == CreativeThrusterBlockEntity.PlumeType.PLASMA) {
            return new PlasmaParticleData();
        }
        if (plumeType == CreativeThrusterBlockEntity.PlumeType.ION) {
            return new IonParticleData(List.of(), null, 0.85f);
        }
        if (plumeType == CreativeThrusterBlockEntity.PlumeType.PLUME) {
            return new PlumeParticleData();
        }
        return new PlumeParticleData();
    }

    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        float obstructionEfficiency = 100;
        ChatFormatting tooltipColor = ChatFormatting.GREEN;
        if (emptyBlocks < OBSTRUCTION_LENGTH) {
            obstructionEfficiency = calculateObstructionEffect() * 100;
            tooltipColor = GoggleUtils.efficiencyColor(obstructionEfficiency);
            CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.obstructed")).space()
                    .add(CreateLang.text(GoggleUtils.makeObstructionBar(emptyBlocks, OBSTRUCTION_LENGTH))).style(tooltipColor).forGoggles(tooltip);
        }

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.efficiency")).text(": ")
                .add(CreateLang.number(obstructionEfficiency)).add(CreateLang.text("%"))
                .style(tooltipColor).forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.thrust_output"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        MutableComponent thrustValue = Component.literal(String.format(Locale.ROOT, "%.2f", this.getDisplayedThrustPnForTooltip() / 1000.0d))
                .withStyle(ChatFormatting.AQUA);
        CreateLang.builder()
                .add(Component.literal(" "))
                .add(Component.translatable("createpropulsion.tooltip.thrust1"))
                .add(thrustValue)
                .add(Component.literal(" pN").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);

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

    @Override
    protected double getBaseThrust() {
        return powerBehaviour.getTargetThrust();
    }

    @Override
    protected double getRawThrustCap() {
        return powerBehaviour.getTargetThrust();
    }

    @Override
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection) {
        this.emptyBlocks = OBSTRUCTION_LENGTH;
    }

    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("plumeType", plumeType.ordinal());
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("plumeType")) {
            int idx = compound.getInt("plumeType");
            plumeType = CreativeThrusterBlockEntity.PlumeType.values()[Mth.clamp(idx, 0, CreativeThrusterBlockEntity.PlumeType.values().length - 1)];
        }
    }
}
