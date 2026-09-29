// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.BaseAbility;
import com.hbm.handler.ability.WeaponAbility;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import com.hbm.backport.TooltipDisplay;
import com.hbm.backport.item.tool.ItemProps26;
import com.hbm.backport.item.tool.ToolRegistry;
import com.hbm.backport.item.tool.Weapon;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.ItemCompat;

public class ItemSwordAbility extends ItemCompat {

    private final AvailableAbilities abilities;

    public ItemSwordAbility(Properties properties, AvailableAbilities abilities) {
        super(properties);
        this.abilities = abilities;
    }

    public static Properties swordProperties(
            ToolTier tier,
            float attackDamage,
            double movement,
            @Nullable TagKey<Item> repairItems) {
        Properties properties = new Properties().stacksTo(1);
        // backport: Item.Properties.enchantable/repairable are 26.x-only -> shim components
        if (tier.enchantmentValue() > 0)
            ItemProps26.enchantable(properties, tier.enchantmentValue());
        if (tier.durability() > 0) properties.durability(tier.durability());
        if (repairItems != null) ItemProps26.repairable(properties, repairItems);

        // backport: 1.21.1 Tool.Rule takes blocks/tags directly (26.x: HolderSets from
        // acquireBootstrapRegistrationLookup). #sword_instantly_mines does not exist in 1.21.1:
        // its contents (bamboo, bamboo sapling) are listed instead. 1.21.1 Tool has no
        // canDestroyBlocksInCreative; the 26.x `false` is carried by hbm:backport_no_creative_destroy.
        return properties
                .component(
                        DataComponents.TOOL,
                        new Tool(
                                List.of(
                                        Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F),
                                        Tool.Rule.overrideSpeed(
                                                ItemProps26.SWORD_INSTANTLY_MINES, Float.MAX_VALUE),
                                        Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F)),
                                1.0F,
                                2))
                .component(ToolRegistry.NO_CREATIVE_DESTROY.get(), net.minecraft.util.Unit.INSTANCE)
                .attributes(ItemToolAbility.attributes(attackDamage, -2.4F, movement))
                .component(ToolRegistry.WEAPON.get(), new Weapon(1));
    }

    public AvailableAbilities abilities() {
        return abilities;
    }

    public boolean canOperate(ItemStack stack) {
        return true;
    }

    @Override
    public void backport$hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!(victim.level() instanceof ServerLevel level) || !(attacker instanceof Player player))
            return;
        if (!canOperate(stack)) return;

        for (Map.Entry<BaseAbility, Integer> entry : abilities.weapon()) {
            ((WeaponAbility) entry.getKey()).onHit(level, entry.getValue(), player, victim);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        abilities.appendTooltip(adder);
    }
}
