package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.particles.plasma.PlasmaParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plume.PlumeParticleData;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.utility.math.MathUtility;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class ThrusterParticles {
    private static final float PARTICLE_VELOCITY = 4.0f;

    private ThrusterParticles() {
    }

    public static void spawn(final ThrusterBlockEntity blockEntity) {
        if (!blockEntity.shouldEmitParticles()) {
            return;
        }

        final Direction exhaust = blockEntity.getFacing().getOpposite();
        final double nozzleOffset = blockEntity.getNozzleOffsetFromCenter();
        final int stepX = exhaust.getStepX();
        final int stepY = exhaust.getStepY();
        final int stepZ = exhaust.getStepZ();
        final double baseX = blockEntity.getBlockPos().getX() + 0.5d;
        final double baseY = blockEntity.getBlockPos().getY() + 0.5d;
        final double baseZ = blockEntity.getBlockPos().getZ() + 0.5d;
        final Vector3d localNozzle = new Vector3d(
                baseX + stepX * nozzleOffset,
                baseY + stepY * nozzleOffset,
                baseZ + stepZ * nozzleOffset
        );

        final SimulatedThrustAdapter.Projection projection = SimulatedThrustAdapter.projectToWorld(
                blockEntity.getLevel(),
                blockEntity.getBlockPos(),
                localNozzle,
                new Vector3d(stepX, stepY, stepZ)
        );

        final Level level = projection.level();
        final Vec3 basePos = projection.position();
        final Vec3 dir = projection.direction().normalize();
        final float throttle = (float) blockEntity.getThrottle();
        final float emissionScale = Math.max(throttle, (float) MathUtility.epsilon);
        final double speed = PARTICLE_VELOCITY * emissionScale;
        final int maxCap = PropulsionConfig.CLIENT_PARTICLES_PER_TICK.get();
        if (maxCap <= 0) {
            return;
        }
        final int densityCount = Math.max(1, (int) Math.ceil(speed / AbstractThrusterBlockEntity.TARGET_PARTICLE_SPACING_BLOCKS));
        final int count = Math.min(maxCap, densityCount);

        for (int i = 0; i < count; i++) {
            final double beamFrac = count <= 1 ? 0.0 : (double) i / (double) count;
            final double px = basePos.x + dir.x * speed * beamFrac;
            final double py = basePos.y + dir.y * speed * beamFrac;
            final double pz = basePos.z + dir.z * speed * beamFrac;
            final double vx = dir.x * speed;
            final double vy = dir.y * speed;
            final double vz = dir.z * speed;
            final ParticleOptions particle = blockEntity.isIon()
                    ? new dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData()
                    : (blockEntity.isCreative()
                    ? switch (blockEntity.getPlumeType()) {
                        case PLASMA -> new PlasmaParticleData();
                        case ION -> new dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData();
                        case PLUME, NONE -> new PlumeParticleData();
                    }
                    : new PlumeParticleData());

            if (level instanceof final ClientLevel clientLevel) {
                clientLevel.addParticle(particle,
                        true, px, py, pz, vx, vy, vz);
            } else {
                level.addParticle(particle,
                        px, py, pz, vx, vy, vz);
            }
        }
    }
}


