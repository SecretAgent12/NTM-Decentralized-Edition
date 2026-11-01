// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.create.motor;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import java.util.List;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Create integration (our own, not in ntm-next or 1.7.10): client side of the electric motors.
 * Called from {@code NuclearTechNeoForgeClient}'s constructor, only with Create loaded.
 *
 * <p>Item tooltips are Create's own: "Hold [Shift] for Summary" (lang keys {@code
 * block.hbm.create_motor*.tooltip.*}), then Create's kinetic stats (speed and stress capacity,
 * from {@link CreateMotors#register}), then the motor's HE consumption in the same style.
 */
public final class CreateMotorsClient {

    private CreateMotorsClient() {}

    public static void register(IEventBus modBus) {
        if (modBus == null) return;
        // the shaft partials must exist before Flywheel collects them for baking
        CreateMotorRenderer.init();
        modBus.addListener(CreateMotorsClient::bindShaftRenderer);
        modBus.addListener(CreateMotorsClient::bindTooltips);
    }

    private static void bindShaftRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateMotors.MOTOR_BE.get(), CreateMotorRenderer::new);
    }

    private static void bindTooltips(FMLClientSetupEvent event) {
        List<Item> motors =
                List.of(CreateMotors.MOTOR.get().asItem(), CreateMotors.MOTOR_DESH.get().asItem());
        for (Item item : motors)
            TooltipModifier.REGISTRY.register(
                    item,
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
                            .andThen(CreateMotorsClient::energyStats));
    }

    /** HE at full load and idle, laid out like Create's stress lines above it. */
    private static void energyStats(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof BlockCreateMotor motor)) return;
        List<Component> lines = event.getToolTip();
        lines.add(
                Component.translatable("info.hbm.create_motor.energy")
                        .withStyle(ChatFormatting.GRAY));
        lines.add(line("info.hbm.create_motor.full_load", motor.spec.fullLoadHe()));
        lines.add(line("info.hbm.create_motor.idle", motor.spec.idleHe()));
    }

    private static Component line(String key, double perTick) {
        Component value =
                Component.literal(BlockCreateMotor.he(perTick)).withStyle(ChatFormatting.AQUA);
        return Component.literal(" ")
                .append(Component.translatable(key, value))
                .withStyle(ChatFormatting.DARK_GRAY);
    }
}
