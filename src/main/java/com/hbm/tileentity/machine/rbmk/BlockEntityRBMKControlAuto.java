// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.container.MenuRBMKControlAuto;
import com.hbm.packet.SyncField;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.Nbt;

public class BlockEntityRBMKControlAuto extends BlockEntityRBMKControl
        implements MenuProvider, ICopiable {

    @SyncField(units = 1L << 12)
    public RBMKFunction function = RBMKFunction.LINEAR;

    @SyncField(units = 1L << 8)
    public double levelLower;

    @SyncField(units = 1L << 9)
    public double levelUpper;

    @SyncField(units = 1L << 10)
    public double heatLower;

    @SyncField(units = 1L << 11)
    public double heatUpper;

    public BlockEntityRBMKControlAuto(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONTROL_AUTO.get(), pos, state);
    }

    @Override
    public void tickServer() {
        double fauxLevel;
        double lowerBound = Math.min(this.heatLower, this.heatUpper);
        double upperBound = Math.max(this.heatLower, this.heatUpper);

        if (this.heat < lowerBound) {
            fauxLevel = this.levelLower;
        } else if (this.heat > upperBound) {
            fauxLevel = this.levelUpper;
        } else {
            fauxLevel =
                    switch (this.function) {
                        case LINEAR ->
                                (this.heat - this.heatLower)
                                                * ((this.levelUpper - this.levelLower)
                                                        / (this.heatUpper - this.heatLower))
                                        + this.levelLower;
                        case QUAD_UP ->
                                Math.pow(
                                                        (this.heat - this.heatLower)
                                                                / (this.heatUpper - this.heatLower),
                                                        2)
                                                * (this.levelUpper - this.levelLower)
                                        + this.levelLower;
                        case QUAD_DOWN ->
                                Math.pow(
                                                        (this.heat - this.heatUpper)
                                                                / (this.heatLower - this.heatUpper),
                                                        2)
                                                * (this.levelLower - this.levelUpper)
                                        + this.levelUpper;
                    };
        }

        this.targetLevel = Math.clamp(fauxLevel * 0.01D, 0D, 1D);

        super.tickServer();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Nbt.getInt(data, "function").isPresent()) {
            int c = Math.abs(Nbt.getIntOr(data, "function", 0)) % RBMKFunction.values().length;
            this.function = RBMKFunction.values()[c];
        } else {
            Nbt.getDouble(data, "levelLower").ifPresent(v -> levelLower = v);
            Nbt.getDouble(data, "levelUpper").ifPresent(v -> levelUpper = v);
            Nbt.getDouble(data, "heatLower").ifPresent(v -> heatLower = v);
            Nbt.getDouble(data, "heatUpper").ifPresent(v -> heatUpper = v);
        }
        markChanged();
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.CONTROL_AUTO;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        levelLower = input.getDoubleOr("levelLower", levelLower);
        levelUpper = input.getDoubleOr("levelUpper", levelUpper);
        heatLower = input.getDoubleOr("heatLower", heatLower);
        heatUpper = input.getDoubleOr("heatUpper", heatUpper);
        function =
                RBMKFunction.values()[
                        Math.clamp(
                                input.getIntOr("function", function.ordinal()),
                                0,
                                RBMKFunction.values().length - 1)];
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("levelLower", levelLower);
        output.putDouble("levelUpper", levelUpper);
        output.putDouble("heatLower", heatLower);
        output.putDouble("heatUpper", heatUpper);
        output.putInt("function", function.ordinal());
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        tag.putDouble("levelLower", levelLower);
        tag.putDouble("levelUpper", levelUpper);
        tag.putDouble("heatLower", heatLower);
        tag.putDouble("heatUpper", heatUpper);
        tag.putInt("function", function.ordinal());
    }

    private void writeFunction(ByteBuf output) {
        output.writeByte(function.ordinal());
    }

    private void readFunction(ByteBuf input) {
        int ordinal = input.readUnsignedByte();
        if (ordinal >= RBMKFunction.values().length)
            throw new DecoderException("Invalid RBMK function");
        function = RBMKFunction.values()[ordinal];
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putDouble("levelLower", levelLower);
        data.putDouble("levelUpper", levelUpper);
        data.putDouble("heatLower", heatLower);
        data.putDouble("heatUpper", heatUpper);
        data.putInt("function", function.ordinal());
        return data;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        Nbt.getDouble(nbt, "levelLower").ifPresent(v -> levelLower = v);
        Nbt.getDouble(nbt, "levelUpper").ifPresent(v -> levelUpper = v);
        Nbt.getDouble(nbt, "heatLower").ifPresent(v -> heatLower = v);
        Nbt.getDouble(nbt, "heatUpper").ifPresent(v -> heatUpper = v);
        if (Nbt.getInt(nbt, "function").isPresent()) {
            int f = Nbt.getIntOr(nbt, "function", 0);
            function = RBMKFunction.values()[Math.abs(f % RBMKFunction.values().length)];
        }

        markChanged();
        networkPackNT(50);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkControlAuto");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuRBMKControlAuto(id, inv, this);
    }

    public enum RBMKFunction {
        LINEAR,
        QUAD_UP,
        QUAD_DOWN
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1f00L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 8 -> output.writeDouble(this.levelLower);
            case 9 -> output.writeDouble(this.levelUpper);
            case 10 -> output.writeDouble(this.heatLower);
            case 11 -> output.writeDouble(this.heatUpper);
            case 12 -> writeFunction(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 8 -> this.levelLower = input.readDouble();
            case 9 -> this.levelUpper = input.readDouble();
            case 10 -> this.heatLower = input.readDouble();
            case 11 -> this.heatUpper = input.readDouble();
            case 12 -> readFunction(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
