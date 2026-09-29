// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.armor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Backport of the 26.x {@code net.minecraft.world.item.equipment.Equippable} component, stored as the
 * backport data component {@link #TYPE} ({@code hbm:backport_equippable}; the tree's
 * {@code DataComponents.EQUIPPABLE} is rewritten to it).
 *
 * <p>Where 1.21.1 acts on it:
 * <ul>
 *   <li>{@link ArmorItem26} (all mod armor) reads slot, equip sound, asset and damageOnHurt from it;</li>
 *   <li>{@code MixinBackportEquippableLiving}: {@code LivingEntity.getEquipmentSlotForItem} answers
 *       {@link #slot()} (armor slots, shift-click, dispensers onto mobs, mob pickup);</li>
 *   <li>{@code MixinBackportEquippableItem}: {@code Item.use} equips a swappable non-armor item (jetpacks,
 *       wings) on right click, like 26.x;</li>
 *   <li>{@link ArmorRegistry}: damageOnHurt == false suppresses the armor durability loss.</li>
 * </ul>
 * cameraOverlay, equipOnInteract, canBeSheared and shearingSound are kept as data only.
 */
public record Equippable(
        EquipmentSlot slot,
        Holder<SoundEvent> equipSound,
        Optional<ResourceKey<EquipmentAsset>> assetId,
        Optional<ResourceLocation> cameraOverlay,
        Optional<HolderSet<EntityType<?>>> allowedEntities,
        boolean dispensable,
        boolean swappable,
        boolean damageOnHurt,
        boolean equipOnInteract,
        boolean canBeSheared,
        Holder<SoundEvent> shearingSound) {

    private static final Holder<SoundEvent> SHEARS_SNIP =
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.SHEEP_SHEAR); // backport: 1.21.1 has no item.shears.snip

    public static final Codec<Equippable> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            EquipmentSlot.CODEC.fieldOf("slot").forGetter(Equippable::slot),
                                            SoundEvent.CODEC
                                                    .optionalFieldOf("equip_sound", SoundEvents.ARMOR_EQUIP_GENERIC)
                                                    .forGetter(Equippable::equipSound),
                                            ResourceKey.codec(EquipmentAssets.ROOT_ID)
                                                    .optionalFieldOf("asset_id")
                                                    .forGetter(Equippable::assetId),
                                            ResourceLocation.CODEC
                                                    .optionalFieldOf("camera_overlay")
                                                    .forGetter(Equippable::cameraOverlay),
                                            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE)
                                                    .optionalFieldOf("allowed_entities")
                                                    .forGetter(Equippable::allowedEntities),
                                            Codec.BOOL.optionalFieldOf("dispensable", true)
                                                    .forGetter(Equippable::dispensable),
                                            Codec.BOOL.optionalFieldOf("swappable", true)
                                                    .forGetter(Equippable::swappable),
                                            Codec.BOOL.optionalFieldOf("damage_on_hurt", true)
                                                    .forGetter(Equippable::damageOnHurt),
                                            Codec.BOOL.optionalFieldOf("equip_on_interact", false)
                                                    .forGetter(Equippable::equipOnInteract),
                                            Codec.BOOL.optionalFieldOf("can_be_sheared", false)
                                                    .forGetter(Equippable::canBeSheared),
                                            SoundEvent.CODEC
                                                    .optionalFieldOf("shearing_sound", SHEARS_SNIP)
                                                    .forGetter(Equippable::shearingSound))
                                    .apply(i, Equippable::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Equippable> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /**
     * The backport component type. A plain instance (registered by {@link ArmorRegistry}) so it can be
     * used in Item.Properties at any time.
     */
    public static final DataComponentType<Equippable> TYPE =
            DataComponentType.<Equippable>builder()
                    .persistent(CODEC)
                    .networkSynchronized(STREAM_CODEC)
                    .cacheEncoding()
                    .build();

    public static Builder builder(EquipmentSlot slot) {
        return new Builder(slot);
    }

    /**
     * The stack's Equippable: the backport component, or -- for 1.21.1 vanilla equipables (armor, elytra,
     * pumpkins, skulls), which carry it natively in 26.x -- one derived from {@link Equipable}.
     * Replaces the tree's {@code stack.get(DataComponents.EQUIPPABLE)}.
     */
    public static @Nullable Equippable get(ItemStack stack) {
        Equippable equippable = stack.get(TYPE);
        if (equippable != null) return equippable;
        Equipable vanilla = Equipable.get(stack);
        if (vanilla == null) return null;
        EquipmentSlot slot = vanilla.getEquipmentSlot();
        // 1.21.1 shields are Equipable (off hand); 26.x shields have no Equippable
        if (slot.getType() == EquipmentSlot.Type.HAND) return null;
        return builder(slot).setEquipSound(vanilla.getEquipSound()).build();
    }

    public boolean canBeEquippedBy(EntityType<?> type) {
        return allowedEntities.isEmpty() || allowedEntities.get().contains(type.builtInRegistryHolder());
    }

    /** 26.x Equippable.swapWithEquipmentSlot, in 1.21.1 result form (as Equipable's). */
    public InteractionResultHolder<ItemStack> swapWithEquipmentSlot(
            ItemStack inHand, Level level, Player player, InteractionHand hand) {
        if (!player.canUseSlot(slot) || !canBeEquippedBy(player.getType()))
            return InteractionResultHolder.pass(inHand);
        ItemStack inSlot = player.getItemBySlot(slot);
        if ((EnchantmentHelper.has(inSlot, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
                        && !player.isCreative())
                || ItemStack.isSameItemSameComponents(inHand, inSlot)) {
            return InteractionResultHolder.fail(inHand);
        }
        if (!level.isClientSide()) player.awardStat(Stats.ITEM_USED.get(inHand.getItem()));
        if (inHand.getCount() <= 1) {
            ItemStack swapped = inSlot.isEmpty() ? inHand : inSlot.copyAndClear();
            ItemStack equipped = player.isCreative() ? inHand.copy() : inHand.copyAndClear();
            player.setItemSlot(slot, equipped);
            return InteractionResultHolder.sidedSuccess(swapped, level.isClientSide());
        }
        ItemStack swapped = inSlot.copyAndClear();
        ItemStack equipped = inHand.consumeAndReturn(1, player);
        player.setItemSlot(slot, equipped);
        if (!player.getInventory().add(swapped)) player.drop(swapped, false);
        return InteractionResultHolder.sidedSuccess(inHand, level.isClientSide());
    }

    public static final class Builder {
        private final EquipmentSlot slot;
        private Holder<SoundEvent> equipSound = SoundEvents.ARMOR_EQUIP_GENERIC;
        private Optional<ResourceKey<EquipmentAsset>> assetId = Optional.empty();
        private Optional<ResourceLocation> cameraOverlay = Optional.empty();
        private Optional<HolderSet<EntityType<?>>> allowedEntities = Optional.empty();
        private boolean dispensable = true;
        private boolean swappable = true;
        private boolean damageOnHurt = true;
        private boolean equipOnInteract;
        private boolean canBeSheared;
        private Holder<SoundEvent> shearingSound = SHEARS_SNIP;

        Builder(EquipmentSlot slot) {
            this.slot = slot;
        }

        public Builder setEquipSound(Holder<SoundEvent> equipSound) {
            this.equipSound = equipSound;
            return this;
        }

        public Builder setAsset(ResourceKey<EquipmentAsset> assetId) {
            this.assetId = Optional.of(assetId);
            return this;
        }

        public Builder setCameraOverlay(ResourceLocation cameraOverlay) {
            this.cameraOverlay = Optional.of(cameraOverlay);
            return this;
        }

        public Builder setAllowedEntities(EntityType<?>... types) {
            return setAllowedEntities(
                    HolderSet.direct(List.of(types).stream().map(EntityType::builtInRegistryHolder).toList()));
        }

        public Builder setAllowedEntities(HolderSet<EntityType<?>> types) {
            this.allowedEntities = Optional.of(types);
            return this;
        }

        public Builder setDispensable(boolean dispensable) {
            this.dispensable = dispensable;
            return this;
        }

        public Builder setSwappable(boolean swappable) {
            this.swappable = swappable;
            return this;
        }

        public Builder setDamageOnHurt(boolean damageOnHurt) {
            this.damageOnHurt = damageOnHurt;
            return this;
        }

        public Builder setEquipOnInteract(boolean equipOnInteract) {
            this.equipOnInteract = equipOnInteract;
            return this;
        }

        public Builder setCanBeSheared(boolean canBeSheared) {
            this.canBeSheared = canBeSheared;
            return this;
        }

        public Builder setShearingSound(Holder<SoundEvent> shearingSound) {
            this.shearingSound = shearingSound;
            return this;
        }

        public Equippable build() {
            return new Equippable(
                    slot,
                    equipSound,
                    assetId,
                    cameraOverlay,
                    allowedEntities,
                    dispensable,
                    swappable,
                    damageOnHurt,
                    equipOnInteract,
                    canBeSheared,
                    shearingSound);
        }
    }
}
