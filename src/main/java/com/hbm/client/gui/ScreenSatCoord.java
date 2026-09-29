// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemSatelliteInterface;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.ItemControlPayload;
import com.hbm.platform.Services;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import com.hbm.backport.client.gui.input.KeyEvent;
import com.hbm.backport.client.gui.input.MouseButtonEvent;
import com.hbm.backport.client.rendertype.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.apache.commons.lang3.math.NumberUtils;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.client.gui.ScreenCompat;
import com.hbm.backport.Nbt;
import com.hbm.backport.client.gui.WidgetBridge;

public class ScreenSatCoord extends ScreenCompat {

    private static final ResourceLocation TEXTURE =
            Library.id("textures/gui/satellites/gui_sat_coord.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 126;
    private static final int CONFIRM_X = 133;
    private static final int CONFIRM_Y = 52;
    private static final int CONFIRM_SIZE = 18;

    private final ItemStack device;
    private int left;
    private int top;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    public ScreenSatCoord(Player player) {
        super(Component.translatable("item.hbm.sat_coord"));
        this.device = player.getMainHandItem();
    }

    private static @Nullable Integer parse(EditBox field) {
        String text = field.getValue();
        if (!NumberUtils.isCreatable(text)) return null;
        try {
            return (int) Double.parseDouble(text);
        } catch (NumberFormatException e) {

            return null;
        }
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;

        xField = field(left + 66, top + 21, "x");
        yField = field(left + 66, top + 56, "y");
        zField = field(left + 66, top + 92, "z");
    }

    private EditBox field(int x, int y, String axis) {
        EditBox box = new EditBox(font, x, y, 48, 12, Component.literal(axis));
        box.setMaxLength(7);
        box.setBordered(false);
        box.setTextColor(CommonColors.WHITE);
        addRenderableWidget(box);
        return box;
    }

    private boolean connected() {
        CustomData data = device.get(ModDataComponents.PERSISTENT_DATA.get());
        return data != null
                && Nbt.getBooleanOr(data.copyTag(), ItemSatelliteInterface.KEY_CONNECTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!connected()) return false;

        setFocused(null);
        if (super.mouseClicked(event, doubleClick)) return true;
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (mouseX < left + CONFIRM_X || mouseX >= left + CONFIRM_X + CONFIRM_SIZE) return false;
        if (mouseY < top + CONFIRM_Y || mouseY >= top + CONFIRM_Y + CONFIRM_SIZE) return false;

        Integer x = parse(xField);
        Integer z = parse(zField);
        Integer y = parse(yField);
        if (x == null || z == null) return false;

        if (minecraft != null) {
            minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        CompoundTag data = new CompoundTag();
        data.putInt("x", x);
        data.putInt("z", z);
        if (y != null) data.putInt("y", y);
        Services.NETWORK.sendToServer(new ItemControlPayload(data));
        onClose();
        return true;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left,
                top,
                0.0F,
                0.0F,
                WIDTH,
                HEIGHT,
                256,
                256);

        if (xField.isFocused()) focus(graphics, top + 16);
        if (yField.isFocused()) focus(graphics, top + 52);
        if (zField.isFocused()) focus(graphics, top + 88);

        if (connected()) {
            pip(graphics, top + 17);
            pip(graphics, top + 25);
        }
    }

    private void focus(GuiGraphicsExtractor graphics, int y) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left + 61,
                y,
                0.0F,
                126.0F,
                54,
                18,
                256,
                256);
    }

    private void pip(GuiGraphicsExtractor graphics, int y) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, left + 120, y, 194.0F, 0.0F, 7, 7, 256, 256);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft != null && WidgetBridge.matches(minecraft.options.keyInventory, event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
