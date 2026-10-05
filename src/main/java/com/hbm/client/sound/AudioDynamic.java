// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.sound;

import com.hbm.backport.SubLevelSpace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AudioDynamic extends AbstractTickableSoundInstance {

    public float maxVolume = 1F;
    public float range = 10F;
    public int keepAlive;
    public int timeSinceKA;
    public boolean shouldExpire;
    public @Nullable Entity parentEntity;

    // backport-fix: BF-061 — game time of the last tick the sound engine gave this sound, and of
    // start(); see isPlaying().
    private long lastTicked = -100L;
    private long startedAt = -100L;

    protected AudioDynamic(SoundEvent sound, SoundSource category) {
        super(sound, category, RandomSource.create());
        looping = true;
        attenuation = SoundInstance.Attenuation.NONE;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void attachTo(Entity entity) {
        parentEntity = entity;
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        lastTicked = gameTime();

        if (parentEntity != null && player != parentEntity) {
            setPosition(
                    (float) parentEntity.getX(),
                    (float) parentEntity.getY(),
                    (float) parentEntity.getZ());
        }

        if (player != null && player != parentEntity) {
            volume = volumeAtDistance(distanceTo(player));
        } else {
            if (player != null && player == parentEntity) {
                setPosition(
                        (float) parentEntity.getX(),
                        (float) parentEntity.getY() + 10F,
                        (float) parentEntity.getZ());
            }
            volume = maxVolume;
        }

        if (shouldExpire) {
            if (timeSinceKA > keepAlive) {
                stop();
            }
            timeSinceKA++;
        }
    }

    /**
     * backport-fix: BF-061 — distance to where the sound is drawn. A machine on a physics
     * contraption (Sable sub-level) sits in a plot millions of blocks away; Sable moves the sound
     * itself to the contraption, but this volume (no vanilla attenuation) was worked out from the
     * plot position, so it was always 0.
     */
    private float distanceTo(LocalPlayer player) {
        Vec3 at = SubLevelSpace.toWorld(player.level(), x, y, z);
        double dx = at.x - player.getX();
        double dy = at.y - player.getEyeY();
        double dz = at.z - player.getZ();
        return (float) Mth.length(dx, dy, dz);
    }

    private static long gameTime() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0L;
    }

    public void start() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();

        if (!manager.isActive(this)) {
            startedAt = gameTime();
            manager.play(this);
        }
    }

    public void stopSound() {
        stop();
        Minecraft.getInstance().getSoundManager().stop(this);
    }

    public void setVolume(float volume) {
        maxVolume = volume;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
        shouldExpire = true;
    }

    public void keepAlive() {
        timeSinceKA = 0;
    }

    public float volumeAtDistance(float distance) {
        return (distance / range) * -maxVolume + maxVolume;
    }

    /**
     * backport-fix: BF-061 — Sable plays a sound started in a sub-level plot through its own wrapper,
     * so the sound engine only knows the wrapper and {@code isActive(this)} is false: the machine
     * restarted its loop every tick. The wrapper still ticks this sound, so a sound the engine
     * ticked within the last few ticks (or just started) counts as playing.
     */
    public boolean isPlaying() {
        if (Minecraft.getInstance().getSoundManager().isActive(this)) return true;
        if (isStopped()) return false;
        long now = gameTime();
        return now - lastTicked <= 3 || now - startedAt <= 3;
    }

    public float rawVolume() {
        return volume;
    }

    public float rawPitch() {
        return pitch;
    }
}
