// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.jspecify.annotations.Nullable;

public record FluidTooltipFrame(int top, int bottom) implements TooltipComponent {

    private static final int NONE_COLOR = 0x888888;

    public static FluidTooltipFrame of(@Nullable Fluid fluid) {
        NTMFluidProperty prop = NTMFluidProperties.get(fluid);
        int color = prop != null ? prop.color() : NONE_COLOR;
        int r = color >> 16 & 0xFF, g = color >> 8 & 0xFF, b = color & 0xFF;
        int add = (r + g + b) / 3 > 0x80 ? -0x40 : 0x40;
        int shade =
                Mth.clamp(r + add, 0, 255) << 16
                        | Mth.clamp(g + add, 0, 255) << 8
                        | Mth.clamp(b + add, 0, 255);
        return new FluidTooltipFrame(0xFF000000 | color, 0xFF000000 | shade);
    }

    /**
     * backport: 26.x draws the frame from the component's extractImage, which gets the tooltip's
     * width and height; 1.21.1's renderImage does not. The 26.x frame lies exactly on 1.21.1's
     * tooltip border (1px left/right gradient top->bottom, top line, bottom line), so the colours
     * are applied through NeoForge's RenderTooltipEvent.Color border instead (registered by
     * com.hbm.backport.client.gui.GuiBackport).
     */
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        for (ClientTooltipComponent c : event.getComponents()) {
            if (c instanceof Renderer r) {
                event.setBorderStart(r.frame.top);
                event.setBorderEnd(r.frame.bottom);
                return;
            }
        }
    }

    public static final class Renderer implements ClientTooltipComponent {

        private static final int GAP = 4;
        private final FluidTooltipFrame frame;

        public Renderer(FluidTooltipFrame frame) {
            this.frame = frame;
        }

        @Override
        public int getHeight() {
            return GAP;
        }

        @Override
        public int getWidth(Font font) {
            return 0;
        }
    }
}
