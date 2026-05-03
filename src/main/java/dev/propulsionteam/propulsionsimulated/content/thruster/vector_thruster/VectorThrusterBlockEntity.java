package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

import dev.propulsionteam.propulsionsimulated.content.thruster.IonThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.joml.Vector3d;

public class VectorThrusterBlockEntity extends IonThrusterBlockEntity {
    public static final float MAX_VISUAL_TILT_DEGREES = 30.0f;
    private static final float TWEEN_SPEED = 0.2f;

    public LinkBehaviour leftLink;
    public VectorRedstoneLinkBehaviour rightLink;

    private int leftSignal;
    private int rightSignal;
    private float targetVectorX;
    private float targetVectorY;
    private float currentVectorX;
    private float currentVectorY;
    private float obstructionEfficiency = 1.0f;

    public VectorThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    protected VectorThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected double getParticleCountMultiplier() {
        return PropulsionConfig.VECTOR_THRUSTER_PARTICLE_COUNT_MULTIPLIER.get();
    }

    @Override
    protected double getParticleVelocityMultiplier() {
        return PropulsionConfig.VECTOR_THRUSTER_PARTICLE_VELOCITY_MULTIPLIER.get();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        
        leftLink = LinkBehaviour.receiver(this,
            ValueBoxTransform.Dual.makeSlots(isFirst -> new VectorThrusterLinkTransform(isFirst, false)),
            this::setLeftSignal);
            
        rightLink = VectorRedstoneLinkBehaviour.receiver(this,
            ValueBoxTransform.Dual.makeSlots(isFirst -> new VectorThrusterLinkTransform(isFirst, true)),
            this::setRightSignal);
            
        behaviours.add(leftLink);
        behaviours.add(rightLink);
    }

    private void setLeftSignal(int power) {
        int clamped = Math.clamp(power, 0, 15);
        if (this.leftSignal == clamped) {
            return;
        }
        this.leftSignal = clamped;
        onVectorSignalChanged();
    }

    private void setRightSignal(int power) {
        int clamped = Math.clamp(power, 0, 15);
        if (this.rightSignal == clamped) {
            return;
        }
        this.rightSignal = clamped;
        onVectorSignalChanged();
    }

    @Override
    public void tick() {
        updateMappedTargets();
        currentVectorX = tweenTowards(currentVectorX, targetVectorX);
        currentVectorY = tweenTowards(currentVectorY, targetVectorY);
        super.tick();
    }

    public float getCurrentVectorX() {
        return currentVectorX;
    }

    public float getCurrentVectorY() {
        return currentVectorY;
    }

    @Override
    public Vector3d getThrustDirectionLocal() {
        Vector3d forward = new Vector3d(getFacing().getStepX(), getFacing().getStepY(), getFacing().getStepZ()).normalize();
        Vector3d right = computeRight(forward);
        Vector3d up = new Vector3d(right).cross(forward).normalize();

        Vector3d combined = new Vector3d(forward)
            .fma(currentVectorX, right)
            .fma(currentVectorY, up);

        if (combined.lengthSquared() < 1e-8) {
            return forward;
        }
        return combined.normalize();
    }

    @Override
    protected Vec3 getParticleExhaustDirectionLocal() {
        // Keep plume moving backward, but mirror lateral/up deflection.
        // Scale side/up components to match the rendered max tilt angle.
        Vector3d forward = new Vector3d(getFacing().getStepX(), getFacing().getStepY(), getFacing().getStepZ()).normalize();
        Vector3d right = computeRight(forward);
        Vector3d up = new Vector3d(right).cross(forward).normalize();
        double tiltScale = Math.tan(Math.toRadians(MAX_VISUAL_TILT_DEGREES));

        Vector3d exhaust = new Vector3d(forward).negate()
            .fma(currentVectorX * tiltScale, right)
            .fma(currentVectorY * tiltScale, up);

        if (exhaust.lengthSquared() < 1e-8) {
            exhaust.set(forward).negate();
        } else {
            exhaust.normalize();
        }
        return new Vec3(exhaust.x, exhaust.y, exhaust.z);
    }

