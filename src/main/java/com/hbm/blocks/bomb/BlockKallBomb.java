// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.explosion.ExplosionNukeSmall.MukeParams;
import com.hbm.interfaces.IBomb;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.bomb.BlockEntityKallBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.Orientation;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockKallBomb extends BlockMultiblockCore implements EntityBlock, IBomb {

    /** Мультиблок 1х1х3 горизонтально: 0 вверх/вниз, 0 вперед/назад, 1 влево, 1 вправо */
    private static final int[] DIMENSIONS = {0, 0, 0, 0, 1, 1};

    public BlockKallBomb(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityKallBomb(pos, state);
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if (level.hasNeighborSignal(core) || level.hasNeighborSignal(cell)) {
            explode(level, core, null);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!(level instanceof ServerLevel server)) return BombReturnCode.UNDEFINED;
        if (!(server.getBlockEntity(pos) instanceof BlockEntityKallBomb bomb)) {
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        boolean hasLens1 = BlockEntityKallBomb.isLens(bomb.getItem(BlockEntityKallBomb.SLOT_LENS_1));
        boolean hasLens2 = BlockEntityKallBomb.isLens(bomb.getItem(BlockEntityKallBomb.SLOT_LENS_2));
        boolean hasLens3 = BlockEntityKallBomb.isLens(bomb.getItem(BlockEntityKallBomb.SLOT_LENS_3));
        boolean hasLens4 = BlockEntityKallBomb.isLens(bomb.getItem(BlockEntityKallBomb.SLOT_LENS_4));
        boolean hasIgniter = BlockEntityKallBomb.isIgniter(bomb.getItem(BlockEntityKallBomb.SLOT_IGNITER));

        ItemStack wasteStack = bomb.getItem(BlockEntityKallBomb.SLOT_WASTE);
        long wasteMb = BlockEntityKallBomb.getToxicWasteMb(wasteStack);

        boolean allLenses = hasLens1 && hasLens2 && hasLens3 && hasLens4;

        if (!allLenses || !hasIgniter || wasteMb <= 0) {
            notifyWrongComposition(server, pos, detonator, allLenses, hasIgniter, wasteMb);
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }

        long effectiveMb = Math.min(wasteMb, 64_000L);

        bomb.clearSlots();
        server.removeBlock(pos, false);

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        // Взрыв ядерной гранаты с масштабированием от объема отходов
        float ratio = (float) effectiveMb / 64_000F;
        MukeParams params = new MukeParams();
        params.miniNuke = true;
        params.blastRadius = 10F + ratio * 20F;
        params.killRadius = 25F + ratio * 35F;
        params.radiationLevel = 1.5F + ratio * 3.5F;
        params.shrapnelCount = 0;
        params.sound = false;
        ExplosionNukeSmall.explode(server, x, y, z, params);

        // Фоновый звук взрыва (громкость настроена ниже калл бомба)
        server.playSound(
                null,
                x,
                y,
                z,
                ModSounds.KALL_BOMB_EXPLOSION.get(),
                SoundSource.BLOCKS,
                50.0F,
                1.0F);

        // Звук калл бомба (звучит громче взрыва)
        server.playSound(
                null,
                x,
                y,
                z,
                ModSounds.KALL_BOMB.get(),
                SoundSource.BLOCKS,
                50.0F,
                1.0F);

        // Заполняем дно и склоны кратера токсичной грязью Ватцза
        fillCraterWithMud(server, pos, params.blastRadius);

        // Осколки Watz: в 10 раз больше (20..1280 штук) и идеально покрывают кольцо толщиной 50 блоков вокруг кратера
        int shrapnelCount = Math.max(20, (int) (effectiveMb / 50L));
        spawnWasteShrapnels(server, x, y, z, params.blastRadius, shrapnelCount);

        return BombReturnCode.DETONATED;
    }

    private void fillCraterWithMud(ServerLevel server, BlockPos center, float blastRadius) {
        int r = (int) Math.ceil(blastRadius);
        BlockState mudState = ModBlocks.MUD_BLOCK.get().defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        RandomSource rand = server.getRandom();

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double distSq = dx * dx + dz * dz;
                if (distSq > r * r) continue;
                double dist = Math.sqrt(distSq);

                // Сканируем сверху вниз от верхней точки кратера до дна
                cursor.set(cx + dx, Math.min(server.getMaxBuildHeight() - 1, cy + r / 2 + 2), cz + dz);
                while (cursor.getY() > server.getMinBuildHeight() + 1 && server.getBlockState(cursor).isAir()) {
                    cursor.move(Direction.DOWN);
                }

                BlockPos floor = cursor.immutable();
                BlockState floorState = server.getBlockState(floor);
                if (floorState.isAir() || floor.getY() <= server.getMinBuildHeight() + 1) continue;

                // Заполняем дно и склоны кратера токсичной грязью Ватцза
                if (dist <= r * 0.45) {
                    // Глубокий центр: сплошное токсичное озеро на дне
                    BlockPos above = floor.above();
                    if (server.getBlockState(above).canBeReplaced() || server.getBlockState(above).isAir()) {
                        server.setBlockAndUpdate(above, mudState);
                    }
                    if (rand.nextBoolean()) {
                        server.setBlockAndUpdate(floor, mudState);
                    }
                } else if (dist <= r * 0.8) {
                    // Склоны кратера: густые потёки грязи (70%)
                    if (rand.nextFloat() < 0.7F) {
                        BlockPos above = floor.above();
                        if (server.getBlockState(above).canBeReplaced() || server.getBlockState(above).isAir()) {
                            server.setBlockAndUpdate(above, mudState);
                        }
                    }
                } else {
                    // Края кратера: брызги (35%)
                    if (rand.nextFloat() < 0.35F) {
                        BlockPos above = floor.above();
                        if (server.getBlockState(above).canBeReplaced() || server.getBlockState(above).isAir()) {
                            server.setBlockAndUpdate(above, mudState);
                        }
                    }
                }
            }
        }
    }

    private void notifyWrongComposition(
            ServerLevel server,
            BlockPos pos,
            @Nullable Entity detonator,
            boolean allLenses,
            boolean hasIgniter,
            long wasteMb) {
        StringBuilder sb = new StringBuilder("§c[Kall Bomb] Ошибка детонации: ");
        if (!allLenses) {
            sb.append("требуется 4 взрывных линзы! ");
        }
        if (!hasIgniter) {
            sb.append("требуется детонатор (Igniter)! ");
        }
        if (wasteMb <= 0) {
            sb.append("требуется контейнер с токсичными отходами (watz_mud / wastefluid)! ");
        }
        Component message = Component.literal(sb.toString().trim());
        if (detonator instanceof Player player) {
            player.displayClientMessage(message, false);
        } else {
            AABB notifyBox = new AABB(pos).inflate(32);
            for (Player player : server.getEntitiesOfClass(Player.class, notifyBox)) {
                player.displayClientMessage(message, false);
            }
        }
    }

    private void spawnWasteShrapnels(
            ServerLevel server, double x, double y, double z, float craterRadius, int count) {
        RandomSource rand = server.getRandom();
        double rMin = Math.max(5.0, (double) craterRadius);
        double rMax = rMin + 50.0; // Кольцо толщиной ровно 50 блоков вокруг кратера

        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel = new EntityShrapnel(server, x, y + 1.0, z);

            // Равномерное распределение по площади кольца [rMin, rMax]
            double u = rand.nextDouble();
            double targetDist = Math.sqrt(rMin * rMin + u * (rMax * rMax - rMin * rMin));
            double angle = rand.nextDouble() * Math.PI * 2.0;

            // Баллистическая траектория с учётом гравитации (0.03) и сопротивления воздуха (0.99)
            double vy = 0.7 + 0.4 * rand.nextDouble();
            double ratio = 16.0 + 30.0 * vy;
            double vh = targetDist / ratio;

            double vx = vh * Math.cos(angle);
            double vz = vh * Math.sin(angle);

            shrapnel.setDeltaMovement(vx, vy, vz);
            shrapnel.setTrail(EntityShrapnel.TRAIL_WATZ);
            server.addFreshEntity(shrapnel);
        }
    }
}
