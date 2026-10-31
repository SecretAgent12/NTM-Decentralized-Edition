// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.backport.SubLevelConveyor;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.capability.NtmContracts;
import com.hbm.config.NeoForgeHbmConfig;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import com.hbm.backport.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.EntityCompat;

public abstract class EntityMovingConveyorObject extends EntityCompat {

    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    /** Where the object was last on a build, in that build's coordinates; null in the world. */
    private @Nullable Vec3 onBuild;

    // Sable compat: the place on a build, for the client to draw the object where the build is drawn
    // (the world position from the server arrives late and lags behind a moving build). Fixed point,
    // 1/4096 block: plot coordinates are too large for a float.
    private static final double LOCAL_SCALE = 0x1p12;
    private static final EntityDataAccessor<Boolean> RIDING =
            SynchedEntityData.defineId(EntityMovingConveyorObject.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> LOCAL_X =
            SynchedEntityData.defineId(EntityMovingConveyorObject.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> LOCAL_Y =
            SynchedEntityData.defineId(EntityMovingConveyorObject.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> LOCAL_Z =
            SynchedEntityData.defineId(EntityMovingConveyorObject.class, EntityDataSerializers.LONG);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RIDING, false);
        builder.define(LOCAL_X, 0L);
        builder.define(LOCAL_Y, 0L);
        builder.define(LOCAL_Z, 0L);
    }

    private void publishPlace(@Nullable Vec3 local) {
        entityData.set(RIDING, local != null);
        if (local == null) return;
        entityData.set(LOCAL_X, Math.round(local.x * LOCAL_SCALE));
        entityData.set(LOCAL_Y, Math.round(local.y * LOCAL_SCALE));
        entityData.set(LOCAL_Z, Math.round(local.z * LOCAL_SCALE));
    }

    /**
     * Client: put the object where its place on the build is now drawn. Each tick from the build's
     * pose; between ticks the usual position interpolation follows the build the same way.
     */
    private void followBuild() {
        Vec3 local =
                new Vec3(
                        entityData.get(LOCAL_X) / LOCAL_SCALE,
                        entityData.get(LOCAL_Y) / LOCAL_SCALE,
                        entityData.get(LOCAL_Z) / LOCAL_SCALE);
        var frame = dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(level(), local);
        if (frame == null) return;
        Vec3 world = frame.logicalPose().transformPosition(local);
        setPos(world.x, world.y, world.z);
    }

    protected EntityMovingConveyorObject(
            EntityType<? extends EntityMovingConveyorObject> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player) discard();
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        skipAttackInteraction(source.getEntity());
        return true;
    }

    @Override
    public void tick() {
        boolean riding = level().isClientSide() && entityData.get(RIDING);
        if (!riding) this.interpolation.interpolate();
        super.tick();

        if (riding) followBuild();
        if (!(level() instanceof ServerLevel server)) return;

        if (this.tickCount <= 5) return;

        if ((this.tickCount + getId()) % 400 == 0) cramCheck(server);

        // Sable compat: belts on a physics build are looked at in the build's space, the object
        // itself stays in the world (SubLevelConveyor). Without a build this is the plain world.
        SubLevelConveyor.Place place = SubLevelConveyor.find(server, position(), onBuild);
        BlockPos pos = place.block();
        Vec3 self = place.local();
        IConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(server, pos);

        if (belt == null || !belt.canItemStay(server, pos, self)) {
            onBuild = null;
            publishPlace(null);
            if (onLeaveConveyor()) return;
        } else {
            Vec3 next = belt.getTravelLocation(server, pos, self, getMoveSpeed());
            setDeltaMovement(place.toWorld(next).subtract(position()));
        }

        BlockPos lastPos = pos;
        move(MoverType.SELF, getDeltaMovement());
        SubLevelConveyor.Place moved = place.at(position());
        onBuild = moved.onBuild() ? moved.local() : null;
        publishPlace(onBuild);
        BlockPos newPos = moved.block();

        if (lastPos.equals(newPos)) return;

        IEnterableBlock entered = NtmContracts.ENTERABLE.at(server, newPos);

        if (entered != null) {
            enterBlock(entered, newPos, enterDirection(lastPos, newPos));
        } else if (!server.getBlockState(newPos).isSolid()) {
            IEnterableBlock below = NtmContracts.ENTERABLE.at(server, newPos.below());
            if (below != null) enterBlockFalling(below, newPos);
        }
    }

    private static @Nullable Direction enterDirection(BlockPos from, BlockPos to) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dy = Integer.signum(to.getY() - from.getY());
        int dz = Integer.signum(to.getZ() - from.getZ());

        if (dx != 0 && dy == 0 && dz == 0) return dx < 0 ? Direction.EAST : Direction.WEST;
        if (dy != 0 && dx == 0 && dz == 0) return dy < 0 ? Direction.UP : Direction.DOWN;
        if (dz != 0 && dx == 0 && dy == 0) return dz < 0 ? Direction.SOUTH : Direction.NORTH;
        return null;
    }

    private void cramCheck(ServerLevel server) {
        List<EntityMovingConveyorObject> objs =
                server.getEntitiesOfClass(
                        EntityMovingConveyorObject.class, getBoundingBox().inflate(0.125D));

        if (objs.size() < NeoForgeHbmConfig.runtime().conveyorCramMax()) return;

        for (EntityMovingConveyorObject obj : objs) obj.discard();

        ExplosionVNT vnt = new ExplosionVNT(server, getX(), getY() + 0.125, getZ(), 1, this);
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();

        BlockPos pos = blockPosition();
        if (tickCount > 400
                && NeoForgeHbmConfig.runtime().conveyorCramExplode()
                && server.getBlockState(pos).getBlock() instanceof IConveyorBelt)
            server.destroyBlock(pos, false);
    }

    public abstract void enterBlock(
            IEnterableBlock enterable, BlockPos pos, @Nullable Direction dir);

    public void enterBlockFalling(IEnterableBlock enterable, BlockPos pos) {
        this.enterBlock(enterable, pos.below(), Direction.UP);
    }

    public abstract boolean onLeaveConveyor();

    public double getMoveSpeed() {
        return 0.0625D;
    }
}
