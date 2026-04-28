package dev.createpropulsionsimulated.client.sound;

import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ThrusterLoopSoundController {
    private static final Map<String, ThrusterLoopSoundInstance> ACTIVE_SOUNDS = new HashMap<>();

    private ThrusterLoopSoundController() {
    }

    public static void tick(final AbstractThrusterBlockEntity blockEntity) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.getSoundManager() == null) {
            return;
        }

        final String key = soundKey(blockEntity);
        final boolean active = shouldPlay(blockEntity);
        final ThrusterLoopSoundInstance existing = ACTIVE_SOUNDS.get(key);

        if (!active) {
            if (existing != null) {
                existing.halt();
                ACTIVE_SOUNDS.remove(key);
            }
            return;
        }

        if (existing == null || existing.isStopped()) {
            final ThrusterLoopSoundInstance instance = new ThrusterLoopSoundInstance(blockEntity);
            ACTIVE_SOUNDS.put(key, instance);
            minecraft.getSoundManager().play(instance);
        }

        cleanupStopped();
    }

    private static void cleanupStopped() {
        final Iterator<Map.Entry<String, ThrusterLoopSoundInstance>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isStopped()) {
                iterator.remove();
            }
        }
    }

    private static boolean shouldPlay(final AbstractThrusterBlockEntity blockEntity) {
        return blockEntity.getLevel() != null
                && blockEntity.getLevel().isClientSide()
                && !blockEntity.isRemoved()
                && blockEntity.isVisuallyActive();
    }

    private static String soundKey(final AbstractThrusterBlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        final ResourceKey<Level> dimension = level != null ? level.dimension() : Level.OVERWORLD;
        final BlockPos pos = blockEntity.getBlockPos();
        return dimension.location() + "|" + pos.asLong();
    }

    private static final class ThrusterLoopSoundInstance extends AbstractTickableSoundInstance {
        private final AbstractThrusterBlockEntity blockEntity;

        private ThrusterLoopSoundInstance(final AbstractThrusterBlockEntity blockEntity) {
            super(PropulsionSoundEvents.THRUSTER_LOOP.get(), SoundSource.BLOCKS, RandomSource.create());
            this.blockEntity = blockEntity;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            updateFromBlockEntity();
        }

        @Override
        public void tick() {
            if (!shouldPlay(this.blockEntity)) {
                stop();
                return;
            }
            updateFromBlockEntity();
        }

        private void updateFromBlockEntity() {
            final BlockPos pos = this.blockEntity.getBlockPos();
            final float power = Math.max(this.blockEntity.getPower(), 5.0f / 15.0f);
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
            this.volume = 0.2f + (0.35f * power);
            this.pitch = 0.85f + (0.25f * power);
        }

        private void halt() {
            super.stop();
        }
    }
}