    @Override
    protected Vec3 getLocalNozzlePosition(BlockPos pos, Vec3 localExhaustDirection, double nozzleOffset) {
        Vector3d forward = new Vector3d(getFacing().getStepX(), getFacing().getStepY(), getFacing().getStepZ()).normalize();
        Vector3d right = computeRight(forward);
        Vector3d up = new Vector3d(right).cross(forward).normalize();

        Vector3d center = new Vector3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d);
        Vector3d pivot = new Vector3d(center)
            .fma(-1.0d / 16.0d, right)
            .fma(1.0d / 16.0d, up)
            .fma(7.0d / 16.0d, forward);

        // Neutral nozzle anchor used before applying vector rotation.
        Vector3d neutralNozzle = new Vector3d(center).fma(-nozzleOffset, forward);
        Vector3d relative = neutralNozzle.sub(pivot, new Vector3d());

        double yawRad = Math.toRadians(Mth.clamp(currentVectorX, -1.0f, 1.0f) * MAX_VISUAL_TILT_DEGREES);
        double pitchRad = Math.toRadians(-Mth.clamp(currentVectorY, -1.0f, 1.0f) * MAX_VISUAL_TILT_DEGREES);

        rotateAroundAxis(relative, up, yawRad);
        rotateAroundAxis(relative, right, pitchRad);

