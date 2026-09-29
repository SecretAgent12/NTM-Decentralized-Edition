// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.GunTickState;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.items.weapon.sedna.GunTimers;
import com.hbm.packet.SyncSource;
import com.hbm.util.TooltipStyle;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.hbm.packet.SyncBindings;

@Mixin(ItemStack.class)
public abstract class MixinItemStack implements com.hbm.backport.ItemInstance, GunTickState, SyncSource {

    @Shadow private int count;

    @WrapOperation(
            method =
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/component/ItemAttributeModifiers;forEach(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"))
    private void hbm$crucibleEquipmentAttributes(
            ItemAttributeModifiers modifiers,
            EquipmentSlot slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            Operation<Void> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!(stack.getItem() instanceof ItemCrucible sword) || sword.canOperate(stack)) {
            original.call(modifiers, slot, consumer);
        }
    }

    // backport: 1.21.1 forEachModifier(EquipmentSlotGroup, BiConsumer) (no TriConsumer/Display)
    @WrapOperation(
            method =
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/component/ItemAttributeModifiers;forEach(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"))
    private void hbm$crucibleTooltipAttributes(
            ItemAttributeModifiers modifiers,
            EquipmentSlotGroup slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            Operation<Void> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!(stack.getItem() instanceof ItemCrucible sword) || sword.canOperate(stack)) {
            original.call(modifiers, slot, consumer);
        }
    }

    @Inject(method = "setCount", at = @At("HEAD"))
    private void hbm$syncCount(int next, CallbackInfo ci) {
        if (count != next) syncChanged(3);
    }

    @Unique private @Nullable GunTimers hbm$gunTimers;

    // backport: 26.x wraps MAP_CODEC (MapCodec.recursive), which backs CODEC. 1.21.1 has no
    // MAP_CODEC; CODEC is the first Codec.lazyInitialized in <clinit> (SINGLE_ITEM_CODEC, the
    // second, is left alone as in 26.x).
    @ModifyExpressionValue(
            method = "<clinit>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;",
                            ordinal = 0))
    private static Codec<ItemStack> hbm$saveGunCounters(Codec<ItemStack> codec) {
        return codec.xmap(stack -> stack, GunTimers::forSave);
    }

    // backport: 26.x grays the Consumer handed to appendHoverText in addDetailsToTooltip. In 1.21.1
    // getTooltipLines passes a List; the lines the item appended are restyled after the call.
    @WrapOperation(
            method = "getTooltipLines",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void hbm$grayHoverText(
            Item item,
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> lines,
            TooltipFlag flag,
            Operation<Void> original) {
        int from = lines.size();
        original.call(item, stack, context, lines, flag);
        if (!TooltipStyle.isNtm(item)) return;
        for (int i = from; i < lines.size(); i++)
            lines.set(i, TooltipStyle.defaultColor(lines.get(i), ChatFormatting.GRAY));
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void hbm$copyGunTimers(CallbackInfoReturnable<ItemStack> cir) {
        if (hbm$gunTimers != null && !cir.getReturnValue().isEmpty()) {
            ((GunTickState) (Object) cir.getReturnValue()).hbm$setGunTimers(hbm$gunTimers.copy());
        }
    }

    @Override
    public @Nullable GunTimers hbm$gunTimers() {
        return hbm$gunTimers;
    }

    @Override
    public void hbm$setGunTimers(@Nullable GunTimers timers) {
        hbm$gunTimers = timers;
    }


    // backport: woven trait SyncSource
    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
