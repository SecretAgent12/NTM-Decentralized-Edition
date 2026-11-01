// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.create.motor;

import com.hbm.items.CreativeContents;
import com.hbm.lib.Library;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.simibubi.create.api.stress.BlockStressValues;
import java.util.Set;
import java.util.function.DoubleSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Create integration (our own, not in ntm-next or 1.7.10): registration of the electric motors.
 * Only loaded with Create installed ({@link #register}, from the mod constructor); without Create
 * the motors don't exist, and their recipes and loot tables are skipped by their conditions.
 */
public final class CreateMotors {

    public static final RegistryHandle<BlockCreateMotor> MOTOR =
            Reg.blockItem(
                    "create_motor",
                    props -> new BlockCreateMotor(props, MotorSpec.STARTER),
                    CreateMotors::properties);
    public static final RegistryHandle<BlockCreateMotor> MOTOR_DESH =
            Reg.blockItem(
                    "create_motor_desh",
                    props -> new BlockCreateMotor(props, MotorSpec.DESH),
                    CreateMotors::properties);
    public static final Reg.BeHandle<BlockEntityCreateMotor> MOTOR_BE =
            Reg.be(
                    "create_motor",
                    () ->
                            new BlockEntityType<>(
                                    (pos, state) ->
                                            new BlockEntityCreateMotor(
                                                    CreateMotors.MOTOR_BE.get(), pos, state),
                                    Set.<Block>of(MOTOR.get(), MOTOR_DESH.get()),
                                    null));

    static {
        // after NTM's electric press in the machine tab, in this order
        {
            ResourceLocation press = Library.id("machine_epress");
            CreativeContents.insertAfter(
                    CreativeContents.Tab.MACHINES, press, () -> new ItemStack(MOTOR));
            CreativeContents.insertAfter(
                    CreativeContents.Tab.MACHINES, press, () -> new ItemStack(MOTOR_DESH));
        }
    }

    private CreateMotors() {}

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(0x1.4p2F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .noOcclusion();
    }

    /**
     * Loads this class, and with it the registrations above; call from the mod constructor, only
     * with Create loaded. Also tells Create what the motors give, for its kinetic stats in item
     * tooltips; asked lazily, so datapack values are the ones shown.
     */
    public static void register() {
        BlockStressValues.CAPACITIES.registerProvider(
                block ->
                        block instanceof BlockCreateMotor motor
                                ? (DoubleSupplier) motor.spec::stressPerRpm
                                : null);
        BlockStressValues.RPM.registerProvider(
                block ->
                        block instanceof BlockCreateMotor motor
                                ? new BlockStressValues.GeneratedRpm(motor.spec.rpm(), false)
                                : null);
    }
}
