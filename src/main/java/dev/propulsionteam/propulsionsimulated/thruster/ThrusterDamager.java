package dev.propulsionteam.propulsionsimulated.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.debug.PropulsionDebug;
import dev.propulsionteam.propulsionsimulated.debug.routes.MainDebugRoute;
import dev.propulsionteam.propulsionsimulated.utility.AbstractAreaDamagerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.awt.Color;
import java.util.Optional;

public class ThrusterDamager extends AbstractAreaDamagerBehaviour {
    private record ThrusterDamageContext(Vec3 nozzlePos, float visualPowerPercent) {}

    public ThrusterDamager(SmartBlockEntity be) {
        super(be);
    }

    private AbstractThrusterBlockEntity getThruster() {
        return (AbstractThrusterBlockEntity) this.blockEntity;
    }

    @Override
    protected int getTickFrequency() {
        return 5;
    }

    @Override
    protected boolean shouldDamage() {
        return PropulsionConfig.THRUSTER_DAMAGE_ENTITIES.get()
            && getThruster().isPowered()
            && getThruster().isWorking();
    }

    @Override
    protected DamageSource getDamageSource() {
        return getWorld().damageSources().onFire();
    }

    @Override
    protected Optional<DamageZone> calculateDamageZone() {
        AbstractThrusterBlockEntity thruster = getThruster();
        Direction plumeDirection = thruster.getBlockState().getValue(AbstractThrusterBlock.FACING).getOpposite();

        float currentPower = thruster.getPower();
        float threshold = AbstractThrusterBlockEntity.LOWEST_POWER_THRESHOLD;
        if (currentPower < threshold) return Optional.empty();

        float visualPowerPercent = Math.max(0, currentPower - threshold) / (1.0f - threshold);

        float distanceByPower = Math.lerp(0.55f, 1.5f, visualPowerPercent);
        double potentialPlumeLength = thruster.getEmptyBlocks() * distanceByPower;
        
        Vec3 nozzlePos = getNozzlePosInWorld(plumeDirection);
        Vec3 worldPlumeDirection = getWorldPlumeDirection(plumeDirection);

        double correctedPlumeLength = performRaycastCheck(nozzlePos, worldPlumeDirection, potentialPlumeLength);
        if (correctedPlumeLength <= 0.01) {
            return Optional.empty();
        }

        Vec3 boxDimensions = new Vec3(1.4, 1.4, correctedPlumeLength);
        double plumeStartOffset = 0.8;
        double centerOffsetDistance = plumeStartOffset + (correctedPlumeLength / 2.0);
        Vec3 boxOffset = Vec3.atLowerCornerOf(plumeDirection.getNormal()).scale(centerOffsetDistance);
        
        ThrusterDamageContext context = new ThrusterDamageContext(nozzlePos, visualPowerPercent);

        return Optional.of(new DamageZone(boxDimensions, boxOffset, plumeDirection, Direction.SOUTH, context));
    }

    @Override
    protected void applyDamage(LivingEntity entity, DamageSource source, DamageZone zone) {
        ThrusterDamageContext context = (ThrusterDamageContext) zone.context();
        
        float invSqrDistance = (context.visualPowerPercent() * 15.0f) * 8.0f / (float)java.lang.Math.max(1, entity.position().distanceToSqr(context.nozzlePos()));
        float damageAmount = 3 + invSqrDistance;

        entity.igniteForSeconds(3);
        entity.hurt(source, damageAmount);
    }

    @Override
    protected boolean shouldDebug() {
        return PropulsionDebug.isDebug(MainDebugRoute.THRUSTER);
    }

    @Override
    protected Color getDebugColor() {
        return Color.ORANGE;
    }

    private Vec3 getNozzlePosInWorld(Direction plumeDirection) {
        BlockPos worldPosition = getThruster().getBlockPos();
        Vector3d thrusterCenterBlockShipCoords = new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);

        Quaterniond relativeRotation = new Quaterniond().rotateTo(new Vector3d(0, 0, 1), new Vector3d(plumeDirection.getStepX(), plumeDirection.getStepY(), plumeDirection.getStepZ()));

        Vector3d thrusterCenterBlockWorld;
        Quaterniond obbRotationWorld;

        thrusterCenterBlockWorld = thrusterCenterBlockShipCoords;
        obbRotationWorld = relativeRotation;

        Vector3d nozzleOffsetLocal = new Vector3d(0, 0, 0.5);
        Vector3d nozzleOffsetWorld = obbRotationWorld.transform(nozzleOffsetLocal, new Vector3d());
        Vector3d thrusterNozzleWorldPos = thrusterCenterBlockWorld.add(nozzleOffsetWorld, new Vector3d());
        
        return new Vec3(thrusterNozzleWorldPos.x, thrusterNozzleWorldPos.y, thrusterNozzleWorldPos.z);
    }

    private Vec3 getWorldPlumeDirection(Direction plumeDirection) {
        Vector3d localPlumeVec = new Vector3d(plumeDirection.getStepX(), plumeDirection.getStepY(), plumeDirection.getStepZ());
        return new Vec3(localPlumeVec.x, localPlumeVec.y, localPlumeVec.z);
    }


    private double performRaycastCheck(Vec3 nozzlePos, Vec3 worldPlumeDirection, double maxDistance) {
        Level level = getThruster().getLevel();
        Vec3 endPos = nozzlePos.add(worldPlumeDirection.scale(maxDistance));

        var clipContext = new net.minecraft.world.level.ClipContext(
            nozzlePos,
            endPos,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            net.minecraft.world.phys.shapes.CollisionContext.empty()
        );

        net.minecraft.world.phys.BlockHitResult hitResult = level.clip(clipContext);

        if (hitResult.getType() == net.minecraft.world.phys.BlockHitResult.Type.BLOCK) {
            return nozzlePos.distanceTo(hitResult.getLocation());
        }

        return maxDistance;
    }
}
