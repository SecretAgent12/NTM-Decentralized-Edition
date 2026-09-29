// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.item.tool;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

/**
 * 26.x {@code net.minecraft.world.item.ToolMaterial}, implementing 1.21.1's {@link Tier} so it
 * can be handed straight to the native TieredItem/DiggerItem/HoeItem/SwordItem constructors
 * (see {@link HoeItem26}). Those classes then supply durability, enchantment value and repair
 * items from this record exactly as ToolMaterial.applyCommonProperties does in 26.x.
 */
public record ToolMaterial(
        TagKey<Block> incorrectBlocksForDrops,
        int durability,
        float speed,
        float attackDamageBonus,
        int enchantmentValue,
        TagKey<Item> repairItems)
        implements Tier {

    // 26.x vanilla materials; repair tags are the 1.21.1 equivalents of the 26.x *_tool_materials
    // item tags (1.21.1 only has minecraft:stone_tool_materials).
    // backport: unverified: 26.x numeric values taken from 1.21.2+ vanilla
    public static final ToolMaterial WOOD =
            new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 2.0F, 0.0F, 15, ItemTags.PLANKS);
    public static final ToolMaterial STONE =
            new ToolMaterial(
                    BlockTags.INCORRECT_FOR_STONE_TOOL, 131, 4.0F, 1.0F, 5, ItemTags.STONE_TOOL_MATERIALS);
    public static final ToolMaterial IRON =
            new ToolMaterial(
                    BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0F, 2.0F, 14, Tags.Items.INGOTS_IRON);
    public static final ToolMaterial DIAMOND =
            new ToolMaterial(
                    BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10, Tags.Items.GEMS_DIAMOND);
    public static final ToolMaterial GOLD =
            new ToolMaterial(
                    BlockTags.INCORRECT_FOR_GOLD_TOOL, 32, 12.0F, 0.0F, 22, Tags.Items.INGOTS_GOLD);
    public static final ToolMaterial NETHERITE =
            new ToolMaterial(
                    BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
                    2031,
                    9.0F,
                    4.0F,
                    15,
                    Tags.Items.INGOTS_NETHERITE);

    // ---- 1.21.1 Tier ----

    @Override
    public int getUses() {
        return durability;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonus;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectBlocksForDrops;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(repairItems);
    }

    // ---- 26.x ToolMaterial helpers, for code that builds a plain Item from a material ----

    private Item.Properties applyCommonProperties(Item.Properties properties) {
        properties.durability(durability);
        ItemProps26.repairable(properties, repairItems);
        if (enchantmentValue > 0) ItemProps26.enchantable(properties, enchantmentValue);
        return properties;
    }

    public Item.Properties applyToolProperties(
            Item.Properties properties,
            TagKey<Block> minesEfficiently,
            float attackDamage,
            float attackSpeed,
            float disableBlockingForSeconds) {
        return applyCommonProperties(properties)
                .component(
                        DataComponents.TOOL,
                        // backport: 1.21.1 Tool has no canDestroyBlocksInCreative (26.x: true here)
                        new Tool(
                                List.of(
                                        Tool.Rule.deniesDrops(incorrectBlocksForDrops),
                                        Tool.Rule.minesAndDrops(minesEfficiently, speed)),
                                1.0F,
                                1))
                .attributes(createToolAttributes(attackDamage, attackSpeed))
                .component(ToolRegistry.WEAPON.get(), new Weapon(2, disableBlockingForSeconds));
    }

    public Item.Properties applySwordProperties(
            Item.Properties properties, float attackDamage, float attackSpeed) {
        return applyCommonProperties(properties)
                .component(
                        DataComponents.TOOL,
                        new Tool(
                                List.of(
                                        Tool.Rule.minesAndDrops(
                                                List.of(net.minecraft.world.level.block.Blocks.COBWEB),
                                                15.0F),
                                        Tool.Rule.overrideSpeed(ItemProps26.SWORD_INSTANTLY_MINES, Float.MAX_VALUE),
                                        Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F)),
                                1.0F,
                                2))
                .component(ToolRegistry.NO_CREATIVE_DESTROY.get(), net.minecraft.util.Unit.INSTANCE)
                .attributes(createSwordAttributes(attackDamage, attackSpeed))
                .component(ToolRegistry.WEAPON.get(), new Weapon(1));
    }

    private ItemAttributeModifiers createToolAttributes(float attackDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_ID,
                                attackDamage + attackDamageBonus,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Item.BASE_ATTACK_SPEED_ID,
                                attackSpeed,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private ItemAttributeModifiers createSwordAttributes(float attackDamage, float attackSpeed) {
        return createToolAttributes(attackDamage, attackSpeed);
    }
}
