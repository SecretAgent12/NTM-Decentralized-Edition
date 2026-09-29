// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuFileCabinet;
import com.hbm.lib.Library;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.hbm.backport.client.rendertype.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.hbm.backport.client.gui.ContainerScreenCompat;

public class ScreenFileCabinet extends ContainerScreenCompat<MenuFileCabinet> {

    private static final ResourceLocation TEXTURE =
            Library.id("textures/gui/storage/gui_file_cabinet.png");

    public ScreenFileCabinet(MenuFileCabinet menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 170);
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

        graphics.text(font, title, (imageWidth - font.width(title)) / 2, 6, -12566464, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 96 + 2, -12566464, false);
    }
}
