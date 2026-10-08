// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.backport.client.gui.ContainerScreenCompat;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.rendertype.RenderPipelines;
import com.hbm.inventory.container.MenuKallBomb;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.tileentity.bomb.BlockEntityKallBomb;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI для Kall Bomb.
 * Текстура: textures/gui/weapon/kall_bomb_gui.png
 * Сетка и визуальное отображение линз и статуса готовности аналогичны Толстяку (Fat Man).
 */
public class ScreenKallBomb extends ContainerScreenCompat<MenuKallBomb> {

    private static final ResourceLocation TEXTURE =
            Library.id("textures/gui/weapon/kall_bomb_gui.png");

    public ScreenKallBomb(MenuKallBomb menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
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

        if (lens(BlockEntityKallBomb.SLOT_LENS_1)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 19, 176.0F, 0.0F, 24, 24, 256, 256);
        }
        if (lens(BlockEntityKallBomb.SLOT_LENS_2)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 106, 19, 200.0F, 0.0F, 24, 24, 256, 256);
        }
        if (lens(BlockEntityKallBomb.SLOT_LENS_3)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 43, 176.0F, 24.0F, 24, 24, 256, 256);
        }
        if (lens(BlockEntityKallBomb.SLOT_LENS_4)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    106,
                    43,
                    200.0F,
                    24.0F,
                    24,
                    24,
                    256,
                    256);
        }
        if (menu.isReady()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    134,
                    35,
                    176.0F,
                    48.0F,
                    16,
                    16,
                    256,
                    256);
        }

        graphics.text(font, title, (imageWidth - font.width(title)) / 2, 6, -12566464, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 96 + 2, -12566464, false);
    }

    private boolean lens(int slot) {
        return BlockEntityKallBomb.isLens(menu.part(slot));
    }
}
