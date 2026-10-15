// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.blockentity.network.BlockEntityDroneRequester;
import com.hbm.inventory.container.MenuDroneRequester;
import com.hbm.lib.Library;
import java.util.List;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.rendertype.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ScreenDroneRequester extends ScreenInfoContainer<MenuDroneRequester> {

    private static final ResourceLocation TEXTURE =
            Library.id("textures/gui/storage/gui_drone_requester.png");

    public ScreenDroneRequester(MenuDroneRequester menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected List<Component> slotTooltip(Slot slot) {
        return slot.index < BlockEntityDroneRequester.FILTER_COUNT
                ? patternModeLines(menu.blockEntity().matcher.mode(slot.index))
                : List.of();
    }
}