        Vector3d rotatedNozzle = pivot.add(relative, new Vector3d());
        return new Vec3(rotatedNozzle.x, rotatedNozzle.y, rotatedNozzle.z);
    }

    @Override
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection) {
        int scanLength = OBSTRUCTION_LENGTH;
        ObstructionRaySample sample = sampleObstructionRaycast(level, scanLength);
        double firstHitDistance = sample.firstHitDistance();
        float newEfficiency = scanLength <= 0
            ? 0.0f
            : Math.clamp((float) (firstHitDistance / scanLength), 0.0f, 1.0f);
        int newEmptyBlocks = sample.emptyBlocksEstimate();

        if (this.emptyBlocks != newEmptyBlocks || Math.abs(this.obstructionEfficiency - newEfficiency) > 1e-4f) {
            this.emptyBlocks = newEmptyBlocks;
            this.obstructionEfficiency = newEfficiency;
            this.isThrustDirty = true;
        }
    }

    @Override
    protected float calculateObstructionEffect() {
        return obstructionEfficiency;
    }

    private void onVectorSignalChanged() {
        updateMappedTargets();
        if (level != null && !level.isClientSide) {
            setChanged();
            notifyUpdate();
        }
    }

    private void updateMappedTargets() {
        // Relative RIGHT side drives X, relative LEFT side drives Y.
        targetVectorX = mapSignalLevelToAxis(rightSignal);
        targetVectorY = mapSignalLevelToAxis(leftSignal);
    }

    private static float mapSignalLevelToAxis(int level) {
        int clamped = Math.clamp(level, 0, 15);
        if (clamped == 0 || clamped == 8) {
            return 0.0f;
        }
        if (clamped < 8) {
            return -1.0f + ((clamped - 1.0f) / 6.0f);
        }
        return (clamped - 9.0f) / 6.0f;
    }

    private static float tweenTowards(float current, float target) {
        float next = current + (target - current) * TWEEN_SPEED;
        if (Math.abs(target - next) < 0.001f) {
            return target;
        }
        return next;
    }

    private static Vector3d computeRight(Vector3d forward) {
        Vector3d reference = Math.abs(forward.y) > 0.999 ? new Vector3d(0, 0, 1) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(forward).cross(reference);
        if (right.lengthSquared() < 1e-8) {
            return new Vector3d(1, 0, 0);
        }
        return right.normalize();
    }

    private static void rotateAroundAxis(Vector3d v, Vector3d axisUnit, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double dot = v.dot(axisUnit);

        Vector3d cross = new Vector3d(axisUnit).cross(v);
        Vector3d rotated = new Vector3d(v).mul(cos)
            .add(cross.mul(sin))
            .add(new Vector3d(axisUnit).mul(dot * (1.0d - cos)));

        v.set(rotated);
    }

    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("LeftSignal", leftSignal);
        compound.putInt("RightSignal", rightSignal);
        compound.putFloat("TargetVectorX", targetVectorX);
        compound.putFloat("TargetVectorY", targetVectorY);
        compound.putFloat("CurrentVectorX", currentVectorX);
        compound.putFloat("CurrentVectorY", currentVectorY);
        compound.putFloat("ObstructionEfficiency", obstructionEfficiency);
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        leftSignal = compound.getInt("LeftSignal");
        rightSignal = compound.getInt("RightSignal");
        updateMappedTargets();
        targetVectorX = compound.contains("TargetVectorX") ? compound.getFloat("TargetVectorX") : targetVectorX;
        targetVectorY = compound.contains("TargetVectorY") ? compound.getFloat("TargetVectorY") : targetVectorY;
        currentVectorX = compound.contains("CurrentVectorX") ? compound.getFloat("CurrentVectorX") : targetVectorX;
        currentVectorY = compound.contains("CurrentVectorY") ? compound.getFloat("CurrentVectorY") : targetVectorY;
        obstructionEfficiency = compound.contains("ObstructionEfficiency") ? compound.getFloat("ObstructionEfficiency")
            : (OBSTRUCTION_LENGTH <= 0 ? 0.0f : Math.clamp((float) emptyBlocks / (float) OBSTRUCTION_LENGTH, 0.0f, 1.0f));
    }

    @Override
    public double getNozzleOffsetFromCenter() {
        return 0.5;
    }

    @Override
    protected double getBaseThrust() { return Math.min(PropulsionConfig.VECTOR_THRUSTER_BASE_THRUST.get(), this.getRawThrustCap()); }

    @Override
    protected double getRawThrustCap() { return PropulsionConfig.VECTOR_THRUSTER_MAX_THRUST.get(); }

    @Override
    protected ParticleOptions createParticleOptions() {
        return new IonParticleData(List.of(), null, 0.85f);
    }

    private static class VectorThrusterLinkTransform extends ValueBoxTransform.Dual {
        private final boolean rightSide;

        public VectorThrusterLinkTransform(boolean first, boolean rightSide) {
            super(first);
            this.rightSide = rightSide;
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            // Match the four Blockbench redstone link element centers:
            // [0]=west/top, [1]=west/bottom, [2]=east/top, [3]=east/bottom
            Vec3 local = VecHelper.voxelSpace(
                rightSide ? 14.6f : 1.4f,
                isFirst() ? 11f : 5f,
                2f
            );
            return rotatePointForFacing(local, state.getValue(AbstractThrusterBlock.FACING));
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Direction side = getWorldSide(state.getValue(AbstractThrusterBlock.FACING));
            float yRot = AngleHelper.horizontalAngle(side) + 180;
            float xRot = side == Direction.UP ? 90 : side == Direction.DOWN ? 270 : 0;
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
            ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot));
        }

        @Override
        public float getScale() {
            // Match Create's redstone-link frequency slot hitbox size for reliable targeting.
            return 0.4975f;
        }

        private Direction getWorldSide(Direction blockFacing) {
            Direction localSide = rightSide ? Direction.EAST : Direction.WEST;
            Vec3 sideNormal = Vec3.atLowerCornerOf(localSide.getNormal());
            Vec3 rotated = rotateDirectionForFacing(sideNormal, blockFacing);
            return Direction.getNearest(rotated.x, rotated.y, rotated.z);
        }

        private Vec3 rotatePointForFacing(Vec3 vec, Direction blockFacing) {
            return switch (blockFacing) {
                case NORTH -> vec;
                case EAST -> VecHelper.rotateCentered(vec, -90, Direction.Axis.Y);
                case SOUTH -> VecHelper.rotateCentered(vec, 180, Direction.Axis.Y);
                case WEST -> VecHelper.rotateCentered(vec, 90, Direction.Axis.Y);
                case UP -> VecHelper.rotateCentered(vec, 90, Direction.Axis.X);
                case DOWN -> VecHelper.rotateCentered(vec, -90, Direction.Axis.X);
            };
        }

        private Vec3 rotateDirectionForFacing(Vec3 vec, Direction blockFacing) {
            return switch (blockFacing) {
                case NORTH -> vec;
                case EAST -> VecHelper.rotate(vec, -90, Direction.Axis.Y);
                case SOUTH -> VecHelper.rotate(vec, 180, Direction.Axis.Y);
                case WEST -> VecHelper.rotate(vec, 90, Direction.Axis.Y);
                case UP -> VecHelper.rotate(vec, 90, Direction.Axis.X);
                case DOWN -> VecHelper.rotate(vec, -90, Direction.Axis.X);
            };
        }
    }
}
