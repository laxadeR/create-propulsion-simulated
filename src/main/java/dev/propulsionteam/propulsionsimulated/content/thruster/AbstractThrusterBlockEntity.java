package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.ComputerBehaviour;
import dev.propulsionteam.propulsionsimulated.particles.plume.PlumeParticleData;
import dev.propulsionteam.propulsionsimulated.utility.GoggleUtils;
import dev.propulsionteam.propulsionsimulated.utility.math.MathUtility;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Vector3d;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

public abstract class AbstractThrusterBlockEntity extends SmartBlockEntity
    implements IHaveGoggleInformation, BlockEntitySubLevelPropellerActor, BlockSubLevelAssemblyListener, BlockEntityPropeller {
    //Constants
    protected static final int OBSTRUCTION_LENGTH = 10;
    protected static final int TICKS_PER_ENTITY_CHECK = 5;
    private static final float PARTICLE_VELOCITY = 4;
    
    protected static final float LOWEST_POWER_THRESHOLD = 5.0f / 15.0f;

    //Common State
    protected ThrusterData thrusterData;
    protected int emptyBlocks;
    protected boolean isThrustDirty = false;

    //Ticking
    private int currentTick = 0;
    private int clientTick = 0;
    private float particleSpawnAccumulator = 0.0f;

    //CC Peripheral
    public AbstractComputerBehaviour computerBehaviour;
    public enum ControlMode {
        NORMAL,
        PERIPHERAL
    }

    protected ControlMode controlMode = ControlMode.NORMAL;
    protected int redstoneInput = 0;
    protected float digitalInput = 0.0f;

    public AbstractThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        thrusterData = new ThrusterData();
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!level.isClientSide) {
            BlockState state = getBlockState();
            calculateObstruction(level, worldPosition, state.getValue(AbstractThrusterBlock.FACING));
            ThrusterData data = this.getThrusterData();
            data.setDirection(new Vector3d(state.getValue(AbstractThrusterBlock.FACING).getNormal().getX(), state.getValue(AbstractThrusterBlock.FACING).getNormal().getY(), state.getValue(AbstractThrusterBlock.FACING).getNormal().getZ()));
            data.setThrust(0);

            Block block = getBlockState().getBlock();
            if (block instanceof AbstractThrusterBlock) {
                ((AbstractThrusterBlock) block).doRedstoneCheck(level, getBlockState(), worldPosition);
            }
        }
    }

    //Control logic

    public void setRedstoneInput(int power) {
        if (redstoneInput != power) {
            redstoneInput = power;
            if (controlMode == ControlMode.NORMAL) {
                dirtyThrust();
                notifyUpdate();
            }
        }
    }

    public void setDigitalInput(float power) {
        float clamped = org.joml.Math.clamp(0.0f, 1.0f, power);
        if (java.lang.Math.abs(digitalInput - clamped) > 1e-4) {
            digitalInput = clamped;
            if (controlMode == ControlMode.PERIPHERAL) {
                dirtyThrust();
                notifyUpdate();
            }
        }
    }

    public void setControlMode(ControlMode mode) {
        if (this.controlMode != mode) {
            this.controlMode = mode;
            dirtyThrust();
            notifyUpdate();
        }
    }

    public float getPower() {
        if (controlMode == ControlMode.PERIPHERAL) {
            return digitalInput;
        }
        return redstoneInput / 15.0f;
    }

    public int getLegacyPowerInt() {
        return (int) Math.round(getPower() * 15);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
        behaviours.add(new ThrusterDamager(this));
    }

    @Override
    public void tick() {
        if (this.isRemoved()) {
            return;
        }
        //This part should ACTUALLY fix the issue with particle emission 
        if (level.getBlockState(worldPosition).getBlock() != this.getBlockState().getBlock()) {
            this.setRemoved();
            return;
        }

        super.tick();
        // Use live world state on each tick; cached BE state can be stale client-side
        // and cause particle direction to appear fixed.
        BlockState currentBlockState = level.getBlockState(worldPosition);
        if (level.isClientSide) {
            if (shouldEmitParticles()) {
                emitParticles(level, worldPosition, currentBlockState);
            }
            ThrusterSoundHooks.clientTick(this);
            return;
        }
        currentTick++;
        int tick_rate = PropulsionConfig.THRUSTER_TICKS_PER_UPDATE.get();

        //Periodically recalculate obstruction
        if (currentTick % (tick_rate * 2) == 0) {
            int previousEmptyBlocks = emptyBlocks;
            calculateObstruction(level, worldPosition, currentBlockState.getValue(AbstractThrusterBlock.FACING));
            if (previousEmptyBlocks != emptyBlocks) {
                isThrustDirty = true;
                setChanged();
                level.sendBlockUpdated(worldPosition, currentBlockState, currentBlockState, Block.UPDATE_CLIENTS);
            }
        }

        //Update thrust periodically or when marked dirty
        if (isThrustDirty || currentTick % tick_rate == 0) {
            updateThrust(currentBlockState);
        }
    }

    public abstract void updateThrust(BlockState currentBlockState);

    protected abstract boolean isWorking();

    protected abstract LangBuilder getGoggleStatus();

    public ThrusterData getThrusterData() {
        return thrusterData;
    }

    public int getEmptyBlocks() {
        return emptyBlocks;
    }

    public void dirtyThrust() {
        isThrustDirty = true;
    }

    public boolean shouldEmitParticles() {
        return isPowered() && isWorking();
    }

    protected boolean shouldDamageEntities() {
        return PropulsionConfig.THRUSTER_DAMAGE_ENTITIES.get() && isPowered() && isWorking();
    }

    protected void addSpecificGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {}
    
    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(AbstractThrusterBlock.FACING)) {
            return state.getValue(AbstractThrusterBlock.FACING);
        }
        return Direction.NORTH;
    }

    public float getCurrentThrust() {
        return (float) thrusterData.getThrust();
    }

    public boolean isVisuallyActive() {
        return getThrottle() > 0 && isWorking();
    }

    public int getUnobstructedBlocks() {
        return emptyBlocks;
    }

    public double getDisplayedThrustPnForTooltip() {
        return thrusterData.getThrust();
    }

    public double getDisplayedAirflowMsForTooltip() {
        return getAirflow();
    }
    protected float getFuelEfficiencyMultiplier() { return 1.0f; }
    
    public boolean isCreative() { return false; }
    public boolean isIon() { return false; }
    
    public dev.propulsionteam.propulsionsimulated.content.thruster.creative_thruster.CreativeThrusterBlockEntity.PlumeType getPlumeType() {
        return dev.propulsionteam.propulsionsimulated.content.thruster.creative_thruster.CreativeThrusterBlockEntity.PlumeType.NONE;
    }
    
    public IFluidHandler getFluidHandler(Direction side) { return null; }

    protected boolean isPowered() {
        return getPower() > MathUtility.epsilon;
    }

    protected float calculateObstructionEffect() {
        return (float) emptyBlocks / (float) OBSTRUCTION_LENGTH;
    }

    protected ParticleOptions createParticleOptions() {
        return new PlumeParticleData();
    }

    public abstract double getNozzleOffsetFromCenter();
    protected abstract double getBaseThrust();
    protected abstract double getRawThrustCap();

    public float getThrottle() {
        return getPower();
    }

    public void setRedstonePower(int power) {
        setRedstoneInput(power);
    }

    public boolean tryConsumeFuelBucket(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack heldStack) {
        return false;
    }

    public int getFuelAmountMb() { return 0; }
    public int getFuelCapacityMb() { return 0; }
    public boolean validFluid() { return false; }
    public net.neoforged.neoforge.fluids.FluidStack fluidStack() { return net.neoforged.neoforge.fluids.FluidStack.EMPTY; }

    @Override
    public BlockEntityPropeller getPropeller() {
        return this;
    }

    @Override
    public Direction getBlockDirection() {
        return getBlockState().getValue(AbstractThrusterBlock.FACING).getOpposite();
    }

    @Override
    public double getAirflow() {
        return getPower() * calculateObstructionEffect() * 200.0;
    }

    @Override
    public double getThrust() {
        // Sable force API works in kilo-pixel-Newtons; convert from our Newton-like internal thrust.
        return thrusterData.getThrust() / 1000.0;
    }

    @Override
    public boolean isActive() {
        return isPowered() && isWorking();
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        // Recompute obstruction and refresh redstone-derived power after assembly/disassembly moves.
        if (newLevel != null) {
            setRedstoneInput(newLevel.getBestNeighborSignal(newPos));
            calculateObstruction(newLevel, newPos, state.getValue(AbstractThrusterBlock.FACING));
            dirtyThrust();
        }
    }

    public void emitParticles(Level level, BlockPos pos, BlockState state) {
        if (emptyBlocks == 0) return;
        float power = getPower();
    
        double particleCountMultiplier = org.joml.Math.clamp(0.0, 2.0, PropulsionConfig.THRUSTER_PARTICLE_COUNT_MULTIPLIER.get());
        if (particleCountMultiplier <= 0) return;
    
        clientTick++;
        if (power < LOWEST_POWER_THRESHOLD && clientTick % 2 == 0) {
            clientTick = 0;
            return;
        }
    
        this.particleSpawnAccumulator += particleCountMultiplier;
    
        int particlesToSpawn = (int) this.particleSpawnAccumulator;
        if (particlesToSpawn == 0) return;
    
        float visualPower = Math.max(power, LOWEST_POWER_THRESHOLD);

        this.particleSpawnAccumulator -= particlesToSpawn;
        Direction direction = state.getValue(AbstractThrusterBlock.FACING);
        Direction oppositeDirection = direction.getOpposite();
    
        double currentNozzleOffset = getNozzleOffsetFromCenter();
        Vector3d additionalVel = new Vector3d();

        Vec3 localExhaustDirection = new Vec3(oppositeDirection.getStepX(), oppositeDirection.getStepY(), oppositeDirection.getStepZ());
        Vec3 localBlockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 localNozzlePosition = localBlockCenter.add(localExhaustDirection.scale(currentNozzleOffset));

        // Convert local sublevel coordinates into world-space coordinates so particles align
        // with ship rotation instead of sticking to global axes.
        Vec3 worldNozzlePosition = Sable.HELPER.projectOutOfSubLevel(level, localNozzlePosition);
        Vec3 worldAheadPosition = Sable.HELPER.projectOutOfSubLevel(level, localNozzlePosition.add(localExhaustDirection));
        Vec3 worldExhaustDirection = worldAheadPosition.subtract(worldNozzlePosition);
        if (worldExhaustDirection.lengthSqr() < MathUtility.epsilon) {
            worldExhaustDirection = localExhaustDirection;
        } else {
            worldExhaustDirection = worldExhaustDirection.normalize();
        }

        double particleX = worldNozzlePosition.x;
        double particleY = worldNozzlePosition.y;
        double particleZ = worldNozzlePosition.z;

        Vector3d particleVelocity = new Vector3d(worldExhaustDirection.x, worldExhaustDirection.y, worldExhaustDirection.z)
            .mul(PARTICLE_VELOCITY * visualPower)
            .add(additionalVel);
    
        ParticleOptions particleData = createParticleOptions();

        //Spawn the calculated number of particles.
        for (int i = 0; i < particlesToSpawn; i++) {
            level.addParticle(particleData, true,
                particleX, particleY, particleZ,
                particleVelocity.x, particleVelocity.y, particleVelocity.z);
        }
    }

    @SuppressWarnings("deprecation") // i hate compilers let me use ts
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection){
        //Starting from the block behind and iterate OBSTRUCTION_LENGTH blocks in that direction
        //Can't really use level.clip as we explicitly want to check for obstruction only in ship space
        int oldEmptyBlocks = this.emptyBlocks;
        for (emptyBlocks = 0; emptyBlocks < OBSTRUCTION_LENGTH; emptyBlocks++){
            BlockPos checkPos = pos.relative(forwardDirection.getOpposite(), emptyBlocks + 1);
            BlockState state = level.getBlockState(checkPos);
            if (!state.isAir() && state.isSolid()) break;
        }
        if (oldEmptyBlocks != this.emptyBlocks) { //Only set dirty if it actually changed
            isThrustDirty = true;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean wasThrustDirty = isThrustDirty;
        calculateObstruction(getLevel(), worldPosition, getBlockState().getValue(AbstractThrusterBlock.FACING));
        isThrustDirty = wasThrustDirty;

        CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status")).text(":").space().add(getGoggleStatus()).forGoggles(tooltip);

        addThrusterDetails(tooltip, isPlayerSneaking);

        if (controlMode == ControlMode.PERIPHERAL) {
            CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.cc.peripheral_controlled")).style(ChatFormatting.GRAY).forGoggles(tooltip);
        }

        return true;
    }

    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        float obstructionEfficiency = 100;
        ChatFormatting tooltipColor = ChatFormatting.GREEN;
        if (emptyBlocks < OBSTRUCTION_LENGTH) {
            obstructionEfficiency = calculateObstructionEffect() * 100;
            tooltipColor = GoggleUtils.efficiencyColor(obstructionEfficiency);
            CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.obstructed")).space().add(CreateLang.text(GoggleUtils.makeObstructionBar(emptyBlocks, OBSTRUCTION_LENGTH))).style(tooltipColor).forGoggles(tooltip);
        }

        float fuelEfficiency = getFuelEfficiencyMultiplier();
        float totalEfficiency = obstructionEfficiency * fuelEfficiency;
        ChatFormatting totalEfficiencyColor = GoggleUtils.efficiencyColor(totalEfficiency);
        CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.efficiency")).text(": ").add(CreateLang.number(totalEfficiency)).add(CreateLang.text("%")).style(totalEfficiencyColor).forGoggles(tooltip);
    }


    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("emptyBlocks", emptyBlocks);
        compound.putInt("currentTick", currentTick);
        
        compound.putInt("RedstoneInput", redstoneInput);
        compound.putFloat("DigitalInput", digitalInput);
        compound.putInt("ControlMode", controlMode.ordinal());
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        emptyBlocks = compound.getInt("emptyBlocks");
        currentTick = compound.getInt("currentTick");

        redstoneInput = compound.getInt("RedstoneInput");
        digitalInput = compound.getFloat("DigitalInput");
        if (compound.contains("ControlMode")) {
            controlMode = ControlMode.values()[compound.getInt("ControlMode")];
        }
    }

}
