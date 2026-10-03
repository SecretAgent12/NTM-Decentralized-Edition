// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineOilWell;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.BlockEntityOilDrillBase;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import com.hbm.backport.client.rendertype.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

public class ScreenMachineOilWell extends ScreenInfoContainer<MenuMachineOilWell> {

    private static final ResourceLocation TEXTURE = Library.id("textures/gui/machine/gui_well.png");

    public ScreenMachineOilWell(MenuMachineOilWell menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 184, 190);
        this.titleLabelY = 10;
        this.inventoryLabelX = 12;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 126;
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

        BlockEntityOilDrillBase be = drill();
        var extra = be.extraTank();

        int i = (int) (be.power * 34L / be.getMaxPower());
        if (i > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 8, 56 - i, 184, 34 - i, 16, i, 256, 256);

        // backport: on a contraption the drill shows the error lamp plus a warning panel (BF-034)
        boolean subLevel = be.indicator == BlockEntityOilDrillBase.INDICATOR_SUB_LEVEL;
        int lamp = subLevel ? 2 : be.indicator;
        if (lamp != 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    50,
                    19,
                    184 + (lamp - 1) * 14,
                    34,
                    14,
                    14,
                    256,
                    256);
        }

        if (extra == null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 48, 44, 200, 0, 18, 34, 256, 256);
        } else {
            drawFluidBar(graphics, 54, 45, 6, 32, extra);
        }

        drawFluidBar(graphics, 76, 22, 16, 52, be.tanks[0]);
        drawFluidBar(graphics, 112, 22, 16, 52, be.tanks[1]);
        drawInfoPanel(graphics, 160, 21, 8, 8, 8);

        drawElectricityInfo(graphics, mouseX, mouseY, 8, 22, 16, 34, be.power, be.getMaxPower());
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 76, 22, 16, 52, be.tanks[0]);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, 112, 22, 16, 52, be.tanks[1]);
        if (extra != null) drawFluidGaugeInfo(graphics, mouseX, mouseY, 54, 45, 6, 32, extra);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                160,
                21,
                8,
                8,
                resolveLines(
                        "desc.gui.upgrade",
                        "desc.gui.upgrade.speed",
                        "desc.gui.upgrade.power",
                        "desc.gui.upgrade.afterburner",
                        "desc.gui.upgrade.overdrive"));

        if (subLevel) {
            drawInfoPanel(graphics, -16, 36, 16, 16, 6);
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    -16,
                    36,
                    16,
                    16,
                    -8,
                    52,
                    List.of(Component.translatable("desc.gui.drill.subLevel")));
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityOilDrillBase drill() {
        return menu.blockEntity();
    }
}
